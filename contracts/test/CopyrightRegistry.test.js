const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("CopyrightRegistry", function () {
  let copyrightRegistry;
  let owner, creator, otherUser;

  const TEST_FILE_HASH = "QmSHA256Hash1234567890abcdef1234567890abcdef1234567890abcdef";
  const TEST_IPFS_CID = "QmIPFS1234567890abcdef1234567890abcdef1234567890abcdef123456";
  const TEST_META_CID = "QmMETA1234567890abcdef1234567890abcdef1234567890abcdef123456";
  const TEST_TITLE = "AI Generated Artwork #1";
  const TEST_TYPE = "IMAGE";

  beforeEach(async function () {
    [owner, creator, otherUser] = await ethers.getSigners();
    const CopyrightRegistry = await ethers.getContractFactory("CopyrightRegistry");
    copyrightRegistry = await CopyrightRegistry.deploy();
    await copyrightRegistry.waitForDeployment();
  });

  describe("部署", function () {
    it("应该正确设置合约拥有者", async function () {
      expect(await copyrightRegistry.owner()).to.equal(owner.address);
    });

    it("应该给部署者授予管理员角色", async function () {
      const DEFAULT_ADMIN_ROLE = await copyrightRegistry.DEFAULT_ADMIN_ROLE();
      expect(await copyrightRegistry.hasRole(DEFAULT_ADMIN_ROLE, owner.address)).to.be.true;
    });
  });

  describe("版权注册", function () {
    it("应该成功注册版权", async function () {
      const tx = await copyrightRegistry.connect(creator).registerCopyright(
        TEST_FILE_HASH, TEST_IPFS_CID, TEST_META_CID, TEST_TITLE, TEST_TYPE
      );
      const receipt = await tx.wait();

      // 检查事件
      await expect(tx)
        .to.emit(copyrightRegistry, "CopyrightRegistered")
        .withArgs(1, creator.address, TEST_FILE_HASH, TEST_IPFS_CID, await getBlockTimestamp());

      // 检查版权记录
      const record = await copyrightRegistry.getCopyright(1);
      expect(record.creator).to.equal(creator.address);
      expect(record.fileHash).to.equal(TEST_FILE_HASH);
      expect(record.ipfsCid).to.equal(TEST_IPFS_CID);
      expect(record.exists).to.be.true;
    });

    it("应该拒绝重复注册相同文件哈希", async function () {
      await copyrightRegistry.connect(creator).registerCopyright(
        TEST_FILE_HASH, TEST_IPFS_CID, TEST_META_CID, TEST_TITLE, TEST_TYPE
      );
      await expect(
        copyrightRegistry.connect(creator).registerCopyright(
          TEST_FILE_HASH, TEST_IPFS_CID + "2", TEST_META_CID, TEST_TITLE + "2", TEST_TYPE
        )
      ).to.be.revertedWith("CopyrightRegistry: file already registered");
    });

    it("应该拒绝空文件哈希", async function () {
      await expect(
        copyrightRegistry.connect(creator).registerCopyright(
          "", TEST_IPFS_CID, TEST_META_CID, TEST_TITLE, TEST_TYPE
        )
      ).to.be.revertedWith("CopyrightRegistry: empty file hash");
    });
  });

  describe("版权查询", function () {
    beforeEach(async function () {
      await copyrightRegistry.connect(creator).registerCopyright(
        TEST_FILE_HASH, TEST_IPFS_CID, TEST_META_CID, TEST_TITLE, TEST_TYPE
      );
    });

    it("应该根据ID查询版权", async function () {
      const record = await copyrightRegistry.getCopyright(1);
      expect(record.workTitle).to.equal(TEST_TITLE);
    });

    it("应该根据文件哈希查询版权ID", async function () {
      const id = await copyrightRegistry.getCopyrightIdByHash(TEST_FILE_HASH);
      expect(id).to.equal(1);
    });

    it("应该检查文件是否已注册", async function () {
      expect(await copyrightRegistry.isCopyrightRegistered(TEST_FILE_HASH)).to.be.true;
      expect(await copyrightRegistry.isCopyrightRegistered("nonexistent")).to.be.false;
    });
  });

  describe("角色管理", function () {
    it("管理员应该能授予注册者角色", async function () {
      const REGISTRAR_ROLE = await copyrightRegistry.REGISTRAR_ROLE();
      await copyrightRegistry.grantRegistrarRole(otherUser.address);
      expect(await copyrightRegistry.hasRole(REGISTRAR_ROLE, otherUser.address)).to.be.true;
    });
  });
});

describe("CopyrightNFT", function () {
  let copyrightNFT;
  let owner, creator, minter;

  beforeEach(async function () {
    [owner, creator, minter] = await ethers.getSigners();
    const CopyrightNFT = await ethers.getContractFactory("CopyrightNFT");
    copyrightNFT = await CopyrightNFT.deploy();
    await copyrightNFT.waitForDeployment();

    // 授予 minter 铸造权限
    const MINTER_ROLE = await copyrightNFT.MINTER_ROLE();
    await copyrightNFT.grantMinterRole(minter.address);
  });

  describe("NFT铸造", function () {
    it("应该成功铸造NFT", async function () {
      const tx = await copyrightNFT.connect(minter).mintCopyrightNFT(
        creator.address,
        1,
        "QmIPFS123",
        "ipfs://QmTOKENURI",
        500
      );

      await expect(tx)
        .to.emit(copyrightNFT, "NFTMinted")
        .withArgs(1, 1, creator.address, "QmIPFS123", "ipfs://QmTOKENURI");

      const ownerOf = await copyrightNFT.ownerOf(1);
      expect(ownerOf).to.equal(creator.address);
    });

    it("非铸造者不能铸造NFT", async function () {
      await expect(
        copyrightNFT.connect(creator).mintCopyrightNFT(
          creator.address, 1, "QmIPFS123", "ipfs://QmTOKENURI", 500
        )
      ).to.be.reverted;
    });
  });
});

describe("AuthorizationManager", function () {
  let authManager;
  let owner, licensor, licensee;

  beforeEach(async function () {
    [owner, licensor, licensee] = await ethers.getSigners();
    const AuthorizationManager = await ethers.getContractFactory("AuthorizationManager");
    authManager = await AuthorizationManager.deploy();
    await authManager.waitForDeployment();
  });

  describe("授权创建", function () {
    it("应该成功创建授权", async function () {
      const tx = await authManager.connect(licensor).createAuthorization(
        1,
        licensee.address,
        0, // EXCLUSIVE
        365 // 365天
      );

      await expect(tx)
        .to.emit(authManager, "AuthorizationCreated")
        .withArgs(1, 1, licensor.address, licensee.address, 0, 0);

      const auth = await authManager.getAuthorization(1);
      expect(auth.licensor).to.equal(licensor.address);
      expect(auth.licensee).to.equal(licensee.address);
      expect(auth.active).to.be.true;
    });
  });
});

async function getBlockTimestamp() {
  const block = await ethers.provider.getBlock("latest");
  return block ? block.timestamp : Math.floor(Date.now() / 1000);
}
