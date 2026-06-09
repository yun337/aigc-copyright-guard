<template>
  <div class="upload-page">
    <el-card shadow="never">
      <template #header>
        <h2>上传AIGC作品</h2>
      </template>

      <el-steps :active="activeStep" align-center finish-status="success" class="mb-24">
        <el-step title="上传作品" />
        <el-step title="版权存证" />
        <el-step title="NFT铸造" />
        <el-step title="完成" />
      </el-steps>

      <!-- 步骤1: 上传作品 -->
      <div v-show="activeStep === 0">
        <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-position="top">
          <el-row :gutter="24">
            <el-col :span="14">
              <el-form-item label="作品标题" prop="title">
                <el-input v-model="uploadForm.title" placeholder="请输入作品标题" />
              </el-form-item>
            </el-col>
            <el-col :span="10">
              <el-form-item label="作品类型" prop="workType">
                <el-select v-model="uploadForm.workType" style="width:100%">
                  <el-option label="图片" value="IMAGE" />
                  <el-option label="视频" value="VIDEO" />
                  <el-option label="音乐" value="MUSIC" />
                  <el-option label="文本" value="TEXT" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="作品描述">
            <el-input v-model="uploadForm.description" type="textarea" :rows="3" placeholder="描述你的作品..." />
          </el-form-item>
          <el-form-item label="AI Prompt信息">
            <el-input v-model="uploadForm.promptInfo" type="textarea" :rows="4"
              placeholder="输入生成此作品使用的Prompt，这将成为版权存证的重要证据" />
          </el-form-item>
          <el-form-item label="作品文件" prop="file">
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :limit="1"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              :before-upload="beforeUpload"
              accept="image/*,.mp4,.mp3,.txt,.pdf"
              drag
            >
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <div class="el-upload__text">
                将文件拖到此处，或<em>点击上传</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  支持 JPG/PNG/MP4/MP3/TXT/PDF，单个文件不超过 50MB
                </div>
              </template>
            </el-upload>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" :loading="uploading" @click="handleUpload">
              <el-icon><Upload /></el-icon>上传并开始存证
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 步骤2: 版权存证 -->
      <div v-show="activeStep === 1">
        <el-result icon="success" title="作品上传成功">
          <template #sub-title>
            <span style="word-break:break-all;font-size:12px">IPFS CID: {{ uploadResult?.ipfsCid || '' }}</span>
          </template>
          <template #extra>
            <el-descriptions :column="1" border class="desc-table">
              <el-descriptions-item label="文件哈希" label-width="80">
                <span class="hash-text">{{ truncateHash(uploadResult?.fileHash) }}</span>
                <el-button link type="primary" size="small" @click="copyText(uploadResult?.fileHash)">复制</el-button>
              </el-descriptions-item>
              <el-descriptions-item label="IPFS CID" label-width="80">
                <span class="hash-text">{{ truncateHash(uploadResult?.ipfsCid) }}</span>
                <el-button link type="primary" size="small" @click="copyText(uploadResult?.ipfsCid)">复制</el-button>
              </el-descriptions-item>
              <el-descriptions-item label="文件大小" label-width="80">{{ formatFileSize(uploadResult?.fileSize) }}</el-descriptions-item>
              <el-descriptions-item label="上传时间" label-width="80">{{ uploadResult?.createdAt }}</el-descriptions-item>
            </el-descriptions>
            <div class="mt-20">
              <el-button type="primary" size="large" :loading="registering" @click="handleRegisterCopyright">
                注册版权存证
              </el-button>
            </div>
          </template>
        </el-result>
      </div>

      <!-- 步骤3: NFT铸造 -->
      <div v-show="activeStep === 2">
        <el-result icon="success" title="版权存证成功">
          <template #extra>
            <el-descriptions :column="1" border class="desc-table">
              <el-descriptions-item label="证书编号" label-width="80">{{ copyrightResult?.certificateId }}</el-descriptions-item>
              <el-descriptions-item label="交易哈希" label-width="80">
                <span class="hash-text">{{ copyrightResult?.txHash }}</span>
                <el-button link type="primary" size="small" @click="copyText(copyrightResult?.txHash)">复制</el-button>
              </el-descriptions-item>
              <el-descriptions-item label="区块号" label-width="80">{{ copyrightResult?.blockNumber }}</el-descriptions-item>
              <el-descriptions-item label="存证时间" label-width="80">{{ copyrightResult?.registeredAt }}</el-descriptions-item>
            </el-descriptions>
            <div class="mt-20 step3-actions">
              <div>
                <span style="margin-right:12px;color:#606266">版税率:</span>
                <el-input-number v-model="royaltyRate" :min="0" :max="10000" :step="100" size="default" />
                <span style="margin-left:6px;color:#909399;font-size:12px">({{ (royaltyRate / 100).toFixed(1) }}%)</span>
              </div>
              <div style="margin-top:12px">
                <el-button type="warning" size="large" :loading="minting" @click="handleMintNFT">
                  铸造版权NFT
                </el-button>
                <el-button size="large" @click="handleSkipMint">跳过</el-button>
              </div>
            </div>
          </template>
        </el-result>
      </div>

      <!-- 步骤4: 完成 -->
      <div v-show="activeStep === 3">
        <el-result icon="success" title="全部完成！" sub-title="您的作品已成功上传、存证并铸造NFT">
          <template #extra>
            <el-button type="primary" @click="$router.push('/my-works')">查看我的作品</el-button>
            <el-button @click="$router.push('/my-nfts')">查看我的NFT</el-button>
            <el-button @click="$router.push('/')">返回首页</el-button>
          </template>
        </el-result>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { workAPI, copyrightAPI, nftAPI } from '@/api'
