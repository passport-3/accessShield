package com.module.server.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

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
}