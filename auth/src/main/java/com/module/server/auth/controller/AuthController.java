package com.module.server.auth.controller;

import com.module.server.auth.dto.TokenResponseDto;
import com.module.server.auth.dto.UserInfoDto;
import com.module.server.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {


    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@RequestBody UserInfoDto userInfoDto) {
        // 1. AuthService에서 로그인 처리 및 JWT 토큰 발급
        String accessToken = authService.login(userInfoDto);
        String refreshToken = null; // authService.createRefreshToken(userInfoDto.getUserId());

        // 2. 토큰을 클라이언트에게 응답으로 반환
        TokenResponseDto tokenResponseDto = new TokenResponseDto(accessToken, refreshToken);
        return ResponseEntity.ok(tokenResponseDto);
    }
}
