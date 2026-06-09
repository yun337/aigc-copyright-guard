package com.copyright.service;

import com.copyright.model.dto.WorkUploadRequest;
import com.copyright.model.entity.User;
import com.copyright.model.entity.Work;
import com.copyright.model.vo.ApiResponse;
import com.copyright.repository.UserRepository;
import com.copyright.repository.WorkRepository;
import com.copyright.utils.IPFSUtil;
import com.copyright.utils.SHA256Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 作品管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkService {

    private final WorkRepository workRepository;
    private final UserRepository userRepository;
    private final IPFSUtil ipfsUtil;
    private final TransactionTemplate transactionTemplate;

    @Value("${ai.detection.url}")
    private String aiServiceUrl;

    /**
     * 上传作品
     * <p>
     * IPFS操作与DB事务分离，避免IPFS异常污染Spring事务。
     * 注：IPFSUtil内部已有mock fallback，此处仍按分离模式保证一致性。
     */
    public ApiResponse<?> uploadWork(Long userId, WorkUploadRequest request, MultipartFile file) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ApiResponse.notFound("用户不存在");
        }

        if (file == null || file.isEmpty()) {
            return ApiResponse.badRequest("请上传作品文件");
        }

        // === 阶段1: 文件处理与IPFS上传（事务外） ===
        String fileHash;
        String ipfsCid;
        String metadataCid;
        long fileSize;
        try {
            byte[] fileBytes = file.getBytes();
            fileSize = fileBytes.length;

            // 计算SHA256哈希
            fileHash = SHA256Util.calculateHash(fileBytes);

            // 检查是否已存在相同哈希的作品
            Optional<Work> existingWork = workRepository.findByFileHash(fileHash);
            if (existingWork.isPresent()) {
                return ApiResponse.badRequest("该作品已上传，请勿重复上传");
            }

            // 上传到IPFS（有mock fallback，不会抛异常）
            ipfsCid = ipfsUtil.uploadFile(fileBytes, file.getOriginalFilename());
            log.info("文件上传到IPFS, CID: {}", ipfsCid);

            // 构建元数据并上传
            String metadataJson = buildMetadataJson(request, fileHash, ipfsCid);
            metadataCid = ipfsUtil.uploadJson(metadataJson);
            log.info("元数据上传到IPFS, CID: {}", metadataCid);

        } catch (IOException e) {
            log.error("文件读取失败", e);
            return ApiResponse.error("文件读取失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("文件处理失败", e);
            return ApiResponse.error("文件处理失败: " + e.getMessage());
        }

        // === 阶段2: DB保存（独立事务） ===
        try {
            Work work = transactionTemplate.execute(status -> {
                Work entity = Work.builder()
                        .user(userOpt.get())
                        .title(request.getTitle())
                        .description(request.getDescription())
                        .promptInfo(request.getPromptInfo())
                        .workType(request.getWorkType() != null ? request.getWorkType() : "IMAGE")
                        .fileHash(fileHash)
                        .ipfsCid(ipfsCid)
                        .metadataCid(metadataCid)
                        .fileSize(fileSize)
                        .copyrightStatus("UNREGISTERED")
                        .build();
                return workRepository.save(entity);
            });

            Map<String, Object> data = new HashMap<>();
            data.put("workId", work.getId());
            data.put("title", work.getTitle());
            data.put("fileHash", work.getFileHash());
            data.put("ipfsCid", work.getIpfsCid());
            data.put("metadataCid", work.getMetadataCid());
            data.put("fileSize", work.getFileSize());
            data.put("createdAt", work.getCreatedAt());

            log.info("作品上传成功: {} (ID: {})", request.getTitle(), work.getId());

            // 异步提取AI特征向量（失败不影响上传）
            try {
                extractAndSaveFeatures(work);
            } catch (Exception ex) {
                log.warn("特征提取失败（不影响上传）: {}", ex.getMessage());
            }

            return ApiResponse.success("作品上传成功", data);

        } catch (Exception e) {
            log.error("作品记录保存失败", e);
            return ApiResponse.error("作品记录保存失败: " + e.getMessage());
        }
    }

    /**
     * 获取作品列表
     */
    public ApiResponse<?> getWorks(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Work> workPage = workRepository.findAll(pageRequest);

        Map<String, Object> data = new HashMap<>();
        data.put("content", workPage.getContent());
        data.put("totalElements", workPage.getTotalElements());
        data.put("totalPages", workPage.getTotalPages());
        data.put("currentPage", page);

        return ApiResponse.success(data);
    }

    /**
     * 获取用户作品列表
     */
    public ApiResponse<?> getUserWorks(Long userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Work> workPage = workRepository.findByUserId(userId, pageRequest);

        Map<String, Object> data = new HashMap<>();
        data.put("content", workPage.getContent());
        data.put("totalElements", workPage.getTotalElements());
        data.put("totalPages", workPage.getTotalPages());

        return ApiResponse.success(data);
    }

    /**
     * 获取作品详情
     */
    public ApiResponse<?> getWorkDetail(Long workId) {
        Optional<Work> workOpt = workRepository.findById(workId);
        if (workOpt.isEmpty()) {
            return ApiResponse.notFound("作品不存在");
        }

        Work work = workOpt.get();

        Map<String, Object> data = new HashMap<>();
        data.put("id", work.getId());
        data.put("title", work.getTitle());
        data.put("description", work.getDescription());
        data.put("promptInfo", work.getPromptInfo());
        data.put("workType", work.getWorkType());
        data.put("fileHash", work.getFileHash());
        data.put("ipfsCid", work.getIpfsCid());
        data.put("metadataCid", work.getMetadataCid());
        data.put("fileSize", work.getFileSize());
        data.put("copyrightStatus", work.getCopyrightStatus());
        data.put("userId", work.getUser().getId());
        data.put("username", work.getUser().getUsername());
        data.put("createdAt", work.getCreatedAt());

        return ApiResponse.success(data);
    }

    /**
     * 构建作品元数据JSON
     */
    private String buildMetadataJson(WorkUploadRequest request, String fileHash, String ipfsCid) {
        return """
        {
            "title": "%s",
            "description": "%s",
            "promptInfo": "%s",
            "workType": "%s",
            "fileHash": "%s",
            "ipfsCid": "%s",
            "platform": "CopyrightGuard",
            "version": "1.0"
        }
        """.formatted(
                escapeJson(request.getTitle()),
                escapeJson(request.getDescription() != null ? request.getDescription() : ""),
                escapeJson(request.getPromptInfo() != null ? request.getPromptInfo() : ""),
                escapeJson(request.getWorkType() != null ? request.getWorkType() : "IMAGE"),
                fileHash,
                ipfsCid
        );
    }

    /**
     * 更新作品信息
     */
    @Transactional
    public ApiResponse<?> updateWork(Long userId, Long workId, WorkUploadRequest request) {
        Optional<Work> workOpt = workRepository.findById(workId);
        if (workOpt.isEmpty()) {
            return ApiResponse.notFound("作品不存在");
        }

        Work work = workOpt.get();
        if (!work.getUser().getId().equals(userId)) {
            return ApiResponse.forbidden("只能修改自己的作品");
        }

        if (request.getTitle() != null) work.setTitle(request.getTitle());
        if (request.getDescription() != null) work.setDescription(request.getDescription());
        if (request.getPromptInfo() != null) work.setPromptInfo(request.getPromptInfo());
        if (request.getWorkType() != null) work.setWorkType(request.getWorkType());

        workRepository.save(work);
        log.info("作品更新成功: {} (ID: {})", work.getTitle(), workId);

        return ApiResponse.success("作品更新成功");
    }

    /**
     * 删除作品
     */
    @Transactional
    public ApiResponse<?> deleteWork(Long userId, Long workId) {
        Optional<Work> workOpt = workRepository.findById(workId);
        if (workOpt.isEmpty()) {
            return ApiResponse.notFound("作品不存在");
        }

        Work work = workOpt.get();
        if (!work.getUser().getId().equals(userId)) {
            return ApiResponse.forbidden("只能删除自己的作品");
        }

        // 如果已注册版权，不允许直接删除
        if ("REGISTERED".equals(work.getCopyrightStatus())) {
            return ApiResponse.badRequest("已注册版权的作品不能删除，请先撤销版权存证");
        }

        workRepository.delete(work);
        log.info("作品删除成功: {} (ID: {})", work.getTitle(), workId);

        return ApiResponse.success("作品已删除");
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /**
     * 上传时自动提取AI特征向量
     */
    private void extractAndSaveFeatures(Work work) {
        try {
            String url = aiServiceUrl + "/api/extract-features";
            Map<String, Object> body = new HashMap<>();
            body.put("ipfsCid", work.getIpfsCid());
            body.put("ipfsUrl", "http://ipfs:8080/ipfs/" + work.getIpfsCid());
            body.put("workId", work.getId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            RestTemplate rt = new RestTemplate();
            ResponseEntity<Map> response = rt.postForEntity(url, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object features = response.getBody().get("features");
                if (features instanceof List) {
                    work.setFeatureVector(features.toString());
                    workRepository.save(work);
                    log.info("特征向量已提取: workId={}", work.getId());
                }
            }
        } catch (Exception e) {
            log.warn("AI特征提取失败(不影响上传): workId={}, error={}", work.getId(), e.getMessage());
        }
    }
}
