package com.module.server.gateway.constants;

public class ErrorMessage {
    public static final String UNAUTHORIZED = "로그인이 필요합니다.";
    public static final String INVALID_TOKEN = "유효하지 않은 토큰입니다.";
    public static final String ACCESS_DENIED = "접근 권한이 없습니다.";
    public static final String API_NOT_FOUND = "API 경로가 존재하지 않습니다.";
    public static final String LOGIN_REQUIRED = "로그인이 필요합니다.";
    public static final String AUTH_SERVER_ERROR = "인증 서비스 오류가 발생했습니다.";
    public static final String TOO_MANY_REQUESTS = "요청 수가 너무 많습니다. 잠시 후 다시 시도하세요.";
    public static final String ACCESS_DENIED_IP = "접근이 불가한 IP입니다.";
}
