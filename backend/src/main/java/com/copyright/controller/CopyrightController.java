package com.copyright.controller;

import com.copyright.model.dto.CopyrightRegisterRequest;
import com.copyright.model.vo.ApiResponse;
import com.copyright.service.CopyrightService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 版权存证控制器
 */
@RestController
@RequestMapping("/copyright")
@RequiredArgsConstructor
public class CopyrightController {

    private final CopyrightService copyrightService;

    /**
     * 注册版权存证
     */
    @PostMapping("/register")
    public ApiResponse<?> registerCopyright(
            HttpServletRequest request,
            @Valid @RequestBody CopyrightRegisterRequest registerRequest) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("未登录");
        }
        return copyrightService.registerCopyright(userId, registerRequest.getWorkId());
    }

    /**
     * 获取版权存证详情
     */
    @GetMapping("/{copyrightId}")
    public ApiResponse<?> getCopyright(@PathVariable Long copyrightId) {
        return copyrightService.getCopyright(copyrightId);
    }

    /**
     * 根据作品ID查询版权记录
     */
    @GetMapping("/by-work/{workId}")
    public ApiResponse<?> getCopyrightByWorkId(@PathVariable Long workId) {
        return copyrightService.getCopyrightByWorkId(workId);
    }

    /**
     * 获取当前用户的版权记录
     */
    @GetMapping("/my")
    public ApiResponse<?> getMyCopyrights(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("未登录");
        }
        return copyrightService.getUserCopyrights(userId);
    }

    /**
     * 链上验证版权
     */
    @PostMapping("/verify/{copyrightId}")
    public ApiResponse<?> verifyOnChain(@PathVariable Long copyrightId) {
        return copyrightService.verifyOnChain(copyrightId);
    }

    /**
     * 下载版权证书PDF
     */
    @GetMapping("/{copyrightId}/download")
    public void downloadPdf(@PathVariable Long copyrightId,
                            jakarta.servlet.http.HttpServletResponse response) {
        copyrightService.downloadCertificatePdf(copyrightId, response);
    }
}
