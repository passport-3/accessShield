package com.module.server.auth.repository;

import com.module.server.auth.model.Api;
import com.module.server.auth.model.ApiRoleMapping;
import com.module.server.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// API와 Role간 매핑 정보 조회
public interface ApiRoleMappingRepository extends JpaRepository<ApiRoleMapping, Long> {
    List<ApiRoleMapping> findByApi(Api api);  // 특정 API에 대한 역할 조회
}

