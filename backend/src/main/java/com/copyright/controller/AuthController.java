package com.copyright.controller;

import com.copyright.model.dto.LoginRequest;
import com.copyright.model.dto.RegisterRequest;
import com.copyright.model.vo.ApiResponse;
import com.copyright.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<?> register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse<?> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("未登录");
        }
        return userService.getUserInfo(userId);
    }

    /**
     * 绑定钱包地址
     */
    @PostMapping("/bind-wallet")
    public ApiResponse<?> bindWallet(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("未登录");
        }
        String walletAddress = body.get("walletAddress");
        return userService.bindWallet(userId, walletAddress);
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public ApiResponse<?> refreshToken(HttpServletRequest request) {
        // 在JWT过滤器中处理
        return ApiResponse.success("Token已刷新");
    }
}
