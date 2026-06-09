package com.copyright.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AuthorizationRequest {

    @NotNull(message = "版权ID不能为空")
    private Long copyrightId;

    @NotNull(message = "被授权人ID不能为空")
    private Long licenseeId;

    private String licenseType;

    private BigDecimal licenseFee;

    private Integer durationDays;
}
