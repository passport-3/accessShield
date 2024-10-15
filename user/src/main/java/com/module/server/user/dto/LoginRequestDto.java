package com.module.server.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 로그인 요청 객체
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {
    private String username;
    private String password;
}
