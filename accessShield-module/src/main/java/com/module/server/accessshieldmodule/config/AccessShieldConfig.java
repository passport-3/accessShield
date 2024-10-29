package com.module.server.accessshieldmodule.config;

import com.module.server.accessshieldmodule.filter.AccessShield;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.WebFilter;

@Configuration
public class AccessShieldConfig {

    @Bean
    public WebFilter accessShieldWebFilter(WebClient.Builder webClientBuilder,
                                           ReactiveRedisTemplate<String, String> redisTemplate) {
        AccessShield filter = new AccessShield(webClientBuilder, redisTemplate);
        // TODO: properties에서 설정값을 필터에 적용
        return filter;
    }
}
