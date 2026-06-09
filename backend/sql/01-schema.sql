-- ============================================================
-- 链创守护 - AIGC作品版权保护平台
-- 数据库初始化脚本
-- MySQL 8.0+
-- ============================================================

CREATE DATABASE IF NOT EXISTS copyright_guard
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE copyright_guard;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE `user` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`      VARCHAR(64)   NOT NULL                COMMENT '用户名',
  `email`         VARCHAR(128)  DEFAULT NULL            COMMENT '邮箱',
  `password_hash` VARCHAR(256)  NOT NULL                COMMENT '密码哈希(BCrypt)',
  `wallet_address`VARCHAR(128)  DEFAULT NULL            COMMENT '以太坊钱包地址',
  `avatar_url`    VARCHAR(512)  DEFAULT NULL            COMMENT '头像URL',
  `role`          VARCHAR(32)   NOT NULL DEFAULT 'USER' COMMENT '角色: USER/ADMIN',
  `status`        TINYINT       NOT NULL DEFAULT 1      COMMENT '状态: 1正常 0禁用',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_wallet` (`wallet_address`),
  INDEX `idx_role` (`role`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';


-- ============================================================
-- 2. 作品表
-- ============================================================
CREATE TABLE `work` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '作品ID',
  `user_id`       BIGINT        NOT NULL                COMMENT '创作者ID',
  `title`         VARCHAR(256)  NOT NULL                COMMENT '作品标题',
  `description`   TEXT          DEFAULT NULL            COMMENT '作品描述',
  `prompt_info`   TEXT          DEFAULT NULL            COMMENT 'AI生成Prompt信息',
  `work_type`     VARCHAR(32)   NOT NULL DEFAULT 'IMAGE' COMMENT '作品类型: IMAGE/VIDEO/MUSIC/TEXT',
  `file_hash`     VARCHAR(128)  NOT NULL                COMMENT '文件SHA256哈希',
  `ipfs_cid`      VARCHAR(128)  DEFAULT NULL            COMMENT 'IPFS CID',
  `metadata_cid`  VARCHAR(128)  DEFAULT NULL            COMMENT '元数据IPFS CID',
  `feature_vector`JSON          DEFAULT NULL            COMMENT 'CLIP特征向量(JSON数组)',
  `file_size`     BIGINT        DEFAULT 0               COMMENT '文件大小(字节)',
  `copyright_status` VARCHAR(32) DEFAULT 'UNREGISTERED' COMMENT '版权状态: UNREGISTERED/REGISTERED/REJECTED',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_file_hash` (`file_hash`),
  INDEX `idx_ipfs_cid` (`ipfs_cid`),
  INDEX `idx_copyright_status` (`copyright_status`),
  CONSTRAINT `fk_work_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品表';


-- ============================================================
-- 3. 版权存证表
-- ============================================================
CREATE TABLE `copyright` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '版权ID',
  `work_id`         BIGINT       NOT NULL                COMMENT '作品ID',
  `user_id`         BIGINT       NOT NULL                COMMENT '版权所有者ID',
  `certificate_id`  VARCHAR(128) NOT NULL                COMMENT '版权证书编号',
  `tx_hash`         VARCHAR(128) NOT NULL                COMMENT '链上交易哈希',
  `block_number`    BIGINT       DEFAULT NULL            COMMENT '区块号',
  `contract_address`VARCHAR(128) DEFAULT NULL            COMMENT '合约地址',
  `cert_ipfs_cid`   VARCHAR(128) DEFAULT NULL            COMMENT '版权证书IPFS CID',
  `registered_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '存证时间',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_certificate_id` (`certificate_id`),
  UNIQUE KEY `uk_work_id` (`work_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_tx_hash` (`tx_hash`),
  CONSTRAINT `fk_copyright_work` FOREIGN KEY (`work_id`) REFERENCES `work` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_copyright_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='版权存证表';


-- ============================================================
-- 4. NFT表
-- ============================================================
CREATE TABLE `nft` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'NFT ID',
  `work_id`          BIGINT       NOT NULL                COMMENT '关联作品ID',
  `owner_id`         BIGINT       NOT NULL                COMMENT 'NFT所有者ID',
  `token_id`         VARCHAR(128) NOT NULL                COMMENT 'NFT TokenID',
  `contract_address` VARCHAR(128) NOT NULL                COMMENT 'NFT合约地址',
  `token_uri`        VARCHAR(512) DEFAULT NULL            COMMENT 'Token URI(元数据链接)',
  `metadata_cid`     VARCHAR(128) DEFAULT NULL            COMMENT '元数据IPFS CID',
  `tx_hash`          VARCHAR(128) DEFAULT NULL            COMMENT '铸造交易哈希',
  `minted_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '铸造时间',
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`contract_address`, `token_id`),
  UNIQUE KEY `uk_work_id` (`work_id`),
  INDEX `idx_owner_id` (`owner_id`),
  CONSTRAINT `fk_nft_work` FOREIGN KEY (`work_id`) REFERENCES `work` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_nft_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='NFT表';


-- ============================================================
-- 5. 侵权检测记录表
-- ============================================================
CREATE TABLE `infringement` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '检测记录ID',
  `work_id`           BIGINT       NOT NULL                COMMENT '检测作品ID',
  `similar_work_id`   BIGINT       DEFAULT NULL            COMMENT '相似作品ID',
  `similar_work_cid`  VARCHAR(128) DEFAULT NULL            COMMENT '相似作品IPFS CID',
  `similarity_score`  DOUBLE       NOT NULL DEFAULT 0      COMMENT '相似度分数(0-1)',
  `detection_type`    VARCHAR(32)  NOT NULL DEFAULT 'SEMANTIC' COMMENT '检测类型: SEMANTIC/HASH/PERCEPTUAL',
  `status`            VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/CONFIRMED/DISMISSED',
  `report_url`        VARCHAR(512) DEFAULT NULL            COMMENT '检测报告URL',
  `detected_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_work_id` (`work_id`),
  INDEX `idx_similar_work_id` (`similar_work_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_detected_at` (`detected_at`),
  CONSTRAINT `fk_infringement_work` FOREIGN KEY (`work_id`) REFERENCES `work` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='侵权检测记录表';


-- ============================================================
-- 6. 授权交易表
-- ============================================================
CREATE TABLE `authorization` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '授权ID',
  `copyright_id`    BIGINT       NOT NULL                COMMENT '版权ID',
  `licensor_id`     BIGINT       NOT NULL                COMMENT '授权人ID(版权方)',
  `licensee_id`     BIGINT       NOT NULL                COMMENT '被授权人ID',
  `license_type`    VARCHAR(32)  NOT NULL DEFAULT 'EXCLUSIVE' COMMENT '授权类型: EXCLUSIVE/NON_EXCLUSIVE/TEMPORARY',
  `license_fee`     DECIMAL(18,6)DEFAULT 0               COMMENT '授权费用(ETH)',
  `start_date`      DATE         NOT NULL                COMMENT '授权开始日期',
  `end_date`        DATE         DEFAULT NULL            COMMENT '授权结束日期',
  `tx_hash`         VARCHAR(128) DEFAULT NULL            COMMENT '链上交易哈希',
  `status`          VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/EXPIRED/REVOKED',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_copyright_id` (`copyright_id`),
  INDEX `idx_licensor_id` (`licensor_id`),
  INDEX `idx_licensee_id` (`licensee_id`),
  INDEX `idx_status` (`status`),
  CONSTRAINT `fk_auth_copyright` FOREIGN KEY (`copyright_id`) REFERENCES `copyright` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_auth_licensor` FOREIGN KEY (`licensor_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_auth_licensee` FOREIGN KEY (`licensee_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='授权交易表';


-- ============================================================
-- 7. 系统操作日志表
-- ============================================================
CREATE TABLE `operation_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id`       BIGINT       DEFAULT NULL            COMMENT '操作用户ID',
  `operation`     VARCHAR(64)  NOT NULL                COMMENT '操作类型',
  `target_type`   VARCHAR(32)  DEFAULT NULL            COMMENT '目标类型: WORK/COPYRIGHT/NFT/AUTH',
  `target_id`     BIGINT       DEFAULT NULL            COMMENT '目标ID',
  `detail`        TEXT         DEFAULT NULL            COMMENT '操作详情JSON',
  `ip_address`    VARCHAR(64)  DEFAULT NULL            COMMENT 'IP地址',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_operation` (`operation`),
  INDEX `idx_created_at` (`created_at`),
  CONSTRAINT `fk_log_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
