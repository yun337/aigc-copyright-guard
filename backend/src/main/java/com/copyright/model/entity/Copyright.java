package com.copyright.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 版权存证实体类
 */
@Entity
@Table(name = "copyright")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Copyright {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_id", nullable = false, unique = true)
    private Work work;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "certificate_id", nullable = false, unique = true, length = 128)
    private String certificateId;

    @Column(name = "tx_hash", nullable = false, length = 128)
    private String txHash;

    @Column(name = "block_number")
    private Long blockNumber;

    @Column(name = "contract_address", length = 128)
    private String contractAddress;

    @Column(name = "cert_ipfs_cid", length = 128)
    private String certIpfsCid;

    @Column(name = "registered_at", nullable = false)
    @Builder.Default
    private LocalDateTime registeredAt = LocalDateTime.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
