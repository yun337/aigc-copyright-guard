package com.copyright.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MintNFTRequest {

    @NotNull(message = "作品ID不能为空")
    private Long workId;

    @Min(value = 0, message = "版税比例范围为0-10000")
    @Max(value = 10000, message = "版税比例范围为0-10000")
    private Integer royaltyRate;
}
