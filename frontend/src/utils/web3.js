import { ethers } from 'ethers'

/**
 * Web3 钱包工具
 * 负责MetaMask钱包连接、合约交互
 */

// 合约ABI（简化版，仅包含常用函数）
const COPYRIGHT_REGISTRY_ABI = [
  "function registerCopyright(string,string,string,string,string) returns (uint256)",
  "function getCopyright(uint256) view returns (tuple(uint256,address,string,string,string,string,string,string,uint256,bool))",
  "function isCopyrightRegistered(string) view returns (bool)",
  "function getCopyrightIdByHash(string) view returns (uint256)",
  "function getCreatorCopyrights(address) view returns (uint256[])",
  "function reportInfringement(uint256,string,string,uint256) returns (uint256)"
]

const COPYRIGHT_NFT_ABI = [
  "function mintCopyrightNFT(address,uint256,string,string,uint256) returns (uint256)",
  "function getNFTMetadata(uint256) view returns (tuple(uint256,uint256,string,address,uint256,uint256))",
  "function getTokenByCopyright(uint256) view returns (uint256)",
  "function getCreatorNFTs(address) view returns (uint256[])",
  "function ownerOf(uint256) view returns (address)",
  "function tokenURI(uint256) view returns (string)"
]

const AUTHORIZATION_MANAGER_ABI = [
  "function createAuthorization(uint256,address,uint8,uint256) payable returns (uint256)",
  "function getAuthorization(uint256) view returns (tuple(uint256,uint256,address,address,uint8,uint256,uint256,uint256,bool,uint256))",
  "function isAuthorizationValid(uint256) view returns (bool)",
  "function revokeAuthorization(uint256)"
]

/**
 * 连接MetaMask钱包
 */
export async function connectWallet() {
  if (!window.ethereum) {
    throw new Error('请安装MetaMask钱包插件')
  }

  try {
    // 请求连接
    const accounts = await window.ethereum.request({
      method: 'eth_requestAccounts'
    })

    const provider = new ethers.BrowserProvider(window.ethereum)
    const signer = await provider.getSigner()
    const network = await provider.getNetwork()

    return {
      address: accounts[0],
      chainId: Number(network.chainId),
      provider,
      signer
    }
  } catch (error) {
    if (error.code === 4001) {
      throw new Error('用户拒绝了钱包连接')
    }
    throw error
  }
}

/**
 * 获取当前连接的钱包
 */
export async function getCurrentWallet() {
  if (!window.ethereum) return null

  const accounts = await window.ethereum.request({
    method: 'eth_accounts'
  })

  if (accounts.length === 0) return null

  const provider = new ethers.BrowserProvider(window.ethereum)
  const signer = await provider.getSigner()

  return {
    address: accounts[0],
    signer
  }
}

/**
 * 获取合约实例
 */
export function getCopyrightRegistry(address, signer) {
  return new ethers.Contract(address, COPYRIGHT_REGISTRY_ABI, signer)
}

export function getCopyrightNFT(address, signer) {
  return new ethers.Contract(address, COPYRIGHT_NFT_ABI, signer)
}

export function getAuthorizationManager(address, signer) {
  return new ethers.Contract(address, AUTHORIZATION_MANAGER_ABI, signer)
}

/**
 * 监听账户切换
 */
export function onAccountsChanged(callback) {
  if (window.ethereum) {
    window.ethereum.on('accountsChanged', callback)
  }
}

/**
 * 监听链切换
 */
export function onChainChanged(callback) {
  if (window.ethereum) {
    window.ethereum.on('chainChanged', callback)
  }
}

export default {
  connectWallet,
  getCurrentWallet,
  getCopyrightRegistry,
  getCopyrightNFT,
  getAuthorizationManager,
  onAccountsChanged,
  onChainChanged
}
