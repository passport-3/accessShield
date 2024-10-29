//import com.module.server.gateway.filter.SecurityAuthenticationFilter;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.core.ReactiveRedisTemplate;
//import org.springframework.security.authentication.ReactiveAuthenticationManager;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
//import org.springframework.security.config.web.server.ServerHttpSecurity;
//import org.springframework.security.web.server.SecurityWebFilterChain;
//import org.springframework.web.reactive.function.client.WebClient;
//
//@Configuration
//@EnableWebSecurity
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//    private final ReactiveRedisTemplate<String, String> redisTemplate;
//    private final WebClient.Builder webClientBuilder;
//    private final ReactiveAuthenticationManager authenticationManager;
//
//    @Bean(name = "customSecurityFilterChain")
//    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
//        return http
//                .csrf(ServerHttpSecurity.CsrfSpec::disable)
//                .authorizeExchange(exchanges -> exchanges
//                        .pathMatchers(getExcludedPaths()).permitAll()
//                        .anyExchange().authenticated()
//                )
//                .addFilterAt(new SecurityAuthenticationFilter(redisTemplate, webClientBuilder, authenticationManager), SecurityWebFiltersOrder.AUTHENTICATION)
//                .build();
//    }
//
//    private String[] getExcludedPaths() {
//        return new String[] {
//                "/api/auth/**",
//                "/api/user/register",
//                "/api/user/login"
//        };
//    }
//}
