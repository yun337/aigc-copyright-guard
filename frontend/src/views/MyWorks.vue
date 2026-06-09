<template>
  <div class="my-works-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <h2>我的作品</h2>
          <el-button type="primary" @click="$router.push('/upload')">
            <el-icon><Upload /></el-icon>上传作品
          </el-button>
        </div>
      </template>

      <el-table :data="works" stripe v-loading="loading" empty-text="还没有上传作品">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="作品标题" min-width="160">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/works/${row.id}`)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="workType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small">{{ row.workType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="copyrightStatus" label="版权状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.copyrightStatus === 'REGISTERED'" type="success" size="small">已存证</el-tag>
            <el-tag v-else type="info" size="small">未存证</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="90">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" width="160" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/works/${row.id}`)">详情</el-button>
            <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.copyrightStatus !== 'REGISTERED'"
              size="small" type="success" @click="handleRegister(row.id)">存证</el-button>
            <el-button size="small" type="warning" @click="handleMint(row.id)">NFT</el-button>
            <el-popconfirm
              title="确定删除该作品？"
              confirm-button-text="删除"
              cancel-button-text="取消"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button size="small" type="danger" :disabled="row.copyrightStatus === 'REGISTERED'"
                  :title="row.copyrightStatus === 'REGISTERED' ? '已存证作品不能删除' : ''">
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :total="total"
          :page-size="pageSize"
          layout="prev, pager, next, total"
          @current-change="loadWorks"
        />
      </div>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑作品" width="500px" :close-on-click-modal="false">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item label="作品标题" prop="title">
          <el-input v-model="editForm.title" placeholder="请输入作品标题" />
        </el-form-item>
        <el-form-item label="作品描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="描述作品..." />
        </el-form-item>
        <el-form-item label="AI Prompt">
          <el-input v-model="editForm.promptInfo" type="textarea" :rows="3" placeholder="生成Prompt..." />
        </el-form-item>
        <el-form-item label="作品类型" prop="workType">
          <el-select v-model="editForm.workType" style="width:100%">
            <el-option label="图片" value="IMAGE" />
            <el-option label="视频" value="VIDEO" />
            <el-option label="音乐" value="MUSIC" />
            <el-option label="文本" value="TEXT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { workAPI, copyrightAPI, nftAPI } from '@/api'
import { ElMessage } from 'element-plus'

const works = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 编辑相关
const editVisible = ref(false)
const saving = ref(false)
const editFormRef = ref(null)
const editingId = ref(null)

const editForm = reactive({
  title: '', description: '', promptInfo: '', workType: 'IMAGE'
})

const editRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  workType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

onMounted(() => loadWorks())

async function loadWorks() {
  loading.value = true
  try {
    const res = await workAPI.getMyWorks(currentPage.value - 1, pageSize.value)
    if (res.data?.code === 200) {
      works.value = res.data.data.content || []
      total.value = res.data.data.totalElements || 0
    }
  } catch (e) { console.error(e) } finally { loading.value = false }
}

// 打开编辑弹窗
function openEdit(row) {
  editingId.value = row.id
  editForm.title = row.title || ''
  editForm.description = row.description || ''
  editForm.promptInfo = row.promptInfo || ''
  editForm.workType = row.workType || 'IMAGE'
  editVisible.value = true
}

// 保存编辑
async function handleSave() {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const res = await workAPI.update(editingId.value, { ...editForm })
      if (res.data?.code === 200) {
        ElMessage.success('作品更新成功')
        editVisible.value = false
        loadWorks()
      }
    } catch (e) { /* handled */ } finally { saving.value = false }
  })
}

// 删除作品
async function handleDelete(workId) {
  try {
    const res = await workAPI.delete(workId)
    if (res.data?.code === 200) {
      ElMessage.success('作品已删除')
      loadWorks()
    }
  } catch (e) { /* handled */ }
}

async function handleRegister(workId) {
  try {
    await copyrightAPI.register({ workId })
    ElMessage.success('版权存证成功')
    loadWorks()
  } catch (e) { /* handled */ }
}

async function handleMint(workId) {
  try {
    await nftAPI.mint({ workId, royaltyRate: 500 })
    ElMessage.success('NFT铸造成功')
  } catch (e) { /* handled */ }
}

function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0, size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(2) + ' ' + units[i]
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
