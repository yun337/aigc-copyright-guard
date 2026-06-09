<template>
  <div class="work-detail-page">
    <el-row :gutter="24">
      <el-col :span="16">
        <el-card shadow="never">
          <div class="work-cover" :style="{ background: coverGradient(work.id) }">
            <!-- IMAGE -->
            <template v-if="isImage">
              <img v-if="!mediaError && work.ipfsCid" :src="ipfsUrl(work.ipfsCid)" class="work-cover-img" :alt="work.title" @error="mediaError = true" />
              <span v-if="mediaError || !work.ipfsCid" class="work-cover-title">{{ work.title }}</span>
            </template>
            <!-- VIDEO -->
            <template v-else-if="isVideo">
              <video v-if="!mediaError && work.ipfsCid" :src="ipfsUrl(work.ipfsCid)" class="work-cover-img" controls @error="mediaError = true" />
              <span v-if="mediaError || !work.ipfsCid" class="work-cover-title">{{ work.title }}</span>
            </template>
            <!-- AUDIO -->
            <template v-else-if="isAudio">
              <audio v-if="!mediaError && work.ipfsCid" :src="ipfsUrl(work.ipfsCid)" class="work-audio" controls @error="mediaError = true" />
              <span v-if="mediaError || !work.ipfsCid" class="work-cover-title">{{ work.title }}</span>
            </template>
            <!-- TEXT: 直接显示API内容 -->
            <div v-else-if="isText" class="work-text-viewer">
              <div class="work-text-content">
                <h3 style="color:#fff;margin-bottom:12px">{{ work.title }}</h3>
                <p v-if="work.description" style="margin-bottom:8px">{{ work.description }}</p>
                <pre v-if="work.promptInfo" style="white-space:pre-wrap;font-size:13px;opacity:0.8">{{ work.promptInfo }}</pre>
              </div>
            </div>
            <div class="work-cover-glow"></div>
          </div>
          <h2 class="work-title">{{ work.title }}</h2>
          <p class="work-desc">{{ work.description }}</p>

          <el-descriptions :column="2" border class="mt-20">
            <el-descriptions-item label="作品类型">{{ work.workType }}</el-descriptions-item>
            <el-descriptions-item label="版权状态">
              <el-tag v-if="work.copyrightStatus === 'REGISTERED'" type="success">已存证</el-tag>
              <el-tag v-else type="info">未存证</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创作者">{{ work.username }}</el-descriptions-item>
            <el-descriptions-item label="上传时间">{{ work.createdAt }}</el-descriptions-item>
            <el-descriptions-item label="IPFS CID" :span="2">{{ work.ipfsCid }}</el-descriptions-item>
            <el-descriptions-item label="文件哈希" :span="2">{{ work.fileHash }}</el-descriptions-item>
            <el-descriptions-item v-if="work.promptInfo" label="Prompt信息" :span="2">
              <pre class="prompt-text">{{ work.promptInfo }}</pre>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :span="8">
        <!-- 版权信息 -->
        <el-card shadow="hover" class="side-card" v-if="copyright">
          <template #header><h3>版权存证信息</h3></template>
          <p><strong>证书编号:</strong> {{ copyright.certificateId }}</p>
          <p><strong>交易哈希:</strong> <el-link type="primary">{{ truncate(copyright.txHash) }}</el-link></p>
          <p><strong>区块号:</strong> {{ copyright.blockNumber }}</p>
          <p><strong>存证时间:</strong> {{ copyright.registeredAt }}</p>
          <el-button type="primary" size="small" @click="$router.push(`/copyright/${copyright.id}`)">
            查看版权证书
          </el-button>
        </el-card>

        <!-- NFT信息 -->
        <el-card shadow="hover" class="side-card" v-if="nft">
          <template #header><h3>NFT信息</h3></template>
          <p><strong>Token ID:</strong> {{ nft.tokenId }}</p>
          <p><strong>合约地址:</strong> <el-link type="primary">{{ truncate(nft.contractAddress) }}</el-link></p>
          <p><strong>铸造时间:</strong> {{ nft.mintedAt }}</p>
          <el-button type="warning" size="small" @click="$router.push('/my-nfts')">
            查看NFT详情
          </el-button>
        </el-card>

        <!-- 操作区域 -->
        <el-card shadow="hover" class="side-card" v-if="isOwner">
          <template #header><h3>操作</h3></template>
          <el-button v-if="work.copyrightStatus !== 'REGISTERED'"
            type="primary" style="width:100%;margin-bottom:8px"
            :loading="registering"
            @click="handleRegisterCopyright">
            注册版权存证
          </el-button>
          <el-button v-if="work.copyrightStatus === 'REGISTERED' && !nft"
            type="warning" style="width:100%;margin-bottom:8px"
            @click="handleMintNFT">
            铸造版权NFT
          </el-button>
          <el-button type="info" style="width:100%;margin-bottom:8px"
            @click="$router.push(`/detection?workId=${work.id}`)">
            侵权检测
          </el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { workAPI, copyrightAPI, nftAPI } from '@/api'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const store = useUserStore()

