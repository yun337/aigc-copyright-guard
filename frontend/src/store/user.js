import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authAPI } from '@/api'

/**
 * 用户状态管理
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('accessToken') || '')
  const userId = ref(Number(localStorage.getItem('userId')) || null)
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')
  const walletAddress = ref(localStorage.getItem('walletAddress') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')

  function setAuth(data) {
    token.value = data.accessToken
    userId.value = data.userId
    username.value = data.username
    role.value = data.role
    walletAddress.value = data.walletAddress || ''

    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role)
    if (data.walletAddress) {
      localStorage.setItem('walletAddress', data.walletAddress)
    }
  }

  function setWallet(address) {
    walletAddress.value = address
    localStorage.setItem('walletAddress', address)
  }

  function logout() {
    token.value = ''
    userId.value = null
    username.value = ''
    role.value = ''
    walletAddress.value = ''

    localStorage.removeItem('accessToken')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    localStorage.removeItem('walletAddress')
  }

  async function fetchUserInfo() {
    if (!token.value) return
    try {
      const res = await authAPI.getMe()
      if (res.data && res.data.code === 200) {
        const user = res.data.data
        username.value = user.username
        role.value = user.role
        walletAddress.value = user.walletAddress || ''
      }
    } catch (e) {
      console.error('获取用户信息失败', e)
    }
  }

  return {
    token, userId, username, role, walletAddress,
    isLoggedIn, isAdmin,
    setAuth, setWallet, logout, fetchUserInfo
  }
})
