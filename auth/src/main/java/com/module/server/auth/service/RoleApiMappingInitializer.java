package com.module.server.auth.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* 역할에 맞는 API 매핑 정보를 Redis에 저장한다.
* */
@Service
public class RoleApiMappingInitializer {

    private final RoleApiMappingService roleApiMappingService;

    public RoleApiMappingInitializer(RoleApiMappingService roleApiMappingService) {
        this.roleApiMappingService = roleApiMappingService;
    }

    @PostConstruct
    public void initializeApiMappings() {
        // 예시: admin은 모든 API에 접근 가능
        List<String> adminApis = List.of("/admin/**", "/user/**", "/manage/**");
        roleApiMappingService.saveRoleApiMapping("admin", adminApis).subscribe();

        // 예시: user는 사용자 API에만 접근 가능
        List<String> userApis = List.of("/user/**");
        roleApiMappingService.saveRoleApiMapping("user", userApis).subscribe();
    }
}
