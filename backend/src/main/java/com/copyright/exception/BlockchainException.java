package com.copyright.exception;

/**
 * 区块链交互异常（checked exception）
 * <p>
 * 使用 checked exception 而非 RuntimeException，
 * 确保在 @Transactional 方法中抛出时不会触发 Spring 自动回滚标记，
 * 避免 "Transaction silently rolled back" 问题。
 */
public class BlockchainException extends Exception {

    public BlockchainException(String message) {
        super(message);
    }

    public BlockchainException(String message, Throwable cause) {
        super(message, cause);
    }
}