const work = ref({})
const copyright = ref(null)
const nft = ref(null)
const registering = ref(false)
const mediaError = ref(false)

const isOwner = computed(() => store.userId && work.value.userId === store.userId)
const isImage = computed(() => work.value.workType === 'IMAGE')
const isVideo = computed(() => work.value.workType === 'VIDEO')
const isAudio = computed(() => work.value.workType === 'MUSIC')
const isText  = computed(() => work.value.workType === 'TEXT')
const textPreview = computed(() => (work.value.description || work.value.promptInfo || '').substring(0, 500))

onMounted(async () => {
  const workId = route.params.id
  try {
    const res = await workAPI.getDetail(workId)
    if (res.data && res.data.code === 200) {
      work.value = res.data.data
    }

    // 尝试加载版权和NFT信息
    try {
      const crRes = await copyrightAPI.getByWorkId(workId)
      if (crRes.data && crRes.data.code === 200) {
        copyright.value = crRes.data.data
      }
    } catch (e) { /* ignore */ }

    try {
      const nftRes = await nftAPI.getByWorkId(workId)
      if (nftRes.data && nftRes.data.code === 200) {
        nft.value = nftRes.data.data
      }
    } catch (e) { /* ignore */ }
  } catch (e) {
    console.error('加载作品详情失败', e)
  }
})

async function handleRegisterCopyright() {
  registering.value = true
  try {
    const res = await copyrightAPI.register({ workId: work.value.id })
    if (res.data && res.data.code === 200) {
      copyright.value = res.data.data
      work.value.copyrightStatus = 'REGISTERED'
      ElMessage.success('版权存证成功')
    } else if (res.data && res.data.message) {
      ElMessage.error(res.data.message)
    } else {
      ElMessage.error('版权存证失败，请稍后重试')
    }
  } catch (e) {
    console.error('版权存证失败:', e)
    ElMessage.error('版权存证失败，请检查网络连接')
  } finally {
    registering.value = false
  }
}

async function handleMintNFT() {
  try {
    const res = await nftAPI.mint({ workId: work.value.id, royaltyRate: 500 })
    if (res.data && res.data.code === 200) {
      nft.value = res.data.data
      ElMessage.success('NFT铸造成功')
    }
  } catch (e) { /* handled */ }
}

function ipfsUrl(cid) { return cid ? `http://localhost:8081/ipfs/${cid}` : '' }

function coverGradient(id) {
  const colors = [
    ['#6C5CE7', '#A29BFE'], ['#00CEC9', '#55EFC4'], ['#E17055', '#FDCB6E'],
    ['#0984E3', '#74B9FF'], ['#00B894', '#00CEC9'], ['#A29BFE', '#FD79A8'],
    ['#6C5CE7', '#00CEC9'], ['#636E72', '#B2BEC3'],
  ]
  const idx = (id || 0) % colors.length
  return `linear-gradient(135deg, ${colors[idx][0]}, ${colors[idx][1]})`
}

function truncate(str) {
  if (!str) return ''
  return str.substring(0, 10) + '...' + str.substring(str.length - 8)
}
</script>

<style scoped>
.work-detail-page {
  max-width: 1200px;
  margin: 0 auto;
}
.work-cover {
  height: 300px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
}

.work-cover-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  z-index: 1;
}

.work-audio {
  position: relative;
  z-index: 2;
  width: 80%;
  max-width: 400px;
}

.work-text-viewer {
  position: relative;
  z-index: 2;
  width: 90%;
  height: 80%;
  overflow-y: auto;
  padding: 20px;
}

.work-text-content {
  color: rgba(255,255,255,0.9);
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.work-cover-glow {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 30% 40%, rgba(255,255,255,0.12) 0%, transparent 50%),
    radial-gradient(circle at 70% 60%, rgba(255,255,255,0.06) 0%, transparent 40%);
}

.work-cover-title {
  position: relative;
  z-index: 1;
  font-size: 24px;
  font-weight: 800;
  color: rgba(255,255,255,0.9);
  text-shadow: 0 2px 8px rgba(0,0,0,0.2);
}
.work-title {
  font-size: 24px;
  margin-bottom: 8px;
}
.work-desc {
  color: #606266;
  line-height: 1.6;
}
.prompt-text {
  white-space: pre-wrap;
  font-family: monospace;
  font-size: 12px;
  color: #606266;
}
.side-card {
  margin-bottom: 16px;
}
.side-card p {
  margin-bottom: 8px;
  font-size: 13px;
}
.mt-20 {
  margin-top: 20px;
}
</style>
