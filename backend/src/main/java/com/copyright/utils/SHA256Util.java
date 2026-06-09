package com.copyright.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA256哈希计算工具类
 */
public class SHA256Util {

    private static final String ALGORITHM = "SHA-256";

    /**
     * 计算字节数组的SHA256哈希
     */
    public static String calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(data);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA256 algorithm not available", e);
        }
    }

    /**
     * 计算字符串的SHA256哈希
     */
    public static String calculateHash(String data) {
        return calculateHash(data.getBytes());
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
