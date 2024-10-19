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
@Table(name = "p_role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="role_id", updatable = false, nullable = false)
    private UUID roleId;

    @Column(nullable = false, unique = true)
    private String name;  // 역할 이름 (ADMIN, USER)

    @OneToMany(mappedBy = "role")
    private Set<ApiRoleMapping> apiRoleMappings = new HashSet<>();  // Role과 ApiRoleMapping 관계

}
