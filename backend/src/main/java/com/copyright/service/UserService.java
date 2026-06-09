package com.copyright.service;

import com.copyright.model.dto.LoginRequest;
import com.copyright.model.dto.RegisterRequest;
import com.copyright.model.entity.User;
import com.copyright.model.vo.ApiResponse;
import com.copyright.repository.UserRepository;
import com.copyright.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 用户管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 用户注册
     */
    @Transactional
    public ApiResponse<?> register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ApiResponse.badRequest("用户名已存在");
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            return ApiResponse.badRequest("邮箱已被注册");
        }
        if (request.getWalletAddress() != null && userRepository.existsByWalletAddress(request.getWalletAddress())) {
            return ApiResponse.badRequest("钱包地址已被绑定");
        }

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .walletAddress(request.getWalletAddress())
                .role("USER")
                .status(1)
                .build();

        userRepository.save(user);
        log.info("新用户注册: {}", user.getUsername());

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());

        return ApiResponse.success("注册成功", data);
    }

    /**
     * 用户登录
     */
    public ApiResponse<?> login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            return ApiResponse.unauthorized("用户名或密码错误");
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ApiResponse.unauthorized("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            return ApiResponse.forbidden("账户已被禁用");
        }

        String accessToken = jwtTokenProvider.generateToken(
                user.getId(), user.getUsername(), user.getRole(), user.getWalletAddress());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", accessToken);
        data.put("refreshToken", refreshToken);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        data.put("walletAddress", user.getWalletAddress());

        log.info("用户登录成功: {}", user.getUsername());
        return ApiResponse.success("登录成功", data);
    }

    /**
     * 获取用户信息
     */
    public ApiResponse<?> getUserInfo(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ApiResponse.notFound("用户不存在");
        }

        User user = userOpt.get();
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("walletAddress", user.getWalletAddress());
        data.put("role", user.getRole());
        data.put("avatarUrl", user.getAvatarUrl());
        data.put("createdAt", user.getCreatedAt());

        return ApiResponse.success(data);
    }

    /**
     * 绑定钱包地址
     */
    @Transactional
    public ApiResponse<?> bindWallet(Long userId, String walletAddress) {
        if (userRepository.existsByWalletAddress(walletAddress)) {
            return ApiResponse.badRequest("该钱包地址已被其他用户绑定");
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ApiResponse.notFound("用户不存在");
        }

        user.setWalletAddress(walletAddress);
        userRepository.save(user);

        log.info("用户 {} 绑定钱包: {}", user.getUsername(), walletAddress);
        return ApiResponse.success("钱包绑定成功");
    }
}
