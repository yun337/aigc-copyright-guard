package com.copyright.service;

import com.copyright.model.dto.MintNFTRequest;
import com.copyright.model.entity.Copyright;
import com.copyright.model.entity.NFT;
import com.copyright.model.entity.Work;
import com.copyright.model.vo.ApiResponse;
import com.copyright.repository.CopyrightRepository;
import com.copyright.repository.NFTRepository;
import com.copyright.repository.WorkRepository;
import com.copyright.utils.BlockchainUtil;
import com.copyright.utils.IPFSUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;

/**
 * NFT铸造服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NFTService {

    private final NFTRepository nftRepository;
    private final WorkRepository workRepository;
    private final CopyrightRepository copyrightRepository;
    private final BlockchainUtil blockchainUtil;
    private final IPFSUtil ipfsUtil;
    private final TransactionTemplate transactionTemplate;

    /**
     * 铸造版权NFT
     * <p>
     * 区块链操作与DB事务分离：
     * 1. 先执行区块链操作（IPFS上传、合约调用）——非事务性
     * 2. 再在事务中保存DB记录
     * <p>
     * 这样即使区块链操作抛出 BlockchainException（checked），
     * 也不会污染 Spring 事务，避免了 "Transaction silently rolled back" 错误。
     */
    public ApiResponse<?> mintNFT(Long userId, MintNFTRequest request) {
        Optional<Work> workOpt = workRepository.findById(request.getWorkId());
        if (workOpt.isEmpty()) {
            return ApiResponse.notFound("作品不存在");
        }

        Work work = workOpt.get();
        if (!work.getUser().getId().equals(userId)) {
            return ApiResponse.forbidden("只能为自己的作品铸造NFT");
        }

        if (!"REGISTERED".equals(work.getCopyrightStatus())) {
            return ApiResponse.badRequest("请先为作品注册版权");
        }

        // 检查是否已铸造
        Optional<NFT> existingNFT = nftRepository.findByWorkId(request.getWorkId());
        if (existingNFT.isPresent()) {
            return ApiResponse.badRequest("该作品已铸造NFT");
        }

        // 获取版权记录
        Optional<Copyright> copyrightOpt = copyrightRepository.findByWorkId(request.getWorkId());
        if (copyrightOpt.isEmpty()) {
            return ApiResponse.badRequest("未找到版权记录");
        }

        // 检查区块链是否已配置
        if (!blockchainUtil.isReady()) {
            return ApiResponse.error("区块链未配置，请设置 ETH_PRIVATE_KEY、COPYRIGHT_REGISTRY_ADDR 等环境变量后重启服务");
        }

        // === 阶段1: 区块链操作（事务外执行，失败不影响DB事务） ===
        String metadataCid;
        String txHash;
        BigInteger tokenId;
        String nftContractAddress;
        try {
            // 准备NFT元数据并上传IPFS
            String nftMetadataJson = buildNFTMetadataJson(work);
            metadataCid = ipfsUtil.uploadJson(nftMetadataJson);
            String tokenURI = "ipfs://" + metadataCid;

            // 设置版税率
            int royaltyRate = request.getRoyaltyRate() != null ? request.getRoyaltyRate() : 500;

            // 调用NFT合约铸造（未绑定钱包时使用默认测试账户）
            String walletAddr = work.getUser().getWalletAddress();
            if (walletAddr == null || walletAddr.isBlank() || walletAddr.length() != 42 || !walletAddr.startsWith("0x")) {
                log.warn("用户钱包地址无效，使用默认测试账户: {}", walletAddr);
                walletAddr = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266"; // Hardhat account #0
            }
            BigInteger copyrightIdOnChain = blockchainUtil.getCopyrightIdByHash(work.getFileHash());
            txHash = blockchainUtil.mintNFT(
                    walletAddr,
                    copyrightIdOnChain.longValue(),
                    work.getIpfsCid(),
                    tokenURI,
                    royaltyRate
            );

            // 查询铸造的Token ID
            tokenId = blockchainUtil.getTokenByCopyright(copyrightIdOnChain.longValue());
            nftContractAddress = blockchainUtil.getCopyrightNFTAddress();

        } catch (Exception e) {
            log.error("NFT链上铸造失败", e);
            return ApiResponse.error("NFT铸造失败: " + e.getMessage());
        }

        // === 阶段2: DB保存（使用 TransactionTemplate，确保独立事务） ===
        // 使用 TransactionTemplate 而非 @Transactional 注解，
        // 避免 Spring AOP 自调用代理失效导致事务不生效
        try {
            NFT nft = transactionTemplate.execute(status -> {
                NFT entity = NFT.builder()
                        .work(work)
                        .owner(work.getUser())
                        .tokenId(tokenId.toString())
                        .contractAddress(nftContractAddress)
                        .tokenUri("ipfs://" + metadataCid)
                        .metadataCid(metadataCid)
                        .txHash(txHash)
                        .mintedAt(LocalDateTime.now())
                        .build();
                return nftRepository.save(entity);
            });

            Map<String, Object> data = new HashMap<>();
            data.put("nftId", nft.getId());
            data.put("tokenId", nft.getTokenId());
            data.put("contractAddress", nft.getContractAddress());
            data.put("tokenURI", nft.getTokenUri());
            data.put("metadataCid", nft.getMetadataCid());
            data.put("txHash", nft.getTxHash());
            data.put("mintedAt", nft.getMintedAt());

            log.info("NFT铸造成功: TokenID={}, 作品={}", tokenId, work.getTitle());
            return ApiResponse.success("NFT铸造成功", data);

        } catch (Exception e) {
            log.error("NFT记录保存失败", e);
            return ApiResponse.error("NFT记录保存失败: " + e.getMessage());
        }
    }

    /**
     * 获取NFT详情
     */
    public ApiResponse<?> getNFTDetail(Long nftId) {
        Optional<NFT> nftOpt = nftRepository.findById(nftId);
        if (nftOpt.isEmpty()) {
            return ApiResponse.notFound("NFT不存在");
        }

        return ApiResponse.success(buildNFTData(nftOpt.get()));
    }

    /**
     * 根据作品ID查询NFT
     */
    public ApiResponse<?> getNFTByWorkId(Long workId) {
        Optional<NFT> nftOpt = nftRepository.findByWorkId(workId);
        if (nftOpt.isEmpty()) {
            return ApiResponse.notFound("该作品未铸造NFT");
        }

        return ApiResponse.success(buildNFTData(nftOpt.get()));
    }

    /**
     * 获取用户的NFT列表
     */
    public ApiResponse<?> getUserNFTs(Long userId) {
        List<NFT> nfts = nftRepository.findByOwnerId(userId);
        List<Map<String, Object>> nftList = new ArrayList<>();
        for (NFT nft : nfts) {
            nftList.add(buildNFTData(nft));
        }
        return ApiResponse.success(nftList);
    }

    /**
     * 构建NFT元数据JSON
     */
    private String buildNFTMetadataJson(Work work) {
        return """
        {
            "name": "#%s",
            "description": "%s",
            "image": "ipfs://%s",
            "attributes": [
                {"trait_type": "Work Type", "value": "%s"},
                {"trait_type": "Platform", "value": "CopyrightGuard"},
                {"trait_type": "Creator", "value": "%s"}
            ]
        }
        """.formatted(
                work.getTitle(),
                escapeJson(work.getDescription() != null ? work.getDescription() : ""),
                work.getIpfsCid(),
                work.getWorkType(),
                work.getUser().getUsername()
        );
    }

    /**
     * 构建NFT响应数据
     */
    private Map<String, Object> buildNFTData(NFT nft) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", nft.getId());
        data.put("tokenId", nft.getTokenId());
        data.put("contractAddress", nft.getContractAddress());
        data.put("tokenURI", nft.getTokenUri());
        data.put("metadataCid", nft.getMetadataCid());
        data.put("txHash", nft.getTxHash());
        data.put("mintedAt", nft.getMintedAt());

        Work work = nft.getWork();
        Map<String, Object> workInfo = new HashMap<>();
        workInfo.put("id", work.getId());
        workInfo.put("title", work.getTitle());
        workInfo.put("ipfsCid", work.getIpfsCid());
        workInfo.put("workType", work.getWorkType());
        data.put("work", workInfo);

        data.put("owner", nft.getOwner().getUsername());
        data.put("ownerWallet", nft.getOwner().getWalletAddress());

        return data;
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
