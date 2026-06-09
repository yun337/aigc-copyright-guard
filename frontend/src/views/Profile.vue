<template>
  <div class="profile-page">
    <el-card shadow="never">
      <template #header><h2>个人中心</h2></template>

      <el-descriptions :column="2" border size="large">
        <el-descriptions-item label="用户ID">{{ userInfo.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ userInfo.username }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ userInfo.email || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="userInfo.role === 'ADMIN' ? 'danger' : 'info'">
            {{ userInfo.role === 'ADMIN' ? '管理员' : '普通用户' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="钱包地址">
          <template v-if="userInfo.walletAddress">
            <el-tag type="success">{{ userInfo.walletAddress }}</el-tag>
          </template>
          <template v-else>
            <el-button size="small" type="warning" @click="handleBindWallet">
              <el-icon><Wallet /></el-icon>绑定钱包
            </el-button>
          </template>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ userInfo.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 统计数据 -->
    <el-row :gutter="20" class="mt-20">
      <el-col :span="8">
        <el-statistic title="上传作品" :value="stats.works" />
      </el-col>
      <el-col :span="8">
        <el-statistic title="版权存证" :value="stats.copyrights" />
      </el-col>
      <el-col :span="8">
        <el-statistic title="铸造NFT" :value="stats.nfts" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { authAPI, workAPI, copyrightAPI, nftAPI } from '@/api'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const store = useUserStore()

const userInfo = ref({})
const stats = ref({ works: 0, copyrights: 0, nfts: 0 })

onMounted(async () => {
  try {
    const res = await authAPI.getMe()
    if (res.data && res.data.code === 200) {
      userInfo.value = res.data.data
    }

    // 加载统计数据
    const [worksRes, copyrightsRes, nftsRes] = await Promise.allSettled([
      workAPI.getMyWorks(0, 1),
      copyrightAPI.getMyCopyrights(),
      nftAPI.getMyNFTs()
    ])

    if (worksRes.status === 'fulfilled' && worksRes.value?.data?.code === 200) {
      stats.value.works = worksRes.value.data.data.totalElements || 0
    }
    if (copyrightsRes.status === 'fulfilled' && copyrightsRes.value?.data?.code === 200) {
      stats.value.copyrights = (copyrightsRes.value.data.data || []).length
    }
    if (nftsRes.status === 'fulfilled' && nftsRes.value?.data?.code === 200) {
      stats.value.nfts = (nftsRes.value.data.data || []).length
    }
  } catch (e) {
    console.error(e)
  }
})

async function handleBindWallet() {
  if (!window.ethereum) {
    ElMessage.warning('请安装MetaMask钱包')
    return
  }
  try {
    const accounts = await window.ethereum.request({ method: 'eth_requestAccounts' })
    if (accounts.length > 0) {
      await authAPI.bindWallet(accounts[0])
      userInfo.value.walletAddress = accounts[0]
      store.setWallet(accounts[0])
      ElMessage.success('钱包绑定成功')
    }
  } catch (e) {
    ElMessage.error('钱包绑定失败')
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 800px;
  margin: 0 auto;
}
.mt-20 {
  margin-top: 20px;
}
</style>
