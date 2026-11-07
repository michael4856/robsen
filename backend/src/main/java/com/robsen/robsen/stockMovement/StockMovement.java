package com.robsen.robsen.stockMovement;

import java.time.LocalDateTime;

import com.robsen.robsen.common.MovementType;
import com.robsen.robsen.common.ReferenceType;
import com.robsen.robsen.productListing.ProductListing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "stock_movements")
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_listing_id", nullable = false)
    private ProductListing productListing;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MovementType movementType;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int previousQuantity;

    @Column(nullable = false)
    private int newQuantity;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(length = 2000)
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ReferenceType referenceType;

    private Long referenceId;
    private Long performedByUserId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
