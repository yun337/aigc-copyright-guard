package com.copyright.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DetectionRequest {

    @NotNull(message = "待检测作品ID不能为空")
    private Long workId;

    private Double threshold;

    private Integer maxResults;

    private String detectionType;
}
