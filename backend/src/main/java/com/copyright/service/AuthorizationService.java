package com.copyright.service;

import com.copyright.model.dto.AuthorizationRequest;
import com.copyright.model.entity.Authorization;
import com.copyright.model.entity.Copyright;
import com.copyright.model.entity.User;
import com.copyright.model.vo.ApiResponse;
import com.copyright.repository.AuthorizationRepository;
import com.copyright.repository.CopyrightRepository;
import com.copyright.repository.UserRepository;
import com.copyright.utils.IPFSUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 授权交易服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final AuthorizationRepository authorizationRepository;
    private final CopyrightRepository copyrightRepository;
    private final UserRepository userRepository;
    private final IPFSUtil ipfsUtil;

    /**
     * 创建版权授权
     */
    @Transactional
    public ApiResponse<?> createAuthorization(Long licensorId, AuthorizationRequest request) {
        Optional<Copyright> copyrightOpt = copyrightRepository.findById(request.getCopyrightId());
        if (copyrightOpt.isEmpty()) {
            return ApiResponse.notFound("版权记录不存在");
        }

        Copyright copyright = copyrightOpt.get();
        if (!copyright.getUser().getId().equals(licensorId)) {
            return ApiResponse.forbidden("只有版权所有者可以授权");
        }

        Optional<User> licenseeOpt = userRepository.findById(request.getLicenseeId());
        if (licenseeOpt.isEmpty()) {
            return ApiResponse.notFound("被授权用户不存在");
        }

        if (licensorId.equals(request.getLicenseeId())) {
            return ApiResponse.badRequest("不能授权给自己");
        }

        try {
            // 构建授权许可证书
            String licenseJson = buildLicenseJson(copyright, licenseeOpt.get(), request);
            String licenseCid = ipfsUtil.uploadJson(licenseJson);

            // 保存授权记录
            Authorization auth = Authorization.builder()
                    .copyright(copyright)
                    .licensor(copyright.getUser())
                    .licensee(licenseeOpt.get())
                    .licenseType(request.getLicenseType() != null ? request.getLicenseType() : "EXCLUSIVE")
                    .licenseFee(request.getLicenseFee() != null ? request.getLicenseFee() : BigDecimal.ZERO)
                    .startDate(LocalDate.now())
                    .endDate(request.getDurationDays() != null ?
                            LocalDate.now().plusDays(request.getDurationDays()) : null)
                    .status("ACTIVE")
                    .build();

            authorizationRepository.save(auth);

            Map<String, Object> data = new HashMap<>();
            data.put("authorizationId", auth.getId());
            data.put("licenseType", auth.getLicenseType());
            data.put("licensee", licenseeOpt.get().getUsername());
            data.put("startDate", auth.getStartDate());
            data.put("endDate", auth.getEndDate());
            data.put("licenseCid", licenseCid);
            data.put("status", auth.getStatus());

            log.info("版权授权创建成功: {} 授权给 {}", copyright.getWork().getTitle(), licenseeOpt.get().getUsername());
            return ApiResponse.success("授权创建成功", data);

        } catch (Exception e) {
            log.error("创建授权失败", e);
            return ApiResponse.error("创建授权失败: " + e.getMessage());
        }
    }

    /**
     * 获取授权详情
     */
    public ApiResponse<?> getAuthorization(Long authId) {
        Optional<Authorization> authOpt = authorizationRepository.findById(authId);
        if (authOpt.isEmpty()) {
            return ApiResponse.notFound("授权记录不存在");
        }

        return ApiResponse.success(buildAuthorizationData(authOpt.get()));
    }

    /**
     * 获取用户作为授权人的授权列表
     */
    public ApiResponse<?> getLicensorAuthorizations(Long licensorId) {
        List<Authorization> authorizations = authorizationRepository.findByLicensorId(licensorId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Authorization auth : authorizations) {
            list.add(buildAuthorizationData(auth));
        }
        return ApiResponse.success(list);
    }

    /**
     * 获取用户作为被授权人的授权列表
     */
    public ApiResponse<?> getLicenseeAuthorizations(Long licenseeId) {
        List<Authorization> authorizations = authorizationRepository.findByLicenseeId(licenseeId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Authorization auth : authorizations) {
            list.add(buildAuthorizationData(auth));
        }
        return ApiResponse.success(list);
    }

    /**
     * 撤销授权
     */
    @Transactional
    public ApiResponse<?> revokeAuthorization(Long authId, Long userId) {
        Optional<Authorization> authOpt = authorizationRepository.findById(authId);
        if (authOpt.isEmpty()) {
            return ApiResponse.notFound("授权记录不存在");
        }

        Authorization auth = authOpt.get();
        if (!auth.getLicensor().getId().equals(userId)) {
            return ApiResponse.forbidden("只有授权人可以撤销");
        }

        if (!"ACTIVE".equals(auth.getStatus())) {
            return ApiResponse.badRequest("该授权已不可撤销");
        }

        auth.setStatus("REVOKED");
        authorizationRepository.save(auth);

        log.info("授权 {} 已被撤销", authId);
        return ApiResponse.success("授权已撤销");
    }

    /**
     * 定时检查过期授权 - 每天凌晨执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void checkExpiredAuthorizations() {
        List<Authorization> expired = authorizationRepository.findExpiredActive();
        for (Authorization auth : expired) {
            auth.setStatus("EXPIRED");
            authorizationRepository.save(auth);
            log.info("授权 {} 已过期", auth.getId());
        }
    }

    /**
     * 构建授权数据响应
     */
    private Map<String, Object> buildAuthorizationData(Authorization auth) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", auth.getId());
        data.put("licenseType", auth.getLicenseType());
        data.put("licenseFee", auth.getLicenseFee());
        data.put("startDate", auth.getStartDate());
        data.put("endDate", auth.getEndDate());
        data.put("txHash", auth.getTxHash());
        data.put("status", auth.getStatus());
        data.put("createdAt", auth.getCreatedAt());

        Map<String, Object> licensorInfo = new HashMap<>();
        licensorInfo.put("id", auth.getLicensor().getId());
        licensorInfo.put("username", auth.getLicensor().getUsername());
        licensorInfo.put("walletAddress", auth.getLicensor().getWalletAddress());
        data.put("licensor", licensorInfo);

        Map<String, Object> licenseeInfo = new HashMap<>();
        licenseeInfo.put("id", auth.getLicensee().getId());
        licenseeInfo.put("username", auth.getLicensee().getUsername());
        data.put("licensee", licenseeInfo);

        Copyright copyright = auth.getCopyright();
        Map<String, Object> workInfo = new HashMap<>();
        workInfo.put("title", copyright.getWork().getTitle());
        workInfo.put("ipfsCid", copyright.getWork().getIpfsCid());
        workInfo.put("workType", copyright.getWork().getWorkType());
        data.put("work", workInfo);

        return data;
    }

    /**
     * 构建授权许可证书JSON
     */
    private String buildLicenseJson(Copyright copyright, User licensee, AuthorizationRequest request) {
        return """
        {
            "licenseType": "%s",
            "licensor": "%s",
            "licensee": "%s",
            "workTitle": "%s",
            "workIpfsCid": "%s",
            "originalCopyright": "%s",
            "startDate": "%s",
            "endDate": "%s",
            "platform": "CopyrightGuard",
            "version": "1.0"
        }
        """.formatted(
                request.getLicenseType() != null ? request.getLicenseType() : "EXCLUSIVE",
                copyright.getUser().getUsername(),
                licensee.getUsername(),
                escapeJson(copyright.getWork().getTitle()),
                copyright.getWork().getIpfsCid(),
                copyright.getCertificateId(),
                LocalDate.now().toString(),
                request.getDurationDays() != null ?
                        LocalDate.now().plusDays(request.getDurationDays()).toString() : "permanent"
        );
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
