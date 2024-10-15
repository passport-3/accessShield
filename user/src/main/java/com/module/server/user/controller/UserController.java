package com.module.server.user.controller;

import com.module.server.user.dto.LoginRequestDto;
import com.module.server.user.dto.RegisterRequestDto;
import com.module.server.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
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
        log.info("login request: {}", loginRequestDto);

        String accessToken = userService.login(loginRequestDto.getUsername());

        return ResponseEntity.ok(accessToken);
    }
    
    // 회원가입
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequestDto registerReqeustDto){
        userService.register(registerReqeustDto);
        return ResponseEntity.ok("회원가입 성공");
    }

    @GetMapping("/test")
    public String test(){
        return "test";
    }

}
