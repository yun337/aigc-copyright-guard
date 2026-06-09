package com.copyright.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CopyrightRegisterRequest {

    @NotNull(message = "作品ID不能为空")
    private Long workId;

    private String workTitle;

    private String workType;
}
