package com.copyright.controller;

import com.copyright.model.dto.WorkUploadRequest;
import com.copyright.model.vo.ApiResponse;
import com.copyright.service.WorkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 作品管理控制器
 */
@RestController
@RequestMapping("/works")
@RequiredArgsConstructor
public class WorkController {

    private final WorkService workService;

    /**
     * 上传AIGC作品
     */
    @PostMapping("/upload")
    public ApiResponse<?> uploadWork(
            HttpServletRequest request,
            @Valid @RequestPart("metadata") WorkUploadRequest workRequest,
            @RequestPart("file") MultipartFile file) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("未登录");
        }
        return workService.uploadWork(userId, workRequest, file);
    }

    /**
     * 查询作品列表（公开）
     */
    @GetMapping("/list")
    public ApiResponse<?> listWorks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return workService.getWorks(page, size);
    }

    /**
     * 获取当前用户的作品列表
     */
    @GetMapping("/my")
    public ApiResponse<?> getMyWorks(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("未登录");
        }
        return workService.getUserWorks(userId, page, size);
    }

    /**
     * 获取作品详情
     */
    @GetMapping("/{workId}")
    public ApiResponse<?> getWorkDetail(@PathVariable Long workId) {
        return workService.getWorkDetail(workId);
    }

    /**
     * 更新作品信息
     */
    @PutMapping("/{workId}")
    public ApiResponse<?> updateWork(
            HttpServletRequest request,
            @PathVariable Long workId,
            @Valid @RequestBody WorkUploadRequest workRequest) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("未登录");
        }
        return workService.updateWork(userId, workId, workRequest);
    }

    /**
     * 删除作品
     */
    @DeleteMapping("/{workId}")
    public ApiResponse<?> deleteWork(
            HttpServletRequest request,
            @PathVariable Long workId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("未登录");
        }
        return workService.deleteWork(userId, workId);
    }
}
