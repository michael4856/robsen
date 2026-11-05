package com.robsen.robsen.role;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import com.robsen.robsen.business.Business;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Table(name = "business_role_permission_configurations", uniqueConstraints =
        @UniqueConstraint(name = "uk_business_role_permission_config", columnNames = { "business_id", "role_id" }))
@Data
public class BusinessRolePermissionConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "business_role_enabled_permissions", joinColumns = @JoinColumn(name = "configuration_id"))
    @Column(name = "permission", nullable = false, length = 100)
    private Set<String> enabledPermissions = new LinkedHashSet<>();

    @Column(nullable = false)
    private int defaultsVersion = 1;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    private Long updatedByUserId;
}
