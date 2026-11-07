package com.robsen.robsen.businessVerification;

import java.time.LocalDateTime;
import com.robsen.robsen.common.VerificationStatus;
import jakarta.persistence.*;
import lombok.Data;

@Data @Entity @Table(name="business_verification_reviews", indexes=@Index(name="idx_verification_review_created", columnList="verification_id, created_at"))
public class BusinessVerificationReview {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="verification_id", nullable=false) private BusinessVerification verification;
    @Enumerated(EnumType.STRING) @Column(length=32) private VerificationStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=32) private VerificationStatus newStatus;
    @Column(nullable=false) private Long reviewerUserId;
    @Column(length=2000) private String reason;
    @Column(length=4000) private String notes;
    @Column(nullable=false, updatable=false) private LocalDateTime createdAt=LocalDateTime.now();
}
