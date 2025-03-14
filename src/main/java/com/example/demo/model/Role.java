package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

public class Role {
    @Id
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;
}
