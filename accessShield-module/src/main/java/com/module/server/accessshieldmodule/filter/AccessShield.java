package com.module.server.accessshieldmodule.filter;

import com.module.server.accessshieldmodule.constans.ErrorMessage;
import jakarta.servlet.*;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Slf4j
public class AccessShield implements WebFilter {
    private final WebClient.Builder webClientBuilder;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    private static List<String> excluePaths = null;



    public AccessShield(WebClient.Builder webClientBuilder, ReactiveRedisTemplate<String, String> redisTemplate) {

        this.webClientBuilder = webClientBuilder;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        log.info("--------------> Start Auth Filter");

        // 1. IP 화이트리스트 체크
        if (!checkIpWhitelist(exchange)) {
            return sendErrorResponse(exchange, HttpStatus.FORBIDDEN, ErrorMessage.ACCESS_DENIED_IP);
        }

        // 2. 현재 요청 경로 확인
        String path = exchange.getRequest().getPath().value();
        log.info("Current request path: {}", path);

        // 요청 헤더 로그
        exchange.getRequest().getHeaders().forEach((key, values) -> {
            log.info("Header {}: {}", key, values);
        });

        // TODO : 제외경로 확인

        log.info("-----------> Next step token verify");

        // 4. 토큰 존재 여부 확인 및 검증
        String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (token == null || token.isEmpty()) {
            return unauthorizedResponse(exchange, ErrorMessage.LOGIN_REQUIRED);
        }
        log.info("------------> user token {}", token);

        return getTokenFromRedis(token, exchange, chain)
                .flatMap(storedToken -> {
                    if (storedToken == null) {
                        String username = getUsernameFromToken(token);
                        if (username == null) {
                            return Mono.empty(); // null일 경우 후속 처리 중단
                        }
                        return handleRefreshToken(username, exchange, chain);
                    }
                    return validateToken(token, exchange, chain);
                })
                .onErrorResume(e -> {
                    log.error("Error during processing: {}", e.getMessage());
                    return sendErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessage.AUTH_SERVER_ERROR);
                });
    }



    private boolean checkIpWhitelist(ServerWebExchange exchange) {
        String clientIp = getClientId(exchange);
        log.info("Current request ip: {}", clientIp);
        List<String> allowedIps = List.of("0:0:0:0:0:0:0:1", "127.0.0.1", "192.168.0.1", "192.168.0.2");

        return allowedIps.contains(clientIp);
    }

    // redis에 저장된 access token이 있는지 확인
    private Mono<String> getTokenFromRedis(String token, ServerWebExchange exchange, WebFilterChain chain) {
        String username = getUsernameFromToken(token);
        String tokenCategory = "access_token"; //getTokenCategory(token);
        String redisKey = generateRedisKey(tokenCategory, username);

        ReactiveValueOperations<String, String> valueOps = redisTemplate.opsForValue();
        log.info("------------> redis access token {}", redisKey);

        return valueOps.get(redisKey);
    }

    private Mono<Void> validateToken(String token, ServerWebExchange exchange, WebFilterChain chain) {
        return callHttpRequest("http://localhost:19092/api/auth/verify?accessToken={token}", HttpMethod.GET, token)
                .flatMap(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        // TODO 권한 체크
                        return chain.filter(exchange);
                    } else {
                        return unauthorizedResponse(exchange, "유효하지 않은 토큰입니다.");
                    }
                })
                .onErrorResume(error -> {
                    return sendErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessage.AUTH_SERVER_ERROR);
                });
    }

    // refresh token 이 redis에 저장되어 있는지 확인 후 새로운 access token 발급 요청
    private Mono<Void> handleRefreshToken(String username, ServerWebExchange exchange, WebFilterChain chain) {
        String refreshTokenKey = generateRedisKey("refresh_token", username);
        ReactiveValueOperations<String, String> valueOps = redisTemplate.opsForValue();

        return valueOps.get(refreshTokenKey)
                .flatMap(refreshToken -> {
                    if (refreshToken == null) {
                        return unauthorizedResponse(exchange, ErrorMessage.INVALID_TOKEN);
                    }
                    return requestNewAccessToken(refreshToken, exchange, chain);
                })
                .switchIfEmpty(unauthorizedResponse(exchange, ErrorMessage.INVALID_TOKEN));
    }

    // 새로운 access token 발급
    private Mono<Void> requestNewAccessToken(String refreshToken, ServerWebExchange exchange, WebFilterChain chain) {
        return webClientBuilder.build()
                .post()
                .uri("http://localhost:19092/api/auth/reIssue?username={username}&role={role}",
                        getUsernameFromToken(refreshToken), getRoleFromToken(refreshToken))
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(newAccessToken -> {
                    exchange.getResponse().getHeaders().set(HttpHeaders.AUTHORIZATION, newAccessToken);
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> rateLimitCheck(ServerWebExchange exchange, WebFilterChain chain) {
        String clientId = getClientId(exchange);
        String redisKey = "rate_limit:" + clientId;

        return redisTemplate.opsForValue().increment(redisKey)
                .flatMap(currentRequestCount -> {
                    if (currentRequestCount > MAX_REQUESTS_PER_MINUTE) {
                        return sendErrorResponse(exchange, HttpStatus.TOO_MANY_REQUESTS, ErrorMessage.TOO_MANY_REQUESTS);
                    }
                    return redisTemplate.expire(redisKey, Duration.ofMinutes(1))
                            .then(chain.filter(exchange));
                });
    }

    // API 접근 권한을 Redis에서 확인하는 메서드
    private Mono<Void> checkApiAccess(String role, String path, ServerWebExchange exchange, WebFilterChain chain) {
        String redisKey = "role_api:" + role;

        // Redis에서 역할에 맞는 API 목록 가져오기
        return redisTemplate.opsForSet().members(redisKey)
                .flatMap(apiSet -> {
                    if (apiSet != null && apiSet.contains(path)) {
                        return chain.filter(exchange); // 권한 있을 경우 다음 필터 진행
                    } else {
                        return unauthorizedResponse(exchange, "접근 권한이 없습니다."); // 권한 없을 경우
                    }
                }).then();
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "text/plain;charset=UTF-8");
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(message.getBytes())));
    }

    private Mono<Void> sendErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String jsonResponse = String.format("{\"error\": \"%s\"}", message);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(jsonResponse.getBytes())));
    }

    private String getClientId(ServerWebExchange exchange) {
        return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }

    private String getUsernameFromToken(String token) {
        return getJwtPayloadInfo(token, "username");
    }

    private String getRoleFromToken(String token) {
        return getJwtPayloadInfo(token, "role");
    }

    private String getTokenCategory(String token) {
        return getJwtPayloadInfo(token, "category");
    }

    private String getJwtPayloadInfo(String jwt, String type) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT token");
            //return null;
        }
        // TODO : 토큰에 없는 subject를 찾는 경우

        String payload = parts[1];
        String decodedPayload = new String(Base64.getUrlDecoder().decode(payload));
        return new JSONObject(decodedPayload).getString(type);
    }

    private String generateRedisKey(String category, String username) {
        return String.format("token:%s:%s", category.toLowerCase(), username.toLowerCase());
    }

    // http 통신
    private Mono<ResponseEntity<Void>> callHttpRequest(String uri, HttpMethod method, String token) {
        return webClientBuilder.build()
                .method(method)
                .uri(uri, token)
                .retrieve()
                .toBodilessEntity();
    }




}
