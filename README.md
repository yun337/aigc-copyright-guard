# aigc-copyright-guard
# aigc-copyright-guard
# CopyrightGuard - 链创守护

基于以太坊区块链的 **AIGC 作品版权保护平台**，提供版权存证 → NFT 铸造 → AI 侵权检测 → 授权交易全链路保护。

## ✨ 功能特性

- **版权存证**：SHA256 文件哈希上链，去中心化存证，不可篡改
- **NFT 铸造**：ERC-721 标准，支持版税率设置（0-100%）
- **AI 侵权检测**：基于 OpenAI CLIP ViT-B/32 的语义相似度比对
- **版权证书**：自动生成并支持 PDF 下载
- **链上验证**：验证作品版权真实性

## 🛠️ 技术栈

| 维度 | 技术选型 |
|------|---------|
| 前端 | Vue 3 + Vite + Element Plus + Ethers.js v6 |
| 后端 | Spring Boot 3.2 + Spring Security + JPA + web3j |
| AI 服务 | Flask + PyTorch + OpenAI CLIP ViT-B/32 |
| 区块链 | Solidity 0.8.20 + Hardhat 本地链 (Chain ID: 1337) |
| 存储 | MySQL 8.0 + IPFS Kubo + Redis 7 |
| 部署 | Docker Compose (7 个容器) |

## 📁 项目结构

```
AIGC/
├── start.bat                    # Windows 一键启动脚本
├── README.md                    # 项目说明文档
│
├── contracts/                   # 智能合约 (Hardhat + Solidity)
│   ├── contracts/
│   │   ├── CopyrightRegistry.sol    # 版权存证合约
│   │   ├── CopyrightNFT.sol         # NFT 铸造合约 (ERC-721)
│   │   └── AuthorizationManager.sol # 授权管理合约
│   ├── scripts/deploy.js            # Hardhat 部署脚本
│   ├── deploy_now.js                # 备选部署脚本
│   └── test/CopyrightRegistry.test.js
│
├── backend/                     # Spring Boot 后端 (Java 17)
│   ├── src/main/java/com/copyright/
│   │   ├── CopyrightGuardApplication.java
│   │   ├── config/                  # Security, Jackson, RestTemplate
│   │   ├── controller/              # Auth, Work, Copyright, NFT, Infringement
│   │   ├── service/                 # 业务逻辑层
│   │   ├── model/entity/            # User, Work, Copyright, NFT, Infringement
│   │   ├── model/dto/               # 请求/响应 DTO
│   │   ├── repository/              # JPA Repository
│   │   ├── security/                # JWT 认证
│   │   ├── exception/               # BlockchainException (checked)
│   │   └── utils/                   # BlockchainUtil, IPFSUtil, SHA256Util
│   ├── sql/01-schema.sql           # 7 张表建表脚本
│   └── mvnw.cmd                    # Maven Wrapper
│
├── frontend/                    # Vue3 前端
│   ├── src/
│   │   ├── router/index.js          # 路由 + 导航守卫
│   │   ├── store/user.js            # Pinia 状态管理
│   │   ├── api/index.js             # Axios 封装
│   │   ├── utils/web3.js            # Ethers.js 钱包连接
│   │   └── views/                   # 11 个页面组件
│   └── vite.config.js
│
├── ai-service/                  # Flask AI 服务
│   ├── app/main.py                  # Flask API
│   ├── app/feature_extractor.py     # CLIP 特征提取
│   └── app/similarity_calculator.py # 余弦相似度计算
│
├── deploy/                      # Docker 编排
│   └── docker-compose.yml          # 容器编排配置
│
└── docs/                        # 项目文档
    ├── 01-系统架构设计文档.md
    ├── 02-项目目录结构.md
    ├── 03-部署方案.md
    └── 04-PPT大纲.md
```

## 🚀 快速开始

### 环境要求

| 组件 | 版本 |
|------|------|
| Docker Desktop | 24+ |
| Node.js | 18+ |
| JDK | 17 |

### 一键启动（Windows）

```cmd
# 双击项目根目录的 start.bat 即可启动所有服务
.\start.bat
```

### 手动启动

```cmd
# 1. 启动 Docker 服务
cd deploy
docker-compose up -d mysql redis ipfs

# 2. 启动 Hardhat 节点
docker-compose --profile dev up -d hardhat-node
timeout /t 10
docker exec copyright-hardhat sh -c "cd /app && npx hardhat run scripts/deploy.js --network localhost"

# 3. 启动后端（设置环境变量）
set ETH_PRIVATE_KEY=0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80
set COPYRIGHT_REGISTRY_ADDR=0x5FbDB2315678afecb367f032d93F642f64180aa3
set COPYRIGHT_NFT_ADDR=0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512
cd ..\backend
.\mvnw.cmd spring-boot:run

# 4. 启动前端
cd ..\frontend
npm run dev
```

### 访问地址

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:5173 |
| 后端 API | http://localhost:8080/api |
| IPFS Gateway | http://localhost:8081/ipfs/ |
| Hardhat RPC | http://localhost:8545 |

## 🔧 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| ETH_RPC_URL | http://localhost:8545 | Hardhat RPC 地址 |
| ETH_PRIVATE_KEY | - | 部署账户私钥 |
| COPYRIGHT_REGISTRY_ADDR | - | 版权注册合约地址 |
| COPYRIGHT_NFT_ADDR | - | NFT 合约地址 |
| MYSQL_PASSWORD | root | 数据库密码 |

### 测试账户 (Hardhat)

| 账户 | 地址 |
|------|------|
| #0 | 0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266 |
| 私钥 | 0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80 |

## 📡 API 接口

### 版权存证
- `POST /api/copyright/register` - 注册版权
- `GET /api/copyright/{id}` - 查询版权详情
- `GET /api/copyright/work/{workId}` - 根据作品查询版权
- `GET /api/copyright/{id}/verify` - 链上验证

### NFT 铸造
- `POST /api/nft/mint` - 铸造 NFT
- `GET /api/nft/{id}` - 查询 NFT 详情

### 作品管理
- `POST /api/work/upload` - 上传作品
- `GET /api/work/{id}` - 查询作品详情
- `DELETE /api/work/{id}` - 删除作品

## 📊 核心业务流程

```
版权存证：用户上传 → SHA256 哈希 → IPFS 存储 → 调用合约上链 → 生成证书

NFT 铸造：版权已存证 → IPFS 元数据 → 调用 mintCopyrightNFT() → 归属用户钱包

AI 检测：上传时提取 CLIP 特征 → 比对余弦相似度 ≥ 阈值 → 标记疑似侵权
```

## 🐛 故障排查

| 问题 | 原因 | 解决 |
|------|------|------|
| 版权存证报 `rollback-only` | RuntimeException 污染事务 | 使用 checked exception |
| 点击按钮无反应 | 前端未处理错误响应 | 完善 else + catch 分支 |
| MySQL 连接拒绝 | 容器未启动 | `docker-compose up -d mysql` |
| 端口 8080 被占用 | 旧进程残留 | `taskkill /F /PID xxx` |

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

**链创守护** - AIGC 作品版权保护平台 | Powered by Ethereum + IPFS + AI
