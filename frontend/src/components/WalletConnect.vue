<template>
  <div class="wc">
    <template v-if="connected">
      <el-popover placement="bottom" :width="180" trigger="click">
        <template #reference>
          <div class="wc-badge" style="cursor:pointer">
            <span class="wc-dot"></span>
            <span class="wc-addr">{{ shortAddress }}</span>
            <span class="wc-eth">ETH</span>
          </div>
        </template>
        <div style="text-align:center;padding:4px 0">
          <p style="font-family:monospace;font-size:12px;color:#606266;margin-bottom:12px;word-break:break-all">{{ address }}</p>
          <el-button size="small" type="danger" plain @click="handleDisconnect">断开钱包</el-button>
        </div>
      </el-popover>
    </template>
    <template v-else>
      <button class="wc-btn" :disabled="connecting" @click="handleConnect">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/>
        </svg>
        {{ connecting ? '连接中...' : '连接钱包' }}
      </button>
    </template>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { connectWallet } from '@/utils/web3'
import { useUserStore } from '@/store/user'
import { authAPI } from '@/api'
import { ElMessage } from 'element-plus'

const store = useUserStore()
const connected = ref(false)
const connecting = ref(false)
const address = ref('')
const shortAddress = ref('')

// 退出登录时自动断开钱包
watch(() => store.isLoggedIn, (loggedIn) => {
  if (!loggedIn && connected.value) {
    connected.value = false
    address.value = ''
    shortAddress.value = ''
  }
})

async function handleConnect() {
  if (!store.isLoggedIn) {
    ElMessage.warning('请先登录后再连接钱包')
    return
  }
  connecting.value = true
  try {
    const wallet = await connectWallet()
    address.value = wallet.address
    shortAddress.value = shorten(wallet.address)
    connected.value = true
    store.setWallet(wallet.address)
    try { await authAPI.bindWallet(wallet.address) } catch (_) {}
    ElMessage.success('钱包已连接')
  } catch (e) {
    ElMessage.error(e.message || '连接失败')
  } finally { connecting.value = false }
}

function handleDisconnect() {
  connected.value = false
  address.value = ''
  shortAddress.value = ''
  ElMessage.info('钱包已断开')
}

function shorten(addr) {
  return addr ? addr.slice(0, 6) + '...' + addr.slice(-4) : ''
}
</script>

<style scoped>
.wc-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border: 1.5px solid #F59E0B;
  border-radius: 100px;
  background: #FFFBEB;
  color: #D97706;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all .2s;
}

.wc-btn:hover { background: #FEF3C7; }

.wc-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: 100px;
  background: #ECFDF5;
  border: 1.5px solid #A7F3D0;
}

.wc-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #10B981;
}

.wc-addr {
  font-size: 13px;
  font-weight: 600;
  font-family: 'SF Mono', 'Fira Code', monospace;
  color: #065F46;
}

.wc-eth {
  font-size: 10px;
  color: #059669;
  background: #D1FAE5;
  padding: 2px 8px;
  border-radius: 100px;
  font-weight: 700;
}
</style>
