package com.module.server.gateway.filter;

import com.module.server.gateway.dto.RoleResponseDto;
import lombok.extern.slf4j.Slf4j;
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


    /**
     * 역할 검증 로직 (Auth 서버에 역할 검증 요청)
     * @param token 인증 토큰
     * @param exchange WebExchange 객체
     * @param chain 필터 체인
     * @return Mono<Void>
     */
    private Mono<Void> checkTokenRole(String token, ServerWebExchange exchange, GatewayFilterChain chain) {
        // Auth 서버로 JWT 토큰 검증 및 역할 요청 (예전 방식은 삭제)
        return webClientBuilder.build()
                .get()
                .uri("http://localhost:19092/api/auth/role?accessToken={token}", token)  // Auth 서버에 역할 확인 요청
                .retrieve()
                .bodyToMono(RoleResponseDto.class)
                .flatMap(roleResponseDto -> {
                    // Auth 서버로부터 받은 사용자 역할 정보
                    String role = roleResponseDto.getRole();
                    log.info("Extracted Role from Auth server: {}", role);

                    String apiEndpoint = exchange.getRequest().getPath().value();  // 현재 요청한 API 경로 가져오기

                    // Auth 서버로 API별 허용된 역할 정보 요청
                    return getRolesFromAuth(apiEndpoint)
                            .flatMap(allowedRoles -> {
                                if (allowedRoles == null || allowedRoles.isEmpty()) {
                                    return sendErrorResponse(exchange, HttpStatus.NOT_FOUND, "API 경로가 존재하지 않습니다.");
                                }

                                // 허용된 역할에 사용자의 역할이 포함되지 않으면 403 Forbidden 반환
                                if (!allowedRoles.contains(role)) {
                                    log.info("Role {} is not allowed to access {}", role, apiEndpoint);
                                    return sendErrorResponse(exchange, HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
                                }

                                // 역할 검증 통과 시 다음 필터 체인 진행
                                return chain.filter(exchange);
                            });
                })
                .onErrorResume(e -> {
                    log.error("Error during role verification: {}", e.getMessage());
                    return sendErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "인증 서비스 오류가 발생했습니다.");
                });
    }

    /**
     * Auth 서버로부터 API별 허용된 역할 정보를 받아오는 메서드
     * @param apiEndpoint API 경로
     * @return 허용된 역할 리스트
     */
    private Mono<List<String>> getRolesFromAuth(String apiEndpoint) {
        return webClientBuilder.build()
                .get()
                .uri("http://localhost:19092/api/auth/role?apiPath={apiEndpoint}", apiEndpoint)  // Auth 서버에서 역할 정보를 가져옴
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {});  // 허용된 역할 리스트 반환
    }

    // 1. JWT토큰 검증 -> 먼저 Auth  서버에 JWT토큰을 보내 사용자의 역할 정보를 받는다.
    // 2. API별 허용된 역할 정보 요청 -> Auth  서버에 API 경로를 보내어 해당 API에 허용된 역할 리스트를 요청한다.
    // 3. 역할비교 : 사용자 역할이 해당 API에 허용된 역할 리스트에 포함되어 있는지 확인한다.
    // 허용되지 않는 경우 -> 403, 허용된경우 -> 필터체인을 진행하여 요청을 허용한다.


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