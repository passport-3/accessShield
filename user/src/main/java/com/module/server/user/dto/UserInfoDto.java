package com.module.server.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
public class UserInfoDto {
    private String username;
    private String role;

    public UserInfoDto(String username, String role) {
        this.username = username;
        this.role = role;

    }
}
