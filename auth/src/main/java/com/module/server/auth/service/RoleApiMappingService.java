package com.module.server.auth.service;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

// 애플리케이션 시작시 역할별로 접근 가능한 API목록을 Redis에 저장한다.
@Service
public class RoleApiMappingService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public RoleApiMappingService(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 역할과 API 매핑 정보를 Redis에 저장
    public Mono<Void> saveRoleApiMapping(String role, List<String> apiList) {
        String redisKey = "role_api:" + role;  // Redis에서 역할에 대한 키
        return redisTemplate.opsForSet().add(redisKey, apiList.toArray(new String[0])).then();
    }

    // 역할에 대한 API 목록을 Redis에서 삭제
    public Mono<Void> removeRoleApiMapping(String role) {
        String redisKey = "role_api:" + role;
        return redisTemplate.delete(redisKey).then();
    }


}

