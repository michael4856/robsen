package com.robsen.robsen.role;

import java.time.LocalDateTime;

import com.robsen.robsen.business.Business;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "business_role_permission_audit")
@Data
public class BusinessRolePermissionAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;
    @Column(nullable = false, length = 40)
    private String roleName;
    @Column(nullable = false, length = 100)
    private String permission;
    @Column(nullable = false)
    private boolean previousEnabled;
    @Column(nullable = false)
    private boolean newEnabled;
    @Column(nullable = false)
    private Long actorUserId;
    @Column(nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();
}
