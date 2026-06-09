package com.copyright.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 侵权检测记录实体类
 */
@Entity
@Table(name = "infringement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Infringement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_id", nullable = false)
    private Work work;

    @Column(name = "similar_work_id")
    private Long similarWorkId;

    @Column(name = "similar_work_cid", length = 128)
    private String similarWorkCid;

    @Column(name = "similarity_score", nullable = false)
    @Builder.Default
    private Double similarityScore = 0.0;

    @Column(name = "detection_type", nullable = false, length = 32)
    @Builder.Default
    private String detectionType = "SEMANTIC";

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "report_url", length = 512)
    private String reportUrl;

    @Column(name = "detected_at", nullable = false)
    @Builder.Default
    private LocalDateTime detectedAt = LocalDateTime.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
