package com.module.server.gateway.filter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.json.JSONObject;

@Slf4j
@Component
@EnableWebFluxSecurity
@Qualifier("customSecurityFilterChain")
public class SecurityAuthenticationFilter extends AuthenticationWebFilter {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final WebClient.Builder webClientBuilder;
    private final ReactiveAuthenticationManager authenticationManager;
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private final List<String> whitelistedIps = List.of("0:0:0:0:0:0:0:1", "127.0.0.1", "192.168.0.1", "192.168.0.2");

    public SecurityAuthenticationFilter(ReactiveRedisTemplate<String, String> redisTemplate, WebClient.Builder webClientBuilder, ReactiveAuthenticationManager authenticationManager) {
        super(authenticationManager);
        this.redisTemplate = redisTemplate;
        this.webClientBuilder = webClientBuilder;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return checkIpWhitelist(exchange)
                .flatMap(isWhitelisted -> {
                    if (!isWhitelisted) {
                        return onError(exchange, HttpStatus.FORBIDDEN, "Access denied");
                    }
                    return extractAndValidateToken(exchange)
                            .flatMap(auth -> {
                                SecurityContext context = SecurityContextHolder.createEmptyContext();
                                context.setAuthentication(auth);
                                exchange.getAttributes().put(SecurityContext.class.getName(), context);
                                return chain.filter(exchange);
                            });
                })
                .switchIfEmpty(onError(exchange, HttpStatus.FORBIDDEN, "Access denied"))
                .transform(call -> rateLimitCheck(exchange, call));
    }

    private Mono<Boolean> checkIpWhitelist(ServerWebExchange exchange) {
        String clientIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        log.info("Current request IP: {}", clientIp);
        return Mono.just(whitelistedIps.contains(clientIp));
    }

    private Mono<Authentication> extractAndValidateToken(ServerWebExchange exchange) {
        String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (token == null || token.isEmpty()) {
            return Mono.empty();
        }

        return getTokenFromRedis(token)
                .switchIfEmpty(handleRefreshToken(token))
                .flatMap(this::validateToken)
                .map(this::createAuthentication);
    }

    private Mono<String> getTokenFromRedis(String token) {
        String username = getUsernameFromToken(token);
        String category = getTokenCategory(token);
        String redisKey = generateRedisKey(category, username);
        return redisTemplate.opsForValue().get(redisKey);
    }

    private Mono<String> handleRefreshToken(String token) {
        String username = getUsernameFromToken(token);
        String refreshTokenKey = generateRedisKey("refresh_token", username);
        return redisTemplate.opsForValue().get(refreshTokenKey)
                .flatMap(refreshToken -> requestNewAccessToken(refreshToken, username));
    }

    private Mono<String> requestNewAccessToken(String refreshToken, String username) {
        return webClientBuilder.build()
                .post()
                .uri("http://localhost:19092/api/auth/reIssue?username={username}&role={role}",
                        username, getRoleFromToken(refreshToken))
                .retrieve()
                .bodyToMono(String.class);
    }

    private Mono<String> validateToken(String token) {
        return webClientBuilder.build()
                .get()
                .uri("http://localhost:19092/api/auth/verify?accessToken={token}", token)
                .retrieve()
                .bodyToMono(String.class);
    }

    private Authentication createAuthentication(String token) {
        String username = getUsernameFromToken(token);
        String role = getRoleFromToken(token);
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())
        );
        return new UsernamePasswordAuthenticationToken(username, null, authorities);
    }

    private Mono<Void> rateLimitCheck(ServerWebExchange exchange, Mono<Void> chain) {
        String clientId = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        String redisKey = "rate_limit:" + clientId;

        return redisTemplate.opsForValue().increment(redisKey)
                .flatMap(count -> {
                    if (count > MAX_REQUESTS_PER_MINUTE) {
                        return onError(exchange, HttpStatus.TOO_MANY_REQUESTS, "Too many requests");
                    }
                    return redisTemplate.expire(redisKey, Duration.ofMinutes(1))
                            .then(chain);
                });
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
            return null;
        }

        String payload = parts[1];
        String decodedPayload = new String(Base64.getUrlDecoder().decode(payload));
        return new JSONObject(decodedPayload).getString(type);
    }

    private String generateRedisKey(String category, String username) {
        return String.format("token:%s:%s", category.toLowerCase(), username.toLowerCase());
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String jsonResponse = String.format("{\"error\": \"%s\"}", message);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(jsonResponse.getBytes()))
        );
    }
}
