package com.module.server.auth.jwt;


import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Key;
import java.util.Date;

@Slf4j(topic = "JwtTokenUtil")
@Component
public class JwtTokenUtil {
    // Header KEY 값
    public static final String AUTHORIZATION_HEADER = "Authorization";
    // 사용자 권한 값의 KEY
    public static final String AUTHORIZATION_KEY = "role";
    // Token 식별자
    public static final String BEARER_PREFIX = "Bearer ";
    // 토큰 만료시간
    private final long TOKEN_TIME = 60 * 60 * 1000L; // 60분

    @Value("${service.jwt.secret-key}") // Base64 Encode 한 SecretKey
    private String secretKey;
    private Key key;
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    // 로그 설정
    public static final Logger logger = LoggerFactory.getLogger("JWT TokenUtil 관련 로그");

    @PostConstruct
    public void init() {
        key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
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

//    // IP 주소 가져오기
//    public String getIp(HttpServletRequest request) {
//        // HTTP 요청 헤더 "X-Forwarded-For"에서 IP 주소를 먼저 가져옴
//        String ip = request.getHeader("X-Forwarded-For");
//
//        // 만약 "X-Forwarded-For" 헤더가 null이거나 비어있거나 "unknown"일 경우, 다른 헤더에서 IP를 가져옴
//        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
//            ip = request.getHeader("Proxy-Client-IP");
//        }
//
//        // 여전히 IP가 없으면 "WL-Proxy-Client-IP" 헤더에서 가져옴
//        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
//            ip = request.getHeader("WL-Proxy-Client-IP");
//        }
//
//        // 그래도 IP를 찾지 못하면, 기본적으로 request 객체에서 원격 IP를 가져옴 (보통 클라이언트의 실제 IP)
//        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
//            ip = request.getRemoteAddr();
//        }
//
//        // 최종적으로 찾은 IP 주소 반환
//        return ip;
//    }
//
//    // 클라이언트 디바이스 정보를 가져오는 메서드 (보통 브라우저 정보)
//    public String getDeviceInfo(HttpServletRequest request) {
//        // HTTP 요청 헤더 "User-Agent"에서 클라이언트 디바이스 정보(브라우저, OS 등)를 가져옴
//        return request.getHeader("User-Agent");
//    }


    // 생성된 JWT를 Cookie에 저장
    // JWT Cookie 에 저장
    public void addJwtToCookie(String token, HttpServletResponse res) {
        try {
            token = URLEncoder.encode(token, "utf-8").replaceAll("\\+", "%20"); // Cookie Value 에는 공백이 불가능해서 encoding 진행

            Cookie cookie = new Cookie(AUTHORIZATION_HEADER, token); // Name-Value
            cookie.setPath("/");
            cookie.setHttpOnly(true); // javascript 접근 방지

            // Response 객체에 Cookie 추가
            res.addCookie(cookie);
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        }
    }

    // Cookie에 들어있던 JWT토큰 SubString
    public String substringToken(String tokenValue) {
        if (StringUtils.hasText(tokenValue) && tokenValue.startsWith(BEARER_PREFIX)) {
            return tokenValue.substring(7);
        }
        logger.error("Not Found Token");
        throw new NullPointerException("Not Found Token");
    }

    // 토큰 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException | SignatureException e) {
            logger.error("Invalid JWT signature, 유효하지 않는 JWT 서명 입니다.");
        } catch (ExpiredJwtException e) {
            logger.error("Expired JWT token, 만료된 JWT token 입니다.");
        } catch (UnsupportedJwtException e) {
            logger.error("Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.");
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims is empty, 잘못된 JWT 토큰 입니다.");
        }
        return false;
    }

    // 토큰에서 사용자 정보 가져오기
    public Claims getUserInfoFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    public String getTokenFromRequest(HttpServletRequest req) {
        return null;
    }

    // 토큰 종류 가져오기
    public String getCategoryFromToken(String token) {
        return getClaimFromToken(token, "category");
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, "username");
    }

    public String getRoleFromToken(String token) {
        return getClaimFromToken(token, "role");
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

}

