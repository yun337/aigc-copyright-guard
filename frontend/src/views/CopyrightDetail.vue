<template>
  <div class="copyright-page">
    <el-card shadow="never" class="certificate-card">
      <div class="cert-header">
        <el-icon :size="48" color="#409EFF"><TrophyBase /></el-icon>
        <div>
          <h1>版权存证证书</h1>
          <p>Copyright Certificate of AIGC Work</p>
        </div>
      </div>

      <el-divider />

      <el-descriptions :column="2" border size="large" v-if="copyright">
        <el-descriptions-item label="证书编号" :span="2">
          <strong style="font-size:18px;color:#409EFF">{{ copyright.certificateId }}</strong>
        </el-descriptions-item>
        <el-descriptions-item label="作品名称">{{ copyright.work?.title }}</el-descriptions-item>
        <el-descriptions-item label="作品类型">{{ copyright.work?.workType }}</el-descriptions-item>
        <el-descriptions-item label="创作者">{{ copyright.work?.creator }}</el-descriptions-item>
        <el-descriptions-item label="存证时间">{{ copyright.registeredAt }}</el-descriptions-item>
        <el-descriptions-item label="文件哈希" :span="2">
          <span style="font-family:monospace;font-size:12px">{{ copyright.work?.fileHash }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="IPFS CID" :span="2">
          <span style="font-family:monospace;font-size:12px">{{ copyright.work?.ipfsCid }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="交易哈希" :span="2">
          <el-link type="primary" style="font-family:monospace;font-size:12px">
            {{ copyright.txHash }}
          </el-link>
        </el-descriptions-item>
        <el-descriptions-item label="区块号">{{ copyright.blockNumber }}</el-descriptions-item>
        <el-descriptions-item label="合约地址">
          <span style="font-family:monospace;font-size:12px">{{ truncate(copyright.contractAddress) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="Prompt信息" :span="2" v-if="copyright.work?.promptInfo">
          <pre style="white-space:pre-wrap;font-size:13px">{{ copyright.work.promptInfo }}</pre>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <div class="cert-footer">
        <el-button type="primary" @click="handleVerifyOnChain" :loading="verifying">
          <el-icon><Check /></el-icon>链上验证
        </el-button>
        <el-button @click="handleDownload">下载版权证书</el-button>
        <el-button @click="$router.push(`/works/${copyright.work?.id}`)">查看作品</el-button>
      </div>

      <!-- 链上验证结果 -->
      <el-alert
        v-if="verifyResult"
        :title="verifyResult.verified ? '✓ 链上验证通过' : '✗ 链上验证失败'"
        :type="verifyResult.verified ? 'success' : 'error'"
        :closable="true"
        show-icon
        class="mt-20"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { copyrightAPI } from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const copyright = ref(null)
const verifying = ref(false)
const verifyResult = ref(null)

onMounted(async () => {
  try {
    const res = await copyrightAPI.getDetail(route.params.id)
    if (res.data && res.data.code === 200) {
      copyright.value = res.data.data
    }
  } catch (e) {
    console.error(e)
  }
})

async function handleVerifyOnChain() {
  verifying.value = true
  try {
    const res = await copyrightAPI.verifyOnChain(route.params.id)
    if (res.data && res.data.code === 200) {
      verifyResult.value = res.data.data
    }
  } catch (e) { /* handled */ } finally {
    verifying.value = false
  }
}

const downloading = ref(false)

function handleDownload() {
  downloading.value = true
  const c = copyright.value
  if (!c) return

  const a = document.createElement('a')
  a.href = `/api/copyright/${c.id}/download`
  a.download = `Copyright_Certificate_${c.certificateId}.pdf`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  ElMessage.success('正在下载PDF证书')
  downloading.value = false
}

function truncate(str) {
  if (!str) return ''
  return str.substring(0, 10) + '...' + str.substring(str.length - 8)
}
</script>

<style scoped>
.copyright-page {
  max-width: 900px;
  margin: 0 auto;
}
.certificate-card {
  padding: 20px;
}
.cert-header {
  display: flex;
  align-items: center;
  gap: 20px;
}
.cert-header h1 {
  font-size: 28px;
  margin-bottom: 4px;
}
.cert-footer {
  display: flex;
  gap: 12px;
}
.mt-20 {
  margin-top: 20px;
}
</style>
