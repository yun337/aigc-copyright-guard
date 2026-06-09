package com.copyright.controller;

import com.copyright.model.dto.MintNFTRequest;
import com.copyright.model.vo.ApiResponse;
import com.copyright.service.NFTService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * NFT控制器
 */
@RestController
@RequestMapping("/nft")
@RequiredArgsConstructor
public class NFTController {

    private final NFTService nftService;

    /**
     * 铸造版权NFT
     */
    @PostMapping("/mint")
    public ApiResponse<?> mintNFT(
            HttpServletRequest request,
            @Valid @RequestBody MintNFTRequest mintRequest) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("未登录");
        }
        return nftService.mintNFT(userId, mintRequest);
    }

    /**
     * 获取NFT详情
     */
    @GetMapping("/{nftId}")
    public ApiResponse<?> getNFTDetail(@PathVariable Long nftId) {
        return nftService.getNFTDetail(nftId);
    }

    /**
     * 根据作品ID查询NFT
     */
    @GetMapping("/by-work/{workId}")
    public ApiResponse<?> getNFTByWorkId(@PathVariable Long workId) {
        return nftService.getNFTByWorkId(workId);
    }

    /**
     * 获取当前用户的NFT列表
     */
    @GetMapping("/my")
    public ApiResponse<?> getMyNFTs(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.unauthorized("未登录");
        }
        return nftService.getUserNFTs(userId);
    }
}
