package com.module.server.gateway.util;

import org.springframework.stereotype.Component;

@Component
public class RedisKeyUtil {
    private static final String TOKEN_KEY_FORMAT = "token:%s:%s";
    private static final String RATE_LIMIT_KEY_FORMAT = "rate_limit:%s";
    private static final String ROLE_API_KEY_FORMAT = "role_api:%s";

    public String generateTokenKey(String category, String username) {
        return String.format(TOKEN_KEY_FORMAT,
                category.toLowerCase(),
                username.toLowerCase()
        );
    }

    public String generateRateLimitKey(String clientId) {
        return String.format(RATE_LIMIT_KEY_FORMAT, clientId);
    }

    public String generateRoleApiKey(String role) {
        return String.format(ROLE_API_KEY_FORMAT, role);
    }
}

