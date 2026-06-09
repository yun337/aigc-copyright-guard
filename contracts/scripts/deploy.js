const { ethers } = require("hardhat");

/**
 * 链创守护 - 智能合约部署脚本
 * 部署顺序: CopyrightRegistry -> CopyrightNFT -> AuthorizationManager
 */
async function main() {
  console.log("========================================");
  console.log("  链创守护 - 智能合约部署");
  console.log("========================================\n");

  const [deployer] = await ethers.getSigners();
  console.log("部署账户:", deployer.address);
  console.log("账户余额:", ethers.formatEther(await ethers.provider.getBalance(deployer.address)), "ETH\n");

  // ============ 1. 部署 CopyrightRegistry ============
  console.log("[1/3] 部署 CopyrightRegistry...");
  const CopyrightRegistry = await ethers.getContractFactory("CopyrightRegistry");
  const copyrightRegistry = await CopyrightRegistry.deploy();
  await copyrightRegistry.waitForDeployment();
  const copyrightRegistryAddress = await copyrightRegistry.getAddress();
  console.log("  ✓ CopyrightRegistry 已部署:", copyrightRegistryAddress);

  // ============ 2. 部署 CopyrightNFT ============
  console.log("[2/3] 部署 CopyrightNFT...");
  const CopyrightNFT = await ethers.getContractFactory("CopyrightNFT");
  const copyrightNFT = await CopyrightNFT.deploy();
  await copyrightNFT.waitForDeployment();
  const copyrightNFTAddress = await copyrightNFT.getAddress();
  console.log("  ✓ CopyrightNFT 已部署:", copyrightNFTAddress);

  // ============ 3. 部署 AuthorizationManager ============
  console.log("[3/3] 部署 AuthorizationManager...");
  const AuthorizationManager = await ethers.getContractFactory("AuthorizationManager");
  const authorizationManager = await AuthorizationManager.deploy();
  await authorizationManager.waitForDeployment();
  const authorizationManagerAddress = await authorizationManager.getAddress();
  console.log("  ✓ AuthorizationManager 已部署:", authorizationManagerAddress);

  // ============ 4. 设置合约间关联 ============
  console.log("\n[配置] 设置合约间关联...");

  // 给后端服务授予注册者角色
  const REGISTRAR_ROLE = ethers.keccak256(ethers.toUtf8Bytes("REGISTRAR_ROLE"));
  const MINTER_ROLE = ethers.keccak256(ethers.toUtf8Bytes("MINTER_ROLE"));

  // 部署者默认已有这些角色，此处可额外添加后端服务账户
  console.log("  ✓ 部署者已自动获得所有管理角色");

  // ============ 5. 输出部署摘要 ============
  console.log("\n========================================");
  console.log("  部署摘要");
  console.log("========================================");
  console.log("网络:", (await ethers.provider.getNetwork()).name);
  console.log("链ID:", (await ethers.provider.getNetwork()).chainId.toString());
  console.log("");
  console.log("CopyrightRegistry:", copyrightRegistryAddress);
  console.log("CopyrightNFT:     ", copyrightNFTAddress);
  console.log("AuthManager:      ", authorizationManagerAddress);
  console.log("");

  // ============ 6. 保存部署地址 ============
  const fs = require("fs");
  const path = require("path");

  const deployInfo = {
    network: (await ethers.provider.getNetwork()).name,
    chainId: (await ethers.provider.getNetwork()).chainId.toString(),
    deployer: deployer.address,
    contracts: {
      copyrightRegistry: copyrightRegistryAddress,
      copyrightNFT: copyrightNFTAddress,
      authorizationManager: authorizationManagerAddress,
    },
    deployedAt: new Date().toISOString(),
  };

  // 保存到 contracts 目录
  const deployPath = path.join(__dirname, "..", "deployments.json");
  fs.writeFileSync(deployPath, JSON.stringify(deployInfo, null, 2));
  console.log(`部署信息已保存到: ${deployPath}`);

  // 同时保存到 backend 的 resources 目录
  const backendDeployPath = path.join(
    __dirname, "..", "..", "backend", "src", "main", "resources", "contract-addresses.json"
  );
  const backendDir = path.dirname(backendDeployPath);
  if (!fs.existsSync(backendDir)) {
    fs.mkdirSync(backendDir, { recursive: true });
  }
  fs.writeFileSync(backendDeployPath, JSON.stringify(deployInfo, null, 2));

  console.log("\n部署完成! 🎉");
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error("部署失败:", error);
    process.exit(1);
  });
