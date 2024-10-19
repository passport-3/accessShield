package com.module.server.user.client;

import com.module.server.user.dto.UserInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("auth-service")
public interface AuthServiceClient {

    @PostMapping("/api/auth/login")
    ResponseEntity<String> login(@RequestBody UserInfoDto userInfoDto);

    @DeleteMapping("/api/auth/logout")
    ResponseEntity<Void> logout(@RequestBody UserInfoDto userInfoDto);
}
