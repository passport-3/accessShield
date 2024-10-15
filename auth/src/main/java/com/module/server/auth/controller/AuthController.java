package com.module.server.auth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.module.server.auth.dto.UserInfoDto;
import com.module.server.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TokenService tokenService;

    /**
     * access token, refresh token 생성 후 Redis에 저장
     * @param userInfo
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<String> getToken(@RequestBody UserInfoDto userInfo) {
        // 입력 검증
        if (userInfo == null || userInfo.getUserId() == null || userInfo.getRole() == null) {
            return ResponseEntity.badRequest().body("Invalid login request: username and role are required.");
        }

        String accessToken = "";
        try {
            accessToken = tokenService.login(userInfo);

            log.info("엑세스토큰 {} ", accessToken);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(accessToken);
    }

    /**
     * 토큰 검증
     * @param accessToken
     * @return
     */
    @GetMapping("/verify")
    public ResponseEntity<Boolean> verify(@RequestParam String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return ResponseEntity.badRequest().body(false);
        }

        boolean isValid = tokenService.validateToken(accessToken);
        log.info("token {}, result {}", accessToken, isValid);

        return ResponseEntity.ok(isValid);
    }

//    /**
//     * 사용자 id로 토큰 정보 조회
//     *
//     * @param category
//     * @param username
//     * @return
//     */
//    @GetMapping("/token")
//    public ResponseEntity<String> getToken(@RequestParam String category, @RequestParam String username)  {
//
//        try {
//            AuthToken tokenData = tokenService.getTokenData(category, username);
//            log.info("tokenData {}", tokenData);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//        return null;
//    }

    /**
     * accesst token 재발급
     * @param userInfo
     * @return
     */
    @PostMapping("/reIssue")
    public ResponseEntity<String> reIssue(@RequestBody UserInfoDto userInfo) {
        // 입력 검증
        if (userInfo == null || userInfo.getUserId() == null || userInfo.getRole() == null) {
            return ResponseEntity.badRequest().body("Invalid login request: userId and role are required.");
        }

        String newAccessToken = "";
        try {
            newAccessToken =  tokenService.reIssueToken(userInfo);
            log.info("=============> NEW AccessToken {}", newAccessToken);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(newAccessToken);
    }

    /**
     * 로그아웃 시 토큰 삭제
     * @param userInfo
     * @return
     */
    @DeleteMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody UserInfoDto userInfo) {
        if (userInfo == null || userInfo.getUserId() == null || userInfo.getRole() == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            tokenService.logout(userInfo);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok().build();
    }

}
