<template>
  <div class="detection-page">
    <el-card shadow="never">
      <template #header>
        <h2><el-icon><Search /></el-icon> AI 侵权检测</h2>
      </template>

      <el-alert type="info" :closable="false" show-icon class="mb-20">
        <template #title>
          选择你的作品，系统会将其与<b>平台所有已上传作品</b>逐一比对，检测是否存在高度相似的内容
        </template>
      </el-alert>

      <el-form :inline="true">
        <el-form-item label="选择你的作品">
          <el-select v-model="selectedWorkId" placeholder="选择要检测的作品" filterable style="width: 340px">
            <el-option v-for="w in myWorks" :key="w.id" :label="`#${w.id} ${w.title}`" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="相似度阈值">
          <el-input-number v-model="threshold" :min="0" :max="1" :step="0.05" :precision="2" />
          <span class="tip-text">超过此值标记为疑似侵权</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleDetect" :loading="detecting" size="large">
            <el-icon><Search /></el-icon>与全平台作品比对
          </el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <!-- 检测中 -->
      <div v-if="detecting" class="detecting-box">
        <el-icon :size="32" class="rotating"><Loading /></el-icon>
        <p>AI模型正在提取语义特征并比对中...</p>
      </div>

      <!-- 检测结果 -->
      <div v-if="detectionResult" class="result-section">
        <el-alert
          :title="`共比对 ${detectionResult.totalCompared} 件平台作品，发现 ${detectionResult.similarCount} 件疑似侵权`"
          :type="detectionResult.similarCount > 0 ? 'warning' : 'success'"
          show-icon :closable="false" class="mb-20"
        />

        <el-table :data="detectionResult.similarWorks" stripe v-if="detectionResult.similarWorks?.length > 0" size="large">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="title" label="疑似侵权作品" min-width="200" />
          <el-table-column prop="creator" label="上传者" width="120" />
          <el-table-column label="相似度" width="200">
            <template #default="{ row }">
              <el-progress :percentage="row.similarity" :color="simColor(row.similarity)" :stroke-width="20" striped striped-flow />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="$router.push(`/works/${row.workId}`)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-result v-else icon="success" title="未发现疑似侵权" sub-title="你的作品在整个平台中是独一无二的" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { workAPI, detectionAPI } from '@/api'
import { ElMessage } from 'element-plus'

const myWorks = ref([])
const selectedWorkId = ref(null)
const threshold = ref(0.85)
const detecting = ref(false)
const detectionResult = ref(null)

onMounted(async () => {
  try {
    const res = await workAPI.getMyWorks(0, 100)
    if (res.data?.code === 200) {
      myWorks.value = res.data.data.content || []
      if (myWorks.value.length) selectedWorkId.value = myWorks.value[0].id
    }
  } catch (_) {}
})

async function handleDetect() {
  if (!selectedWorkId.value) { ElMessage.warning('请选择作品'); return }
  detecting.value = true
  detectionResult.value = null
  try {
    const res = await detectionAPI.check({
      workId: selectedWorkId.value,
      threshold: threshold.value,
      maxResults: 20
    })
    if (res.data?.code === 200) {
      detectionResult.value = res.data.data
    } else {
      ElMessage.error(res.data?.message || '检测失败，请稍后重试')
    }
  } catch (e) {
    console.error('检测失败:', e)
    ElMessage.error('检测失败，请确认AI服务已启动')
  } finally {
    detecting.value = false
  }
}

function simColor(v) {
  if (v >= 90) return '#F56C6C'
  if (v >= 80) return '#E6A23C'
  return '#409EFF'
}
</script>

<style scoped>
.detection-page { max-width: 1000px; margin: 0 auto; }
.mb-20 { margin-bottom: 20px; }
.tip-text { margin-left: 8px; font-size: 12px; color: #909399; }
.detecting-box { text-align: center; padding: 40px; color: #909399; }
.rotating { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
</style>
