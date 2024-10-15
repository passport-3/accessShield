package com.module.server.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.security.SignatureException;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenUtil {
    // Header KEY
    public static final String AUTHORIZATION_HEADER = "Authorization";
    // 사용자 권한 값의 KEY
    public static final String AUTHORIZATION_KEY = "role";
    // Token 식별자
    public static final String BEARER_PREFIX = "Bearer ";

    @Value("${service.jwt.secret-key}") // Base64 Encode 한 SecretKey
    private String secretKey;
    private Key key;
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;


    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKey));
    }

    public String createToken(String category, String username, String role, Long expireTime) {
        return BEARER_PREFIX +
                Jwts.builder()
                        .setSubject(username)               // 사용자 식별자값(ID)
                        .claim("category", category)     // 토큰 종류 access / refresh
                        .claim(AUTHORIZATION_KEY, role)     // 사용자 권한
                        .setExpiration(new Date(System.currentTimeMillis() + expireTime)) // 만료 시간
                        .setIssuedAt(new Date(System.currentTimeMillis())) // 발급일
                        .signWith(key, signatureAlgorithm) // 암호화 알고리즘
                        .compact();
    }

    public boolean validateToken(String token) {
        try {
            if (token.startsWith(BEARER_PREFIX)) {
                token = token.substring(BEARER_PREFIX.length());
            }

            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true; // 토큰이 유효함
        } catch (SecurityException e) {
            log.error("SecurityException: " + e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("MalformedJwtException: " + e.getMessage());
        } catch (SignatureException e) {
            log.error("SignatureException: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("ExpiredJwtException: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("UnsupportedJwtException: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("IllegalArgumentException: " + e.getMessage());
        }
        return false; // 토큰이 유효하지 않음
    }

    private <T> T getClaimFromToken(String token, String claimKey) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token.replace("Bearer ", ""))
                    .getBody();
            return claims.get(claimKey, (Class<T>) Object.class); // Generic 타입으로 클레임 반환
        } catch (Exception e) {
            // 예외 처리: 유효하지 않은 토큰일 경우 null 반환
            return null;
        }
    }

    // 토큰 종류 가져오기
    public String getCategoryFromToken(String token) {
        return getClaimFromToken(token, "category");
    }

    // 사용자 id 가져오기
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, "username");
    }

    // 사용자 권한 가져오기
    public String getRoleFromToken(String token) {
        return getClaimFromToken(token, "role");
    }
}
