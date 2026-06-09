package com.copyright.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkUploadRequest {

    @NotBlank(message = "作品标题不能为空")
    private String title;

    private String description;

    private String promptInfo;

    @NotBlank(message = "作品类型不能为空")
    private String workType;
}
