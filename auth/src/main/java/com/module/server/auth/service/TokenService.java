package com.module.server.auth.service;

import com.module.server.auth.dto.UserInfoDto;
import com.module.server.auth.jwt.JwtTokenUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.module.server.auth.model.AuthToken;
import com.module.server.auth.type.Const;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final JwtTokenUtil jwtTokenUtil;

    @Autowired
    public TokenService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper, JwtTokenUtil jwtTokenUtil) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    public String login(UserInfoDto userInfo) throws JsonProcessingException {
        String username = userInfo.getUsername();
        String role = userInfo.getRole();

        String accessToken = this.generateToken(Const.ACCESS_TOKEN, username, role);
        String refreshToken = this.generateToken(Const.REFRESH_TOKEN, username, role);

        return accessToken;
    }

    public String generateToken(String category, String username, String role) throws JsonProcessingException {

        long expireTime = category.equals(Const.ACCESS_TOKEN) ? Const.ACCESS_TOKEN_EXPIRES_IN : Const.REFRESH_TOKEN_EXPIRES_IN;

        // 토큰 생성
        String token = jwtTokenUtil.createToken(Const.ACCESS_TOKEN, username, role, expireTime);

        AuthToken tokenData = AuthToken.TokenBuilder()
                .userId(username)
                .category(category)
                .expireTime(expireTime)
                .build();

        // 토큰 저장
        String key = this.generateRedisKey(category, username);
        this.saveTokenData(key, tokenData);

        return token;
    }

    private String generateRedisKey(String category, String username) {
        return String.format("token:%s:%s", category.toLowerCase(), username.toLowerCase());
    }


    private void saveTokenData(String key, AuthToken tokenData) throws JsonProcessingException {
        String jsonData = objectMapper.writeValueAsString(tokenData);
        log.info("============> Token Data {}", jsonData);
        redisTemplate.opsForValue().set(key, jsonData, tokenData.getExpireTime(), TimeUnit.MILLISECONDS);

        String savedData = redisTemplate.opsForValue().get(key);
        log.info("============> Saved Data {}", savedData);
    }

    public AuthToken getTokenData(String category, String username) throws JsonProcessingException {
        String key = generateRedisKey(category, username);
        return this.getTokenData(key);
    }

    public AuthToken getTokenData(String key) throws JsonProcessingException {
        String jsonData = redisTemplate.opsForValue().get(key);

        log.info("============>key {}, Get Token Data {}", key, jsonData);
        if (jsonData == null) {
            throw new IllegalArgumentException("Token not found for key: " + key);
        }
        return objectMapper.readValue(jsonData, AuthToken.class);
    }

    public boolean validateToken(String token) {
        return jwtTokenUtil.validateToken(token);
    }

    public String reIssueToken(UserInfoDto userInfo) throws JsonProcessingException {
        String username = userInfo.getUsername();
        String role = userInfo.getRole();

        // refresh token이 존재하는지 확인
        String refreshTokenKey = this.generateRedisKey(Const.REFRESH_TOKEN, username);

        String refreshToken = String.valueOf(this.getTokenData(refreshTokenKey));

        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("Refresh token is not valid or does not exist.");
        }

        return this.generateToken(Const.ACCESS_TOKEN, username, role);

    }

    public void logout(UserInfoDto userInfo) throws JsonProcessingException {
        String userId = userInfo.getUsername();
        String role = userInfo.getRole();
        // access token 삭제
        String accessToken = this.generateToken(Const.ACCESS_TOKEN, userId, role);
        this.deleteTokenData(accessToken);

        // refresh token 삭제
        String refreshTokenKey = this.generateRedisKey(Const.REFRESH_TOKEN, userId);
        this.deleteTokenData(refreshTokenKey);
    }


    private void deleteTokenData(String key) {
        redisTemplate.delete(key);
    }
}
