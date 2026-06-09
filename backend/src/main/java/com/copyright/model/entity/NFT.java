package com.copyright.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * NFT实体类
 */
@Entity
@Table(name = "nft")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NFT {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_id", nullable = false, unique = true)
    private Work work;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "token_id", nullable = false, length = 128)
    private String tokenId;

    @Column(name = "contract_address", nullable = false, length = 128)
    private String contractAddress;

    @Column(name = "token_uri", length = 512)
    private String tokenUri;

    @Column(name = "metadata_cid", length = 128)
    private String metadataCid;

    @Column(name = "tx_hash", length = 128)
    private String txHash;

    @Column(name = "minted_at", nullable = false)
    @Builder.Default
    private LocalDateTime mintedAt = LocalDateTime.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
