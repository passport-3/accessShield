package com.module.server.auth.client;

import com.module.server.auth.dto.UserInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "user-service")
public interface UserServiceClient {

    @PostMapping("/api/user/login")
    ResponseEntity<String> login(@RequestBody UserInfoDto userInfoDto);
}
