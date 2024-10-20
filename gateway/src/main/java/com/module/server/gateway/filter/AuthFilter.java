package com.module.server.gateway.filter;

import com.module.server.gateway.constants.ErrorMessage;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final WebClient.Builder webClientBuilder;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    public AuthFilter(WebClient.Builder webClientBuilder, ReactiveRedisTemplate<String, String> redisTemplate) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            log.info("--------------> Start Auth Filter");

            // 1. IP 화이트리스트 체크
            return checkIpWhitelist(exchange, chain)
                    .flatMap(v -> {
                        // 2. 요청 속도 제한 체크
                        return rateLimitCheck(exchange, chain)
                                .flatMap(vv -> {
                                    // 3. 현재 요청 경로 로그
                                    String path = exchange.getRequest().getPath().value();
                                    log.info("Current request path: {}", path);

                                    // 요청 헤더 로그
                                    exchange.getRequest().getHeaders().forEach((key, values) -> {
                                        log.info("Header {}: {}", key, values);
                                    });

                                    // 제외 경로 확인
                                    if (config.isExcludedPath(path)) {
                                        log.info("Path {} is excluded from authentication", path);
                                        return chain.filter(exchange);
                                    }

                                    log.info("-----------> Next step token verify");

                                    // 4. 토큰 존재 여부 확인 및 검증
                                    String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                                    if (token == null || token.isEmpty()) {
                                        return unauthorizedResponse(exchange, ErrorMessage.LOGIN_REQUIRED);
                                    }

                                    return redisTokenCheck(token, exchange, chain);
                                });
                    })
                    .onErrorResume(e -> {
                        log.error("Error during processing: {}", e.getMessage());
                        return sendErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessage.AUTH_SERVER_ERROR);
                    });
        };
    }

    private Mono<Void> redisTokenCheck(String token, ServerWebExchange exchange, GatewayFilterChain chain) {
        String username = this.getJwtPayloadInfo(token, "username");
        String tokenCategory = this.getJwtPayloadInfo(token, "category");
        String redisKey = generateRedisKey(tokenCategory, username);

        ReactiveValueOperations<String, String> valueOps = redisTemplate.opsForValue();

        return valueOps.get(redisKey)
                .flatMap(storedToken -> {
                    if (storedToken == null) {
                        return handleRefreshToken(username, exchange, chain);
                    }

                    // 토큰 검증
                    return webClientBuilder.build()
                            .get()
                            .uri("http://localhost:19092/api/auth/verify?accessToken={token}", token)
                            .retrieve()
                            .toBodilessEntity()
                            .flatMap(response -> {
                                if (response.getStatusCode().is2xxSuccessful()) {
                                    // TODO 권한 체크
//                                    return authorizeAccess(exchange, getJwtPayloadInfo(token, "role"))
//                                            .then(chain.filter(exchange));
                                    return chain.filter(exchange);
                                } else {
                                    return unauthorizedResponse(exchange, "유효하지 않은 토큰입니다.");
                                }
                            })
                            .onErrorResume(error -> {
                                return sendErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, ErrorMessage.AUTH_SERVER_ERROR);
                            });
                });
    }

    // refresh token 이 redis에 저장되어 있는지 확인 후 새로운 access token 발급 요청
    private Mono<Void> handleRefreshToken(String username, ServerWebExchange exchange, GatewayFilterChain chain) {
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

    private Mono<Void> requestNewAccessToken(String refreshToken, ServerWebExchange exchange, GatewayFilterChain chain) {
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

    private Mono<Void> rateLimitCheck(ServerWebExchange exchange, GatewayFilterChain chain) {
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

    private Mono<Void> checkIpWhitelist(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        List<String> allowedIps = List.of("127.0.0.1", "192.168.0.1", "192.168.0.2");

        if (!allowedIps.contains(clientIp)) {
            return sendErrorResponse(exchange, HttpStatus.FORBIDDEN, ErrorMessage.ACCESS_DENIED_IP);
        }
        return Mono.empty(); // 화이트리스트에 있으면 다음 필터로 진행
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
        }

        String payload = parts[1];
        String decodedPayload = new String(Base64.getUrlDecoder().decode(payload));
        return new JSONObject(decodedPayload).getString(type);
    }

    private String generateRedisKey(String category, String username) {
        return String.format("token:%s:%s", category.toLowerCase(), username.toLowerCase());
    }

    public static class Config {
        private List<String> excludePaths;

        public List<String> getExcludePaths() {
            return excludePaths;
        }

        public void setExcludePaths(List<String> excludePaths) {
            this.excludePaths = excludePaths;
        }

        public boolean isExcludedPath(String path) {
            return excludePaths.stream().anyMatch(excludePath -> path.startsWith(excludePath));
        }
    }
}