import { ElMessage } from 'element-plus'

const activeStep = ref(0)
const uploading = ref(false)
const registering = ref(false)
const minting = ref(false)

const uploadFormRef = ref(null)
const uploadRef = ref(null)
const selectedFile = ref(null)

const uploadForm = reactive({
  title: '',
  workType: 'IMAGE',
  description: '',
  promptInfo: ''
})

const uploadRules = {
  title: [{ required: true, message: '请输入作品标题', trigger: 'blur' }],
  workType: [{ required: true, message: '请选择作品类型', trigger: 'change' }]
}

const uploadResult = ref(null)
const copyrightResult = ref(null)
const nftResult = ref(null)
const royaltyRate = ref(500)

function handleFileChange(file) {
  // Element Plus upload: file.raw is the actual File object
  selectedFile.value = file.raw || file
}

function handleFileRemove() {
  selectedFile.value = null
}

function beforeUpload(file) {
  const maxSize = 50 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过50MB')
    return false
  }
  return true
}

async function handleUpload() {
  if (!uploadFormRef.value) {
    ElMessage.warning('页面尚未加载完成，请稍后重试')
    return
  }

  // 1. 先校验表单
  try {
    await uploadFormRef.value.validate()
  } catch {
    // 校验失败时 Element Plus 会自动提示
    return
  }

  // 2. 检查文件
  if (!selectedFile.value) {
    ElMessage.warning('请先选择要上传的作品文件')
    return
  }

  // 3. 上传
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('metadata', new Blob([JSON.stringify({
      title: uploadForm.title,
      workType: uploadForm.workType,
      description: uploadForm.description,
      promptInfo: uploadForm.promptInfo
    })], { type: 'application/json' }))
    formData.append('file', selectedFile.value)

    const res = await workAPI.upload(formData)
    if (res.data?.code === 200) {
      uploadResult.value = res.data.data
      activeStep.value = 1
      ElMessage.success('作品上传成功')
    } else {
      ElMessage.error(res.data?.message || '上传失败')
    }
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败，请检查文件格式或稍后重试')
  } finally {
    uploading.value = false
  }
}

async function handleRegisterCopyright() {
  if (!uploadResult.value?.workId) {
    ElMessage.warning('作品信息缺失，请重新上传')
    return
  }
  registering.value = true
  try {
    const res = await copyrightAPI.register({ workId: uploadResult.value.workId })
    if (res.data && res.data.code === 200) {
      copyrightResult.value = res.data.data
      activeStep.value = 2
      ElMessage.success('版权存证成功')
    } else {
      ElMessage.error(res.data?.message || '版权存证失败，请稍后重试')
    }
  } catch (error) {
    console.error('版权存证失败:', error)
    ElMessage.error('版权存证失败，请稍后重试')
  } finally {
    registering.value = false
  }
}

async function handleMintNFT() {
  if (!uploadResult.value?.workId) {
    ElMessage.warning('作品信息缺失，请重新上传')
    return
  }
  minting.value = true
  try {
    const res = await nftAPI.mint({
      workId: uploadResult.value.workId,
      royaltyRate: royaltyRate.value
    })
    if (res.data && res.data.code === 200) {
      nftResult.value = res.data.data
      activeStep.value = 3
      ElMessage.success('NFT铸造成功')
    } else {
      ElMessage.error(res.data?.message || 'NFT铸造失败，请稍后重试')
    }
  } catch (error) {
    console.error('NFT铸造失败:', error)
    ElMessage.error('NFT铸造失败，请稍后重试')
  } finally {
    minting.value = false
  }
}

function handleSkipMint() {
  activeStep.value = 3
}

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text || '')
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.info('手动复制: ' + text)
  }
}

function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(2) + ' ' + units[i]
}

function truncateHash(hash) {
  if (!hash) return ''
  return hash.substring(0, 10) + '...' + hash.substring(hash.length - 8)
}
</script>

<style scoped>
.upload-page {
  max-width: 900px;
  margin: 0 auto;
}
.mb-24 { margin-bottom: 24px; }
.mt-20 { margin-top: 20px; }

.desc-table {
  max-width: 600px;
  margin: 0 auto;
}

.desc-table :deep(.el-descriptions__label) {
  white-space: nowrap;
}

.hash-text {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 12px;
  color: #606266;
  word-break: break-all;
}

.step3-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
}
</style>
