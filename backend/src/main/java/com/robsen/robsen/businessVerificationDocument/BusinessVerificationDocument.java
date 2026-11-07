package com.robsen.robsen.businessVerificationDocument;

import java.time.LocalDateTime;

import com.robsen.robsen.businessVerification.BusinessVerification;
import com.robsen.robsen.common.ReviewStatus;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data 
public class BusinessVerificationDocument {
    public enum DocumentType {
        BUSINESS_LICENSE, TAX_IDENTIFICATION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_id", nullable = false)
    private BusinessVerification verification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DocumentType documentType;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, unique = true, length = 512)
    private String storageKey;

    @Column(nullable = false, length = 128)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(nullable = false)
    private Long uploadedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReviewStatus documentStatus;

    @Column(length = 2000)
    private String reviewComment;
}
