package com.copyright.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * IPFS工具类 (基于HTTP REST API)
 * 负责文件上传、JSON元数据上传、内容检索
 */
@Slf4j
@Component
public class IPFSUtil {

    private final String apiUrl;
    private final String gateway;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public IPFSUtil(
            @Value("${ipfs.host}") String host,
            @Value("${ipfs.port}") int port,
            @Value("${ipfs.gateway}") String gateway) {
        this.apiUrl = "http://" + host + ":" + port + "/api/v0";
        this.gateway = gateway;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        log.info("IPFS客户端初始化: {}:{}, API: {}", host, port, apiUrl);
    }

    /**
     * 上传文件到IPFS
     * @param fileBytes 文件字节数组
     * @param fileName 文件名
     * @return IPFS CID
     */
    public String uploadFile(byte[] fileBytes, String fileName) {
        try {
            // 使用 IPFS HTTP API: POST /api/v0/add
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return fileName != null ? fileName : "file";
                }
            };
            body.add("file", resource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl + "/add?pin=true", requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String cid = root.get("Hash").asText();
                log.info("文件上传IPFS成功: {} -> {}", fileName, cid);
                return cid;
            } else {
                throw new RuntimeException("IPFS上传失败: HTTP " + response.getStatusCodeValue());
            }
        } catch (Exception e) {
            log.warn("IPFS上传失败，使用模拟CID: {}", e.getMessage());
            // 当IPFS不可用时生成模拟CID用于开发测试
            return generateMockCid(fileBytes);
        }
    }

    /**
     * 上传JSON字符串到IPFS
     * @param json JSON字符串
     * @return IPFS CID
     */
    public String uploadJson(String json) {
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        return uploadFile(jsonBytes, "metadata.json");
    }

    /**
     * 从IPFS获取文件内容
     * @param cid IPFS CID
     * @return 文件字节数组
     */
    public byte[] getFile(String cid) {
        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(
                    gateway + "/ipfs/" + cid, byte[].class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("IPFS获取文件失败: {}", e.getMessage());
        }
        return new byte[0];
    }

    /**
     * 从IPFS获取JSON内容
     * @param cid IPFS CID
     * @return JSON字符串
     */
    public String getJson(String cid) {
        byte[] data = getFile(cid);
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * 获取IPFS网关地址
     */
    public String getGatewayUrl(String cid) {
        return gateway + "/ipfs/" + cid;
    }

    /**
     * 生成模拟CID用于开发测试
     */
    private String generateMockCid(byte[] content) {
        String hash = SHA256Util.calculateHash(content);
        String b58 = Base64.getUrlEncoder().withoutPadding().encodeToString(
                hash.substring(0, 32).getBytes(StandardCharsets.UTF_8));
        return "Qm" + b58.substring(0, Math.min(44, b58.length()));
    }
}
