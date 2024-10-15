package com.module.server.auth.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
public class LoginUserInfo {

    private String username;
    private String role;

    @Builder(builderClassName = "LoginUserInfoBuilder", builderMethodName = "LoginUserInfoBuilder")
    public LoginUserInfo(String username, String role) {
        this.username = username;
        this.role = role;
    }
}
