package com.module.server.auth.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "api_role_mapping")
public class ApiRoleMapping { // 중간테이블

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="api_role_mapping_id", updatable = false, nullable = false)
    private UUID apiRoleMappingId;

    @ManyToOne
    @JoinColumn(name = "api_id")
    private Api api;  // API 엔티티와 연관 관계

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;  // Role 엔티티와 연관 관계

}
