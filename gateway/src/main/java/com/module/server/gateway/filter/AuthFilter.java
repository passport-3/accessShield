package com.module.server.gateway.filter;

import com.module.server.gateway.dto.RoleResponseDto;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final WebClient.Builder webClientBuilder;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public AuthFilter(WebClient.Builder webClientBuilder, ReactiveRedisTemplate<String, String> redisTemplate) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            log.info("--------------> Start Auth Filter");

            String path = exchange.getRequest().getPath().value();
            log.info("Current request path: {}", path);

            exchange.getRequest().getHeaders().forEach((key, values) -> {
                log.info("Header {}: {}", key, values);
            });

            // 제외 경로 확인
            if (config.isExcludedPath(path)) {
                log.info("Path {} is excluded from authentication", path);
                return chain.filter(exchange);
            }
            log.info("Next step token verify");

            String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            log.info("--------------> Access Token {}", token);

            if (token == null || token.isEmpty()) {
                return Mono.fromRunnable(() -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    exchange.getResponse().getHeaders().add("Content-Type", "text/plain;charset=UTF-8");
                }).then(exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                        .bufferFactory().wrap("로그인이 필요합니다.".getBytes()))));
            }

            // TODO redis check

            // TODO token generate

            // TODO ROLE check

            // TODO 요청속도 제한

            // TODO logging

            // TODO ip white list check

            // TODO 오류 메시지 형식

            // 토큰 검증
            return webClientBuilder.build()
                    .get()  // GET 메서드 사용
                    .uri("http://localhost:19092/api/auth/verify?accessToken={token}", token)  // accessToken을 쿼리 파라미터로 추가
                    .retrieve()
                    .toBodilessEntity()
                    .flatMap(response -> {
                        if (response.getStatusCode().is2xxSuccessful()) {
                            return chain.filter(exchange);
                        } else {
                            return unauthorizedResponse(exchange, "유효하지 않은 토큰입니다.");
                        }
                    })
                    .onErrorResume(error -> {
                        return internalServerErrorResponse(exchange, "인증 서비스 오류가 발생했습니다.");
                    });
        };
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

    // 오류 메시지
    private Mono<Void> sendErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "text/plain;charset=UTF-8");
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory().wrap(message.getBytes())));
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "text/plain;charset=UTF-8");
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory().wrap(message.getBytes())));
    }

    private Mono<Void> internalServerErrorResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        exchange.getResponse().getHeaders().add("Content-Type", "text/plain;charset=UTF-8");
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory().wrap(message.getBytes())));
    }



    // API 접근 권한을 Redis에서 확인하는 메서드
    private Mono<Void> checkApiAccess(String role, String path, ServerWebExchange exchange, GatewayFilterChain chain) {
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

    // JWT에서 role을 추출하는 메서드 (JWT의 Payload만 사용)
    private String getRoleFromToken(String token) {
        String[] tokenParts = token.split("\\.");
        String payload = new String(Base64.getDecoder().decode(tokenParts[1]));
        JSONObject jsonObject = new JSONObject(Integer.parseInt(payload));
        return jsonObject.getAsString("role");
    }









    /**
     * Redis와 연동하여 요청 속도를 제한
     */
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private Mono<Void> rateLimitCheck(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientId = getClientId(exchange);  // 클라이언트 ID를 추출 (IP 주소 또는 사용자 토큰 기반)

        String redisKey = "rate_limit:" + clientId;  // Redis에서 사용할 키 (클라이언트별)
        ReactiveValueOperations<String, String> valueOps = redisTemplate.opsForValue();


        // Redis에서 현재 요청 수를 가져오기
        return valueOps.get(redisKey)
                .defaultIfEmpty("0")  // Redis에 값이 없으면 기본값을 0으로 설정
                .flatMap(currentRequestCount -> {
                    int requestCount = Integer.parseInt(currentRequestCount);

                    // 요청 수가 제한을 초과한 경우
                    if (requestCount >= MAX_REQUESTS_PER_MINUTE) {
                        return sendErrorResponse(exchange, HttpStatus.TOO_MANY_REQUESTS, "요청 수가 너무 많습니다. 잠시 후 다시 시도하세요.");
                    }

                    // 요청 수 증가 및 TTL 설정 (1분)
                    return valueOps.increment(redisKey)  // 현재 요청 수 증가
                            .then(redisTemplate.expire(redisKey, Duration.ofMinutes(1)))  // 1분 후 키 만료 설정
                            .then(chain.filter(exchange));  // 필터 체인 계속 진행
                });
    }

    // 클라이언트 ID를 가져오는 메서드 (IP 주소 또는 사용자 토큰 기반으로 클라이언트 구분)
    private String getClientId(ServerWebExchange exchange) {
        return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }

}