package com.module.server.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class UserInfoDto {
    private String userId;
    private String role;

    public UserInfoDto(String userId, String role) {
        this.userId = userId;
        this.role = role;
    }
}
