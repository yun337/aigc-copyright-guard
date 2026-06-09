<template>
  <div class="auth-page">
    <el-card shadow="never">
      <template #header>
        <h2><el-icon><ShoppingCart /></el-icon> 版权授权管理</h2>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 我的授权（作为版权方） -->
        <el-tab-pane label="我发出的授权" name="grants">
          <el-table :data="grants" stripe v-loading="loadingGrants">
            <el-table-column prop="id" label="授权ID" width="80" />
            <el-table-column prop="work.title" label="作品" min-width="150" />
            <el-table-column prop="licensee.username" label="被授权人" width="120" />
            <el-table-column prop="licenseType" label="授权类型" width="110">
              <template #default="{ row }">
                <el-tag :type="getLicenseTypeColor(row.licenseType)" size="small">
                  {{ getLicenseTypeLabel(row.licenseType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="licenseFee" label="授权费用(ETH)" width="120" />
            <el-table-column prop="startDate" label="开始日期" width="110" />
            <el-table-column prop="endDate" label="结束日期" width="110" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.status === 'ACTIVE'" type="success" size="small">有效</el-tag>
                <el-tag v-else-if="row.status === 'EXPIRED'" type="info" size="small">已过期</el-tag>
                <el-tag v-else type="danger" size="small">已撤销</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 'ACTIVE'"
                  size="small" type="danger" @click="handleRevoke(row.id)">
                  撤销
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 获得的授权（作为被授权方） -->
        <el-tab-pane label="我获得的授权" name="licenses">
          <el-table :data="licenses" stripe v-loading="loadingLicenses">
            <el-table-column prop="id" label="授权ID" width="80" />
            <el-table-column prop="work.title" label="作品" min-width="150" />
            <el-table-column prop="licensor.username" label="授权人" width="120" />
            <el-table-column prop="licenseType" label="授权类型" width="110">
              <template #default="{ row }">
                <el-tag :type="getLicenseTypeColor(row.licenseType)" size="small">
                  {{ getLicenseTypeLabel(row.licenseType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="startDate" label="开始日期" width="110" />
            <el-table-column prop="endDate" label="结束日期" width="110" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.status === 'ACTIVE'" type="success" size="small">有效</el-tag>
                <el-tag v-else-if="row.status === 'EXPIRED'" type="warning" size="small">已过期</el-tag>
                <el-tag v-else type="danger" size="small">已撤销</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 新建授权 -->
        <el-tab-pane label="新建授权" name="create">
          <el-form ref="formRef" :model="authForm" :rules="rules" label-position="top" style="max-width:500px">
            <el-form-item label="版权记录ID" prop="copyrightId">
              <el-input-number v-model="authForm.copyrightId" :min="1" style="width:100%" />
            </el-form-item>
            <el-form-item label="被授权人ID" prop="licenseeId">
              <el-input-number v-model="authForm.licenseeId" :min="1" style="width:100%" />
            </el-form-item>
            <el-form-item label="授权类型" prop="licenseType">
              <el-select v-model="authForm.licenseType" style="width:100%">
                <el-option label="独家授权" value="EXCLUSIVE" />
                <el-option label="非独家授权" value="NON_EXCLUSIVE" />
                <el-option label="临时授权" value="TEMPORARY" />
              </el-select>
            </el-form-item>
            <el-form-item label="授权费用 (ETH)">
              <el-input-number v-model="authForm.licenseFee" :min="0" :precision="6" style="width:100%" />
            </el-form-item>
            <el-form-item label="授权天数 (0=永久)">
              <el-input-number v-model="authForm.durationDays" :min="0" :max="3650" style="width:100%" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="creating" @click="handleCreateAuth">
                创建授权
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { authorizationAPI } from '@/api'
import { ElMessage } from 'element-plus'

const activeTab = ref('grants')
const grants = ref([])
const licenses = ref([])
const loadingGrants = ref(false)
const loadingLicenses = ref(false)
const creating = ref(false)
const formRef = ref(null)

const authForm = reactive({
  copyrightId: null,
  licenseeId: null,
  licenseType: 'EXCLUSIVE',
  licenseFee: 0,
  durationDays: 365
})

const rules = {
  copyrightId: [{ required: true, message: '请输入版权记录ID', trigger: 'blur' }],
  licenseeId: [{ required: true, message: '请输入被授权人ID', trigger: 'blur' }],
  licenseType: [{ required: true, message: '请选择授权类型', trigger: 'change' }]
}

onMounted(() => {
  loadGrants()
  loadLicenses()
})

async function loadGrants() {
  loadingGrants.value = true
  try {
    const res = await authorizationAPI.getMyGrants()
    if (res.data && res.data.code === 200) {
      grants.value = res.data.data || []
    }
  } catch (e) { /* ignore */ } finally {
    loadingGrants.value = false
  }
}

async function loadLicenses() {
  loadingLicenses.value = true
  try {
    const res = await authorizationAPI.getMyLicenses()
    if (res.data && res.data.code === 200) {
      licenses.value = res.data.data || []
    }
  } catch (e) { /* ignore */ } finally {
    loadingLicenses.value = false
  }
}

async function handleCreateAuth() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    creating.value = true
    try {
      const res = await authorizationAPI.grant(authForm)
      if (res.data && res.data.code === 200) {
        ElMessage.success('授权创建成功')
        activeTab.value = 'grants'
        loadGrants()
      }
    } catch (e) { /* handled */ } finally {
      creating.value = false
    }
  })
}

async function handleRevoke(authId) {
  try {
    await authorizationAPI.revoke(authId)
    ElMessage.success('授权已撤销')
    loadGrants()
  } catch (e) { /* handled */ }
}

function getLicenseTypeLabel(type) {
  const map = { EXCLUSIVE: '独家', NON_EXCLUSIVE: '非独家', TEMPORARY: '临时' }
  return map[type] || type
}

function getLicenseTypeColor(type) {
  const map = { EXCLUSIVE: 'danger', NON_EXCLUSIVE: 'warning', TEMPORARY: 'info' }
  return map[type] || 'info'
}
</script>

<style scoped>
.auth-page {
  max-width: 1100px;
  margin: 0 auto;
}
</style>
