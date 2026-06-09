/**
 * 链创守护 - IPFS上传服务模块
 * 封装IPFS文件上传、元数据上传和检索功能
 */

const { create } = require('ipfs-http-client');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

// IPFS客户端配置
const IPFS_HOST = process.env.IPFS_HOST || 'localhost';
const IPFS_PORT = process.env.IPFS_PORT || '5001';
const IPFS_PROTOCOL = process.env.IPFS_PROTOCOL || 'http';

/**
 * IPFS服务类
 * 提供文件上传、JSON上传、CID检索等功能
 */
class IPFSService {
  constructor() {
    this.client = null;
    this.initialized = false;
  }

  /**
   * 初始化IPFS客户端连接
   */
  async init() {
    if (this.initialized) return;
    try {
      this.client = create({
        host: IPFS_HOST,
        port: IPFS_PORT,
        protocol: IPFS_PROTOCOL,
      });
      // 测试连接
      const version = await this.client.version();
      console.log(`[IPFS] Connected to IPFS node v${version.version}`);
      this.initialized = true;
    } catch (error) {
      console.error('[IPFS] Connection failed:', error.message);
      throw new Error('Failed to connect to IPFS node');
    }
  }

  /**
   * 上传文件到IPFS
   * @param {string|Buffer} filePath - 文件路径或Buffer
   * @param {string} fileName - 文件名
   * @returns {Promise<{cid: string, size: number}>}
   */
  async uploadFile(filePath, fileName) {
    await this.init();

    let fileBuffer;
    if (Buffer.isBuffer(filePath)) {
      fileBuffer = filePath;
    } else {
      fileBuffer = fs.readFileSync(filePath);
    }

    const result = await this.client.add({
      path: fileName,
      content: fileBuffer,
    });

    console.log(`[IPFS] File uploaded: ${result.cid.toString()} (${result.size} bytes)`);
    return {
      cid: result.cid.toString(),
      size: result.size,
      path: result.path,
    };
  }

  /**
   * 上传JSON元数据到IPFS
   * @param {Object} metadata - 元数据对象
   * @returns {Promise<{cid: string}>}
   */
  async uploadMetadata(metadata) {
    await this.init();

    const metadataStr = JSON.stringify(metadata, null, 2);
    const metadataBuffer = Buffer.from(metadataStr, 'utf-8');

    const result = await this.client.add({
      path: 'metadata.json',
      content: metadataBuffer,
    });

    console.log(`[IPFS] Metadata uploaded: ${result.cid.toString()}`);
    return {
      cid: result.cid.toString(),
      metadata: metadata,
    };
  }

  /**
   * 上传AIGC作品（文件+元数据+Prompt）
   * @param {Object} params - 作品参数
   * @returns {Promise<Object>} - 返回CID信息
   */
  async uploadArtwork(params) {
    const {
      fileBuffer,
      fileName,
      title,
      description,
      promptInfo,
      workType,
      creator
    } = params;

    await this.init();

    // 1. 上传作品文件
    const fileResult = await this.uploadFile(fileBuffer, fileName);

    // 2. 计算文件哈希
    const fileHash = crypto
      .createHash('sha256')
      .update(fileBuffer)
      .digest('hex');

    // 3. 构建元数据
    const metadata = {
      title: title,
      description: description || '',
      promptInfo: promptInfo || '',
      workType: workType || 'IMAGE',
      fileHash: fileHash,
      ipfsCid: fileResult.cid,
      fileName: fileName,
      creator: creator || '',
      platform: 'CopyrightGuard',
      version: '1.0',
      timestamp: new Date().toISOString(),
    };

    // 4. 上传元数据
    const metadataResult = await this.uploadMetadata(metadata);

    console.log(`[IPFS] Artwork uploaded successfully: ${title}`);
    return {
      fileCid: fileResult.cid,
      metadataCid: metadataResult.cid,
      fileHash: fileHash,
      fileSize: fileResult.size,
      fileName: fileName,
      metadata: metadata,
    };
  }

  /**
   * 从IPFS获取文件内容
   * @param {string} cid - IPFS CID
   * @returns {Promise<Buffer>}
   */
  async getFile(cid) {
    await this.init();

    const chunks = [];
    for await (const chunk of this.client.cat(cid)) {
      chunks.push(chunk);
    }

    return Buffer.concat(chunks);
  }

  /**
   * 从IPFS获取JSON内容
   * @param {string} cid - IPFS CID
   * @returns {Promise<Object>}
   */
  async getJSON(cid) {
    const data = await this.getFile(cid);
    return JSON.parse(data.toString('utf-8'));
  }

  /**
   * 生成IPFS网关URL
   * @param {string} cid - IPFS CID
   * @returns {string}
   */
  getGatewayURL(cid) {
    return `http://${IPFS_HOST}:8080/ipfs/${cid}`;
  }

  /**
   * Pin内容到本地节点
   * @param {string} cid - IPFS CID
   */
  async pin(cid) {
    await this.init();
    await this.client.pin.add(cid);
    console.log(`[IPFS] Pinned: ${cid}`);
  }
}

// 导出单例
const ipfsService = new IPFSService();

module.exports = { IPFSService, ipfsService };

// ============ 命令行入口 ============
if (require.main === module) {
  const args = process.argv.slice(2);
  const command = args[0];

  async function main() {
    switch (command) {
      case 'upload':
        const filePath = args[1];
        if (!filePath) {
          console.error('Usage: node upload.js upload <file-path>');
          process.exit(1);
        }
        const result = await ipfsService.uploadArtwork({
          fileBuffer: fs.readFileSync(filePath),
          fileName: path.basename(filePath),
          title: path.basename(filePath),
          workType: 'IMAGE',
        });
        console.log(JSON.stringify(result, null, 2));
        break;

      case 'get':
        const cid = args[1];
        if (!cid) {
          console.error('Usage: node upload.js get <cid>');
          process.exit(1);
        }
        const data = await ipfsService.getJSON(cid);
        console.log(JSON.stringify(data, null, 2));
        break;

      case 'gateway':
        const gwCid = args[1];
        console.log(ipfsService.getGatewayURL(gwCid));
        break;

      default:
        console.log('IPFS Upload Service');
        console.log('  upload <file>  - Upload file to IPFS');
        console.log('  get <cid>      - Get content from IPFS');
        console.log('  gateway <cid>  - Get gateway URL');
    }
  }

  main().catch(console.error);
}
