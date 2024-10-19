package com.module.server.auth.repository;

import com.module.server.auth.model.Api;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiRepository extends JpaRepository<Api, Long> {
    Optional<Api> findByPath(String path);
}
