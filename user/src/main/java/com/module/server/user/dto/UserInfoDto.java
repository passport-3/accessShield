package com.module.server.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class UserInfoDto {
    private String username;
    private String role;
    private String password;

    public UserInfoDto(String username, String role, String password) {
        this.username = username;
        this.role = role;
        this.password = password;
    }
}
