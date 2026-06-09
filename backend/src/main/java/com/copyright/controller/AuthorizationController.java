package com.copyright.controller;

import com.copyright.model.dto.AuthorizationRequest;
import com.copyright.model.vo.ApiResponse;
import com.copyright.service.AuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 授权交易控制器
 */
@RestController
@RequestMapping("/authorization")
@RequiredArgsConstructor
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    /**
     * 创建版权授权
     */
    @PostMapping("/grant")
    public ApiResponse<?> grantAuthorization(
            HttpServletRequest request,
            @Valid @RequestBody AuthorizationRequest authRequest) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("未登录");
        }
        return authorizationService.createAuthorization(userId, authRequest);
    }

    /**
     * 获取授权详情
     */
    @GetMapping("/{authId}")
    public ApiResponse<?> getAuthorization(@PathVariable Long authId) {
        return authorizationService.getAuthorization(authId);
    }

    /**
     * 获取我授权的记录（作为版权方）
     */
    @GetMapping("/my-grants")
    public ApiResponse<?> getMyGrants(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("未登录");
        }
        return authorizationService.getLicensorAuthorizations(userId);
    }

    /**
     * 获取我获得的授权（作为被授权方）
     */
    @GetMapping("/my-licenses")
    public ApiResponse<?> getMyLicenses(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("未登录");
        }
        return authorizationService.getLicenseeAuthorizations(userId);
    }

    /**
     * 撤销授权
     */
    @PostMapping("/revoke/{authId}")
    public ApiResponse<?> revokeAuthorization(
            HttpServletRequest request,
            @PathVariable Long authId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("未登录");
        }
        return authorizationService.revokeAuthorization(authId, userId);
    }
}
