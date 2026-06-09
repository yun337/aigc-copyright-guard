import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * Axios实例配置
 */
const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 添加Token
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器 - 统一错误处理
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 401: {
          const hadToken = !!localStorage.getItem('accessToken')
          localStorage.removeItem('accessToken')
          // 只在实际有过期token时才提示和跳转，且不在登录页
          if (hadToken && !window.location.pathname.startsWith('/login') && !window.location.pathname.startsWith('/register')) {
            ElMessage.error('登录已过期，请重新登录')
            window.location.href = '/login'
          }
          break
        }
        case 403:
          ElMessage.error('权限不足')
          break
        case 404:
          ElMessage.error('资源不存在')
          break
        case 500:
          ElMessage.error(data?.message || '服务器错误')
          break
        default:
          ElMessage.error(data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络连接失败，请检查网络')
    }
    return Promise.reject(error)
  }
)

export default api

// ============ 认证API ============
export const authAPI = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  getMe: () => api.get('/auth/me'),
  bindWallet: (walletAddress) => api.post('/auth/bind-wallet', { walletAddress })
}

// ============ 作品API ============
export const workAPI = {
  upload: (formData) => api.post('/works/upload', formData, {
    transformRequest: [(data, headers) => { delete headers['Content-Type']; return data }]
  }),
  getList: (page = 0, size = 20) => api.get('/works/list', { params: { page, size } }),
  getMyWorks: (page = 0, size = 20) => api.get('/works/my', { params: { page, size } }),
  getDetail: (workId) => api.get(`/works/${workId}`),
  update: (workId, data) => api.put(`/works/${workId}`, data),
  delete: (workId) => api.delete(`/works/${workId}`)
}

// ============ 版权API ============
export const copyrightAPI = {
  register: (data) => api.post('/copyright/register', data),
  getDetail: (copyrightId) => api.get(`/copyright/${copyrightId}`),
  getByWorkId: (workId) => api.get(`/copyright/by-work/${workId}`),
  getMyCopyrights: () => api.get('/copyright/my'),
  verifyOnChain: (copyrightId) => api.post(`/copyright/verify/${copyrightId}`)
}

// ============ NFT API ============
export const nftAPI = {
  mint: (data) => api.post('/nft/mint', data),
  getDetail: (nftId) => api.get(`/nft/${nftId}`),
  getByWorkId: (workId) => api.get(`/nft/by-work/${workId}`),
  getMyNFTs: () => api.get('/nft/my')
}

// ============ 检测API ============
export const detectionAPI = {
  check: (data) => api.post('/detection/check', data),
  getHistory: (workId) => api.get(`/detection/history/${workId}`),
  getHighSimilarity: (params) => api.get('/detection/high-similarity', { params })
}

// ============ 授权API ============
export const authorizationAPI = {
  grant: (data) => api.post('/authorization/grant', data),
  getDetail: (authId) => api.get(`/authorization/${authId}`),
  getMyGrants: () => api.get('/authorization/my-grants'),
  getMyLicenses: () => api.get('/authorization/my-licenses'),
  revoke: (authId) => api.post(`/authorization/revoke/${authId}`)
}
