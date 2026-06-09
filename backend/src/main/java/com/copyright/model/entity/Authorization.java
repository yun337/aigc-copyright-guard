package com.copyright.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 授权交易实体类
 */
@Entity
@Table(name = "authorization")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Authorization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "copyright_id", nullable = false)
    private Copyright copyright;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licensor_id", nullable = false)
    private User licensor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licensee_id", nullable = false)
    private User licensee;

    @Column(name = "license_type", nullable = false, length = 32)
    @Builder.Default
    private String licenseType = "EXCLUSIVE";

    @Column(name = "license_fee", precision = 18, scale = 6)
    private BigDecimal licenseFee;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "tx_hash", length = 128)
    private String txHash;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
