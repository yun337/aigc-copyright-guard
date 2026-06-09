package com.copyright.service;

import com.copyright.model.entity.Infringement;
import com.copyright.model.entity.Work;
import com.copyright.model.vo.ApiResponse;
import com.copyright.repository.InfringementRepository;
import com.copyright.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 侵权检测服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InfringementService {

    private final InfringementRepository infringementRepository;
    private final WorkRepository workRepository;
    private final RestTemplate restTemplate;

    @Value("${ai.detection.url}")
    private String aiServiceUrl;

    @Value("${ai.detection.similarity-threshold}")
    private double similarityThreshold;

    @Value("${ai.detection.max-results}")
    private int maxResults;

    /**
     * 执行侵权检测
     */
    @Transactional
    public ApiResponse<?> detectInfringement(Long userId, Long workId, Double threshold, Integer maxResults) {
        Optional<Work> workOpt = workRepository.findById(workId);
        if (workOpt.isEmpty()) {
            return ApiResponse.notFound("作品不存在");
        }

        Work work = workOpt.get();
        if (!work.getUser().getId().equals(userId)) {
            return ApiResponse.forbidden("只能检测自己的作品");
        }

        double thresholdValue = threshold != null ? threshold : similarityThreshold;
        int maxRes = maxResults != null ? maxResults : this.maxResults;

        try {
            // 1. 提取目标作品的特征向量
            String featureVector = extractFeatureVector(work);
            if (featureVector == null) {
                return ApiResponse.error("特征提取失败，请确保AI服务正常运行");
            }

            // 更新作品的特征向量
            work.setFeatureVector(featureVector);
            workRepository.save(work);

            // 2. 获取数据库中所有有特征向量的作品
            List<Work> worksWithFeatures = workRepository.findWorksWithFeatureVectors();
            List<Map<String, Object>> similarWorks = new ArrayList<>();

            // 3. 与每个作品比对相似度
            for (Work targetWork : worksWithFeatures) {
                if (targetWork.getId().equals(workId)) continue; // 跳过自己
                if (targetWork.getFeatureVector() == null) continue;

                // 调用AI服务计算相似度
                double similarity = calculateSimilarity(featureVector, targetWork.getFeatureVector());

                if (similarity >= thresholdValue) {
                    Map<String, Object> similarInfo = new HashMap<>();
                    similarInfo.put("workId", targetWork.getId());
                    similarInfo.put("title", targetWork.getTitle());
                    similarInfo.put("ipfsCid", targetWork.getIpfsCid());
                    similarInfo.put("similarity", Math.round(similarity * 10000.0) / 100.0);
                    similarInfo.put("creator", targetWork.getUser().getUsername());
                    similarWorks.add(similarInfo);

                    // 保存侵权记录
                    Infringement infringement = Infringement.builder()
                            .work(work)
                            .similarWorkId(targetWork.getId())
                            .similarWorkCid(targetWork.getIpfsCid())
                            .similarityScore(similarity)
                            .detectionType("SEMANTIC")
                            .status("PENDING")
                            .detectedAt(LocalDateTime.now())
                            .build();
                    infringementRepository.save(infringement);
                }
            }

            // 按相似度排序
            similarWorks.sort((a, b) -> Double.compare(
                    (Double) b.get("similarity"), (Double) a.get("similarity")));

            // 限制结果数量
            if (similarWorks.size() > maxRes) {
                similarWorks = similarWorks.subList(0, maxRes);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("workId", workId);
            result.put("workTitle", work.getTitle());
            result.put("totalCompared", worksWithFeatures.size() - 1);
            result.put("similarCount", similarWorks.size());
            result.put("threshold", thresholdValue);
            result.put("similarWorks", similarWorks);

            log.info("侵权检测完成: 作品#{} 比对{}件作品, 发现{}件相似",
                    workId, worksWithFeatures.size() - 1, similarWorks.size());

            return ApiResponse.success("检测完成", result);

        } catch (Exception e) {
            log.error("侵权检测失败", e);
            return ApiResponse.error("侵权检测失败: " + e.getMessage());
        }
    }

    /**
     * 获取作品的侵权检测历史
     */
    public ApiResponse<?> getDetectionHistory(Long workId) {
        List<Infringement> records = infringementRepository.findByWorkIdOrderByScore(workId);
        return ApiResponse.success(records);
    }

    /**
     * 获取高相似度记录
     */
    public ApiResponse<?> getHighSimilarityRecords(Double threshold, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, size);
        var records = infringementRepository.findHighSimilarity(threshold != null ? threshold : similarityThreshold, pageable);
        return ApiResponse.success(records);
    }

    /**
     * 调用AI服务提取特征向量
     */
    private String extractFeatureVector(Work work) {
        try {
            String url = aiServiceUrl + "/api/extract-features";
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("ipfsCid", work.getIpfsCid());
            requestBody.put("workId", work.getId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object features = response.getBody().get("features");
                if (features instanceof List) {
                    return features.toString();
                }
            }
        } catch (Exception e) {
            log.error("调用AI特征提取服务失败", e);
        }
        return null;
    }

    /**
     * 调用AI服务计算相似度
     */
    private double calculateSimilarity(String vector1, String vector2) {
        try {
            String url = aiServiceUrl + "/api/calculate-similarity";
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("vector1", vector1);
            requestBody.put("vector2", vector2);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object similarity = response.getBody().get("similarity");
                if (similarity instanceof Number) {
                    return ((Number) similarity).doubleValue();
                }
            }
        } catch (Exception e) {
            log.error("调用AI相似度计算服务失败", e);
        }
        return 0.0;
    }
}
