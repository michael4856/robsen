package com.robsen.robsen.businessVerification;

import java.time.LocalDateTime;

import com.robsen.robsen.business.Business;
import com.robsen.robsen.common.VerificationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "business_verifications")
public class BusinessVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "business_id", nullable = false, unique = true)
    private Business business;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VerificationStatus status = VerificationStatus.NOT_SUBMITTED;

    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private Long reviewedByUserId;

    @Column(length = 2000)
    private String rejectionReason;

    @Column(length = 4000)
    private String reviewNotes;

    private LocalDateTime updatedAt = LocalDateTime.now();
}
