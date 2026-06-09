package com.copyright.controller;

import com.copyright.model.dto.DetectionRequest;
import com.copyright.model.vo.ApiResponse;
import com.copyright.service.InfringementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 侵权检测控制器
 */
@RestController
@RequestMapping("/detection")
@RequiredArgsConstructor
public class InfringementController {

    private final InfringementService infringementService;

    /**
     * 执行侵权检测
     */
    @PostMapping("/check")
    public ApiResponse<?> checkInfringement(
            HttpServletRequest request,
            @Valid @RequestBody DetectionRequest detectionRequest) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("未登录");
        }
        return infringementService.detectInfringement(
                userId,
                detectionRequest.getWorkId(),
                detectionRequest.getThreshold(),
                detectionRequest.getMaxResults()
        );
    }

    /**
     * 获取作品的侵权检测历史
     */
    @GetMapping("/history/{workId}")
    public ApiResponse<?> getDetectionHistory(@PathVariable Long workId) {
        return infringementService.getDetectionHistory(workId);
    }

    /**
     * 获取高相似度记录
     */
    @GetMapping("/high-similarity")
    public ApiResponse<?> getHighSimilarity(
            @RequestParam(required = false) Double threshold,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return infringementService.getHighSimilarityRecords(threshold, page, size);
    }
}
