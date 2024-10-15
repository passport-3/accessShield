package com.module.server.auth.type;

public class Const {

    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";

    public static final Long ACCESS_TOKEN_EXPIRES_IN = 600000L;     // 10분
    public static final Long REFRESH_TOKEN_EXPIRES_IN = 86400000L;  // 24시간
}
