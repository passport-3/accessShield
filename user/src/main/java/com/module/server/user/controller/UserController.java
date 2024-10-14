package com.module.server.user.controller;

import com.module.server.user.dto.LoginRequestDto;
import com.module.server.user.dto.RegisterRequestDto;
import com.module.server.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    // 로그인
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDto loginRequestDto) {

        String accessToken = userService.login(loginRequestDto.getUsername(), loginRequestDto.getPassword());

        // accessToken을 응답으로 반환
        return ResponseEntity.ok(accessToken);
    }
    
    // 회원가입
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequestDto registerReqeustDto){
        userService.register(registerReqeustDto);
        return ResponseEntity.ok("회원가입 성공");
    }

}
