package com.copyright.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 作品实体类
 */
@Entity
@Table(name = "work")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Work {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 256)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "prompt_info", columnDefinition = "TEXT")
    private String promptInfo;

    @Column(name = "work_type", nullable = false, length = 32)
    @Builder.Default
    private String workType = "IMAGE";

    @Column(name = "file_hash", nullable = false, length = 128)
    private String fileHash;

    @Column(name = "ipfs_cid", length = 128)
    private String ipfsCid;

    @Column(name = "metadata_cid", length = 128)
    private String metadataCid;

    @Column(name = "feature_vector", columnDefinition = "JSON")
    private String featureVector;

    @Column(name = "file_size")
    @Builder.Default
    private Long fileSize = 0L;

    @Column(name = "copyright_status", length = 32)
    @Builder.Default
    private String copyrightStatus = "UNREGISTERED";

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
