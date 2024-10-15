package com.module.server.auth.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class AuthToken {

    @JsonProperty("username")
    private String username;

    @JsonProperty("category")
    private String category;

    @JsonProperty("expireTime")
    private long expireTime;

    @Builder(builderClassName = "TokenBuilder", builderMethodName = "TokenBuilder")
    public AuthToken(String username, String category, long expireTime) {
        this.username = username;
        this.category = category;
        this.expireTime = expireTime;
    }
}
