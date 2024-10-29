package com.module.server.gateway.config;


import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;

@Configuration
@EnableWebFlux
@ComponentScan(basePackages = "com.module.server.accessshieldmodule")
public class GatewaySecurityConfig {
    // AccessShieldAutoConfiguration에서 자동으로 필터가 등록되므로
    // 추가 설정이 필요없습니다.
}