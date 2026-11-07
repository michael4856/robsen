package com.robsen.robsen.customerOrder;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "order_status_history", indexes = @Index(name = "idx_order_history_order_created", columnList = "order_id, created_at"))
public class OrderStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;
    @Column(length = 32)
    private String previousStatus;
    @Column(nullable = false, length = 32)
    private String newStatus;
    @Column(nullable = false)
    private Long changedByUserId;
    @Column(length = 2000)
    private String remarks;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
