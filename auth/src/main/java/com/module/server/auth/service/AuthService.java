package com.module.server.auth.service;

import com.module.server.auth.dto.UserInfoDto;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
//    // 로그인
//    public String login(UserInfoDto userInfoDto) {
//
//        // 1. 사용자 정보 확인 (UserInfoDto에 사용자 ID 및 역할 정보 포함)
//        String userId = userInfoDto.getUserId();
//        String role = userInfoDto.getRole();
//
////        // 2. JWT 토큰 생성
////        String accessToken = jwtTokenUtil.createToken();
////        String refreshToken = jwtTokenUtil.createToken();
//
//        // 3. refreshToken은 redis로 적재
//        // redisTemplate.
//        // redisTemplate.opsForValue().set(refreshToken, userId, jwtTokenUtil.getRefreshTokenExpiration(), TimeUnit.MILLISECONDS);
//
////        return accessToken; // access token 반환
//        return null;
//    }
}
