package com.copyright.service;

import com.copyright.model.entity.Copyright;
import com.copyright.model.entity.Work;
import com.copyright.model.vo.ApiResponse;
import com.copyright.repository.CopyrightRepository;
import com.copyright.repository.WorkRepository;
import com.copyright.utils.BlockchainUtil;
import com.copyright.utils.IPFSUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 版权存证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopyrightService {

    private final CopyrightRepository copyrightRepository;
    private final WorkRepository workRepository;
    private final BlockchainUtil blockchainUtil;
    private final IPFSUtil ipfsUtil;
    private final TransactionTemplate transactionTemplate;

    /**
     * 注册版权存证
     * <p>
     * 区块链操作与DB事务分离，避免区块链异常（checked BlockchainException）
     * 污染 Spring 事务导致 "Transaction silently rolled back" 错误。
     */
    public ApiResponse<?> registerCopyright(Long userId, Long workId) {
        Optional<Work> workOpt = workRepository.findById(workId);
        if (workOpt.isEmpty()) {
            return ApiResponse.notFound("作品不存在");
        }

        Work work = workOpt.get();
        if (!work.getUser().getId().equals(userId)) {
            return ApiResponse.forbidden("只能为自己的作品注册版权");
        }

        if ("REGISTERED".equals(work.getCopyrightStatus())) {
            return ApiResponse.badRequest("该作品已注册版权");
        }

        // 检查是否已有版权记录
        Optional<Copyright> existingCopyright = copyrightRepository.findByWorkId(workId);
        if (existingCopyright.isPresent()) {
            return ApiResponse.badRequest("该作品已有版权记录");
        }

        // 检查区块链是否已配置
        if (!blockchainUtil.isReady()) {
            return ApiResponse.error("区块链未配置，请设置 ETH_PRIVATE_KEY、COPYRIGHT_REGISTRY_ADDR 等环境变量后重启服务");
        }

        // === 阶段1: 区块链操作（事务外执行） ===
        String certificateId;
        String txHash;
        String certCid;
        Long blockNumber;
        try {
            // 检查区块链上是否已注册
            if (blockchainUtil.isFileRegistered(work.getFileHash())) {
                return ApiResponse.badRequest("该文件哈希已在区块链上注册");
            }

            // 生成版权证书编号
            certificateId = generateCertificateId();

            // 调用智能合约注册版权
            txHash = blockchainUtil.registerCopyright(
                    work.getFileHash(),
                    work.getIpfsCid(),
                    work.getMetadataCid(),
                    work.getTitle(),
                    work.getWorkType()
            );
            log.info("版权上链成功, 交易哈希: {}", txHash);

            // 生成版权证书并上传IPFS
            String certJson = buildCertificateJson(work, certificateId, txHash);
            certCid = ipfsUtil.uploadJson(certJson);
            log.info("版权证书上传IPFS, CID: {}", certCid);

            // 更新合约上的证书CID
            BigInteger copyrightIdOnChain = blockchainUtil.getCopyrightIdByHash(work.getFileHash());
            blockchainUtil.updateCertificateCid(copyrightIdOnChain.longValue(), certCid);

            // 获取区块号
            BigInteger bn = blockchainUtil.getTransactionBlockNumber(txHash);
            blockNumber = bn != null ? bn.longValue() : null;

        } catch (Exception e) {
            log.error("版权链上存证失败", e);
            return ApiResponse.error("版权存证失败: " + e.getMessage());
        }

        // === 阶段2: DB保存（使用 TransactionTemplate，确保独立事务） ===
        // 使用 TransactionTemplate 而非 @Transactional 注解，
        // 避免 Spring AOP 自调用代理失效导致事务不生效
        try {
            Map<String, Object> data = transactionTemplate.execute(status -> {
                Copyright copyright = Copyright.builder()
                        .work(work)
                        .user(work.getUser())
                        .certificateId(certificateId)
                        .txHash(txHash)
                        .blockNumber(blockNumber)
                        .contractAddress(blockchainUtil.getCopyrightRegistryAddress())
                        .certIpfsCid(certCid)
                        .registeredAt(LocalDateTime.now())
                        .build();

                copyrightRepository.save(copyright);

                // 更新作品版权状态
                work.setCopyrightStatus("REGISTERED");
                workRepository.save(work);

                Map<String, Object> result = new HashMap<>();
                result.put("copyrightId", copyright.getId());
                result.put("certificateId", certificateId);
                result.put("txHash", txHash);
                result.put("blockNumber", copyright.getBlockNumber());
                result.put("certIpfsCid", certCid);
                result.put("registeredAt", copyright.getRegisteredAt());

                return result;
            });

            log.info("版权存证成功: {} (证书编号: {})", work.getTitle(), certificateId);
            return ApiResponse.success("版权存证成功", data);

        } catch (Exception e) {
            log.error("版权记录保存失败", e);
            return ApiResponse.error("版权记录保存失败: " + e.getMessage());
        }
    }

    /**
     * 查询版权存证记录
     */
    public ApiResponse<?> getCopyright(Long copyrightId) {
        Optional<Copyright> copyrightOpt = copyrightRepository.findById(copyrightId);
        if (copyrightOpt.isEmpty()) {
            return ApiResponse.notFound("版权记录不存在");
        }

        Copyright copyright = copyrightOpt.get();
        Map<String, Object> data = buildCopyrightDetail(copyright);
        return ApiResponse.success(data);
    }

    /**
     * 根据作品ID查询版权
     */
    public ApiResponse<?> getCopyrightByWorkId(Long workId) {
        Optional<Copyright> copyrightOpt = copyrightRepository.findByWorkId(workId);
        if (copyrightOpt.isEmpty()) {
            return ApiResponse.notFound("该作品未注册版权");
        }

        Map<String, Object> data = buildCopyrightDetail(copyrightOpt.get());
        return ApiResponse.success(data);
    }

    /**
     * 查询用户的所有版权记录
     */
    public ApiResponse<?> getUserCopyrights(Long userId) {
        var copyrights = copyrightRepository.findByUserId(userId);
        return ApiResponse.success(copyrights);
    }

    /**
     * 链上验证版权
     */
    public ApiResponse<?> verifyOnChain(Long copyrightId) {
        Optional<Copyright> copyrightOpt = copyrightRepository.findById(copyrightId);
        if (copyrightOpt.isEmpty()) {
            return ApiResponse.notFound("版权记录不存在");
        }

        Copyright copyright = copyrightOpt.get();
        try {
            BigInteger onChainId = blockchainUtil.getCopyrightIdByHash(copyright.getWork().getFileHash());
            boolean isRegistered = blockchainUtil.isFileRegistered(copyright.getWork().getFileHash());

            Map<String, Object> data = new HashMap<>();
            data.put("copyrightId", copyright.getId());
            data.put("chainCopyrightId", onChainId);
            data.put("isRegisteredOnChain", isRegistered);
            data.put("txHash", copyright.getTxHash());
            data.put("blockNumber", copyright.getBlockNumber());
            data.put("verified", isRegistered);

            return ApiResponse.success("链上验证完成", data);
        } catch (Exception e) {
            log.error("链上验证失败", e);
            return ApiResponse.error("链上验证失败: " + e.getMessage());
        }
    }

    /**
     * 生成版权证书编号
     */
    private String generateCertificateId() {
        return "CG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() +
                "-" + System.currentTimeMillis() % 100000;
    }

    /**
     * 构建版权证书JSON
     */
    private String buildCertificateJson(Work work, String certificateId, String txHash) {
        return """
        {
            "certificateId": "%s",
            "workTitle": "%s",
            "workType": "%s",
            "creator": "%s",
            "fileHash": "%s",
            "ipfsCid": "%s",
            "txHash": "%s",
            "platform": "CopyrightGuard",
            "issuer": "ChainCreation Guardian Platform",
            "issuedAt": "%s",
            "version": "1.0"
        }
        """.formatted(
                certificateId,
                escapeJson(work.getTitle()),
                work.getWorkType(),
                work.getUser().getUsername(),
                work.getFileHash(),
                work.getIpfsCid(),
                txHash,
                LocalDateTime.now().toString()
        );
    }

    /**
     * 构建版权详情响应
     */
    private Map<String, Object> buildCopyrightDetail(Copyright copyright) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", copyright.getId());
        data.put("certificateId", copyright.getCertificateId());
        data.put("txHash", copyright.getTxHash());
        data.put("blockNumber", copyright.getBlockNumber());
        data.put("contractAddress", copyright.getContractAddress());
        data.put("certIpfsCid", copyright.getCertIpfsCid());
        data.put("registeredAt", copyright.getRegisteredAt());

        Work work = copyright.getWork();
        Map<String, Object> workInfo = new HashMap<>();
        workInfo.put("id", work.getId());
        workInfo.put("title", work.getTitle());
        workInfo.put("workType", work.getWorkType());
        workInfo.put("fileHash", work.getFileHash());
        workInfo.put("ipfsCid", work.getIpfsCid());
        workInfo.put("promptInfo", work.getPromptInfo());
        workInfo.put("creator", work.getUser().getUsername());
        data.put("work", workInfo);

        return data;
    }

    /**
     * 生成并下载版权证书PDF
     */
    public void downloadCertificatePdf(Long copyrightId, jakarta.servlet.http.HttpServletResponse response) {
        var copyrightOpt = copyrightRepository.findById(copyrightId);
        if (copyrightOpt.isEmpty()) {
            try { response.sendError(404, "版权记录不存在"); } catch (Exception ignored) {}
            return;
        }

        Copyright c = copyrightOpt.get();
        Work w = c.getWork();

        try {
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"Copyright_Certificate_" + c.getCertificateId() + ".pdf\"");

            com.lowagie.text.Document doc = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4);
            com.lowagie.text.pdf.PdfWriter writer = com.lowagie.text.pdf.PdfWriter.getInstance(doc, response.getOutputStream());
            doc.open();

            // 中文字体（使用 OpenPDF 内置的 CJK 字体）
            com.lowagie.text.pdf.BaseFont bf = com.lowagie.text.pdf.BaseFont.createFont(
                    "STSong-Light", "UniGB-UCS2-H", com.lowagie.text.pdf.BaseFont.NOT_EMBEDDED);

            com.lowagie.text.Font titleFont  = new com.lowagie.text.Font(bf, 22, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font subFont    = new com.lowagie.text.Font(bf, 10);
            com.lowagie.text.Font purpleFont = new com.lowagie.text.Font(bf, 11);
            com.lowagie.text.Font labelFont  = new com.lowagie.text.Font(bf, 9);
            com.lowagie.text.Font valueFont  = new com.lowagie.text.Font(bf, 10);
            com.lowagie.text.Font smallFont  = new com.lowagie.text.Font(bf, 8);

            // 顶部装饰线
            com.lowagie.text.pdf.PdfContentByte cb = writer.getDirectContent();
            cb.setRGBColorFill(108, 92, 231);
            cb.rectangle(30, 810, 535, 3);
            cb.fill();

            // 标题
            com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("版权存证证书", titleFont);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            title.setSpacingBefore(30);
            doc.add(title);

            com.lowagie.text.Paragraph enTitle = new com.lowagie.text.Paragraph(
                    "Copyright Certificate of AIGC Work", subFont);
            enTitle.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            doc.add(enTitle);

            com.lowagie.text.Paragraph certId = new com.lowagie.text.Paragraph(
                    "证书编号: " + c.getCertificateId(), purpleFont);
            certId.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            certId.setSpacingAfter(15);
            doc.add(certId);

            doc.add(new com.lowagie.text.Paragraph(" "));

            // 信息区
            String[][] info = {
                {"作品名称", w.getTitle()},
                {"作品类型", w.getWorkType()},
                {"创 作 者", w.getUser().getUsername()},
                {"存证时间", c.getRegisteredAt() != null ? c.getRegisteredAt().toString() : "-"},
                {"区 块 号", String.valueOf(c.getBlockNumber() != null ? c.getBlockNumber() : "-")},
                {"合约地址", c.getContractAddress() != null ? trunc(c.getContractAddress(), 40) : "-"},
                {"文件哈希", trunc(w.getFileHash(), 60)},
                {"IPFS CID", trunc(w.getIpfsCid(), 60)},
                {"交易哈希", trunc(c.getTxHash(), 60)},
            };
            for (String[] row : info) {
                com.lowagie.text.Paragraph p = new com.lowagie.text.Paragraph();
                p.add(new com.lowagie.text.Chunk(row[0] + "：", labelFont));
                p.add(new com.lowagie.text.Chunk(row[1] != null ? row[1] : "-", valueFont));
                p.setSpacingBefore(6);
                doc.add(p);
            }

            // Prompt
            if (w.getPromptInfo() != null && !w.getPromptInfo().isEmpty()) {
                doc.add(new com.lowagie.text.Paragraph(" "));
                doc.add(new com.lowagie.text.Paragraph("AI Prompt 信息：", labelFont));
                com.lowagie.text.Paragraph promptP = new com.lowagie.text.Paragraph(
                        w.getPromptInfo().length() > 500
                                ? w.getPromptInfo().substring(0, 500) + "..."
                                : w.getPromptInfo(),
                        smallFont);
                promptP.setSpacingBefore(4);
                doc.add(promptP);
            }

            // 底部
            doc.add(new com.lowagie.text.Paragraph(" "));
            cb.setRGBColorFill(108, 92, 231);
            cb.rectangle(30, 80, 535, 2);
            cb.fill();

            com.lowagie.text.Paragraph footer = new com.lowagie.text.Paragraph(
                    "链创守护 - AIGC作品版权保护平台 | Powered by Ethereum + IPFS + AI", smallFont);
            footer.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            footer.setSpacingBefore(8);
            doc.add(footer);

            doc.close();
            log.info("PDF证书已生成: {}", c.getCertificateId());

        } catch (Exception e) {
            log.error("PDF生成失败", e);
            try { response.sendError(500, "PDF生成失败: " + e.getMessage()); } catch (Exception ignored) {}
        }
    }

    private String trunc(String s, int max) {
        if (s == null) return "-";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
