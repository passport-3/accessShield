package com.module.server.auth.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "p_api")
public class Api {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="api_id", updatable = false, nullable = false)
    private UUID apiId;

    @Column(nullable = false, unique = true)
    private String path;  // API 경로

    @OneToMany(mappedBy = "api")
    private Set<ApiRoleMapping> apiRoleMappings = new HashSet<>();  // API와 ApiRoleMapping 관계
}

