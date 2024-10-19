package com.module.server.auth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.module.server.auth.dto.UserInfoDto;
import com.module.server.auth.model.Api;
import com.module.server.auth.model.ApiRoleMapping;
import com.module.server.auth.repository.ApiRepository;
import com.module.server.auth.repository.ApiRoleMappingRepository;
import com.module.server.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TokenService tokenService;
    private final ApiRepository apiRepository;
    private final ApiRoleMappingRepository apiRoleMappingRepository;


    /**
     * access token, refresh token 생성 후 Redis에 저장
     * @param userInfo
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<String> getToken(@RequestBody UserInfoDto userInfo) {

        log.info("Received login request: userId = {}, role = {}", userInfo.getUsername(), userInfo.getRole());

        // 입력 검증
        if (userInfo.getUsername() == null || userInfo.getRole() == null) {
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
        log.info("Received verify request: accessToken = {}", accessToken);

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
        if (userInfo == null || userInfo.getUsername() == null || userInfo.getRole() == null) {
            return ResponseEntity.badRequest().body("Invalid login request: userId and role are required.");
        }

        String newAccessToken = "";
        try {
            newAccessToken = tokenService.reIssueToken(userInfo);
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
        if (userInfo == null || userInfo.getUsername() == null || userInfo.getRole() == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            tokenService.logout(userInfo);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * API 경로에 대한 허용된 역할을 반환하는 엔드포인트
     * @param apiPath API 경로
     * @return 허용된 역할 리스트
     */
    @GetMapping("/role")
    public ResponseEntity<List<String>> getRolesForApi(@RequestParam String apiPath) {
        // API 경로로 API 엔티티 조회
        Optional<Api> apiOpt = apiRepository.findByPath(apiPath);

        if (apiOpt.isPresent()) {
            Api api = apiOpt.get();

            // 해당 API에 허용된 역할들을 중간 테이블에서 가져옴
            List<ApiRoleMapping> apiRoleMappings = apiRoleMappingRepository.findByApi(api);
            List<String> allowedRoles = apiRoleMappings.stream()
                    .map(mapping -> mapping.getRole().getName())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(allowedRoles);  // 허용된 역할 리스트 반환
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);  // API 경로가 존재하지 않으면 404 반환
        }
    }



}
