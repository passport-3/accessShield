package com.module.server.gateway.util;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Base64;

@Slf4j
@Component
public class JwtUtil {
    public record TokenInfo(String username, String role, String category) {}

    public Optional<TokenInfo> parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return Optional.empty();

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JSONObject jsonPayload = new JSONObject(payload);

            return Optional.of(new TokenInfo(
                    jsonPayload.getString("username"),
                    jsonPayload.getString("role"),
                    jsonPayload.getString("category")
            ));
        } catch (Exception e) {
            log.warn("Failed to parse JWT token", e);
            return Optional.empty();
        }
    }
}