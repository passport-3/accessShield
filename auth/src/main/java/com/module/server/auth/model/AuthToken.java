package com.module.server.auth.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class AuthToken {

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("category")
    private String category;

    @JsonProperty("expireTime")
    private long expireTime;

    @Builder(builderClassName = "TokenBuilder", builderMethodName = "TokenBuilder")
    public AuthToken(String userId, String category, long expireTime) {
        this.userId = userId;
        this.category = category;
        this.expireTime = expireTime;
    }
}
