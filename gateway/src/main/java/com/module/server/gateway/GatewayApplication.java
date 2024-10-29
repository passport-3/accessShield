package com.module.server.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;

@SpringBootApplication(
        exclude = {ReactiveSecurityAutoConfiguration.class},
        scanBasePackages = {
                "com.module.server.gateway",
                "com.module.server.accessshieldmodule"
        }
)
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}

