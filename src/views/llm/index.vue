<template>
  <div class="llm-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2>LLM 模型配置</h2>
        <el-tag type="info">设备预警智能分析 - 多模型切换</el-tag>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="openAddDialog">
          <el-icon><Plus /></el-icon>新增模型
        </el-button>
        <el-button @click="loadData">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </div>

    <!-- 筛选条件 -->
    <div class="card">
      <div class="filter-row">
        <el-input v-model="searchKeyword" placeholder="搜索模型Key/名称" style="width: 240px" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filterProvider" placeholder="全部服务商" clearable style="width: 160px">
          <el-option v-for="p in providerOptions" :key="p.value" :label="p.label" :value="p.value" />
        </el-select>
        <el-button type="primary" @click="currentPage = 1; loadData()">搜索</el-button>
      </div>
    </div>

    <!-- 列表 -->
    <div class="card">
      <div class="card-header">
        <h3>模型列表</h3>
        <div class="stats">
          共 <b>{{ total }}</b> 个模型
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center">
          <template #default="scope">{{ (currentPage - 1) * pageSize + scope.$index + 1 }}</template>
        </el-table-column>

        <el-table-column prop="modelKey" label="模型Key" min-width="180">
          <template #default="{ row }">
            <el-tag type="primary" effect="plain">{{ row.modelKey }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="label" label="显示名称" min-width="200" />

        <el-table-column label="服务商" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="providerTagType(row.provider)" effect="light">
              {{ providerLabel(row.provider) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="ApiKey" min-width="260">
          <template #default="{ row }">
            <span v-if="row.apiKey">{{ maskApiKey(row.apiKey) }}</span>
            <span v-else class="text-muted">未配置</span>
          </template>
        </el-table-column>

        <el-table-column prop="baseUrl" label="BaseUrl" min-width="300" show-overflow-tooltip />

        <el-table-column prop="modelId" label="ModelId" min-width="180" show-overflow-tooltip />

        <el-table-column label="可用状态" width="110" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.available" :active-value="1" :inactive-value="0" @change="() => handleToggle(row)" />
          </template>
        </el-table-column>

        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />

        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="success" link @click="viewDetail(row)">详情</el-button>
            <el-popconfirm title="确认删除该模型？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button size="small" type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange" />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="showFormDialog" :title="formMode === 'add' ? '新增模型' : '编辑模型'" width="720px" destroy-on-close>
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="110px">
        <el-form-item label="模型Key" prop="modelKey">
          <el-input v-model="formData.modelKey" placeholder="例: deepseek-chat" maxlength="100" />
        </el-form-item>

        <el-form-item label="显示名称" prop="label">
          <el-input v-model="formData.label" placeholder="例: DeepSeek（深度求索）" maxlength="200" />
        </el-form-item>

        <el-form-item label="服务商" prop="provider">
          <el-select v-model="formData.provider" placeholder="请选择服务商" style="width: 100%">
            <el-option v-for="p in providerOptions" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="formData.apiKey" type="password" show-password placeholder="模型服务商分配的 API Key" maxlength="500" />
        </el-form-item>

        <el-form-item label="BaseUrl">
          <el-input v-model="formData.baseUrl" placeholder="https://api.deepseek.com（可留空，前端会用默认值）" maxlength="500" />
        </el-form-item>

        <el-form-item label="ModelId">
          <el-input v-model="formData.modelId" placeholder="调用 /chat/completions 时的 model 参数" maxlength="200" />
        </el-form-item>

        <el-form-item label="可用状态">
          <el-switch v-model="formData.available" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>

        <el-form-item label="排序">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="9999" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showFormDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="showDetailDialog" title="模型详情" width="680px">
      <el-descriptions v-if="detailRow" :column="2" border>
        <el-descriptions-item label="主键ID">{{ detailRow.id }}</el-descriptions-item>
        <el-descriptions-item label="模型Key">{{ detailRow.modelKey }}</el-descriptions-item>
        <el-descriptions-item label="显示名称" :span="2">{{ detailRow.label }}</el-descriptions-item>
        <el-descriptions-item label="服务商">{{ providerLabel(detailRow.provider) }}</el-descriptions-item>
        <el-descriptions-item label="可用状态">
          <el-tag :type="detailRow.available === 1 ? 'success' : 'danger'">
            {{ detailRow.available === 1 ? '启用' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="API Key" :span="2">{{ detailRow.apiKey }}</el-descriptions-item>
        <el-descriptions-item label="BaseUrl" :span="2">{{ detailRow.baseUrl || '（留空，使用默认）' }}</el-descriptions-item>
        <el-descriptions-item label="ModelId" :span="2">{{ detailRow.modelId || '（留空，使用默认）' }}</el-descriptions-item>
        <el-descriptions-item label="排序">{{ detailRow.sortOrder }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(detailRow.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间" :span="2">{{ formatTime(detailRow.updateTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="detailRow.remark" label="备注" :span="2">{{ detailRow.remark }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { llmApi } from '@/api/modules'

const providerOptions = [
  { value: 'deepseek', label: 'DeepSeek' },
  { value: 'doubao', label: '火山方舟 · 豆包' },
  { value: 'volc-ark', label: '火山方舟（别名）' },
  { value: 'ark', label: '火山方舟（缩写）' }
]
const providerLabel = (p) => providerOptions.find((it) => it.value === p)?.label || p
const providerTagType = (p) => {
  if (p === 'deepseek') return 'primary'
  if (p === 'doubao' || p === 'volc-ark' || p === 'ark') return 'success'
  return 'info'
}
const maskApiKey = (key) => {
  if (!key) return ''
  if (key.length <= 8) return '****' + key.slice(-4)
  return key.slice(0, 4) + '****' + key.slice(-4)
}
const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return t
  return d.toLocaleString()
}

const searchKeyword = ref('')
const filterProvider = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const loading = ref(false)
const saving = ref(false)

const showFormDialog = ref(false)
const formMode = ref('add')
const formRef = ref(null)
const formData = ref({})
const formRules = {
  modelKey: [{ required: true, message: '请输入模型Key', trigger: 'blur' }],
  label: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
  provider: [{ required: true, message: '请选择服务商', trigger: 'change' }],
  apiKey: [{ required: true, message: '请输入 API Key', trigger: 'blur' }]
}

const showDetailDialog = ref(false)
const detailRow = ref(null)

const resetForm = () => {
  formData.value = {
    modelKey: '',
    label: '',
    provider: '',
    apiKey: '',
    baseUrl: '',
    modelId: '',
    available: 1,
    sortOrder: 0,
    remark: ''
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterProvider.value) params.provider = filterProvider.value
    const data = await llmApi.listModels(params)
    tableData.value = data?.list || []
    total.value = data?.total || 0
  } catch (e) {
    console.error(e)
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const openAddDialog = () => {
  formMode.value = 'add'
  resetForm()
  showFormDialog.value = true
}

const openEditDialog = async (row) => {
  formMode.value = 'edit'
  resetForm()
  try {
    const data = await llmApi.getModelDetail(row.id)
    Object.assign(formData.value, {
      id: data.id,
      modelKey: data.modelKey,
      label: data.label,
      provider: data.provider,
      apiKey: data.apiKey,
      baseUrl: data.baseUrl,
      modelId: data.modelId,
      available: data.available != null ? data.available : 1,
      sortOrder: data.sortOrder != null ? data.sortOrder : 0,
      remark: data.remark
    })
    showFormDialog.value = true
  } catch (e) {
    console.error(e)
    ElMessage.error(e?.message || '获取详情失败')
  }
}

const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const data = { ...formData.value }
    if (formMode.value === 'add') {
      await llmApi.addModel(data)
      ElMessage.success('新增成功')
    } else {
      await llmApi.updateModel(data.id, data)
      ElMessage.success('更新成功')
    }
    showFormDialog.value = false
    loadData()
  } catch (e) {
    console.error(e)
    ElMessage.error(e?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

const handleToggle = async (row) => {
  try {
    await llmApi.toggleAvailable(row.id)
    ElMessage.success('状态已切换')
  } catch (e) {
    // 回滚开关状态
    row.available = row.available === 1 ? 0 : 1
    console.error(e)
    ElMessage.error(e?.message || '切换失败')
  }
}

const handleDelete = async (row) => {
  try {
    await llmApi.deleteModel(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    console.error(e)
    ElMessage.error(e?.message || '删除失败')
  }
}

const viewDetail = async (row) => {
  detailRow.value = row
  showDetailDialog.value = true
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadData()
}
const handlePageChange = (page) => {
  currentPage.value = page
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.llm-page { padding: 16px; }
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
}
.page-header h2 { margin: 0 0 4px; font-size: 20px; }
.card {
  background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.card-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 12px;
}
.card-header h3 { margin: 0; font-size: 16px; }
.filter-row { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
.text-muted { color: #909399; }
</style>
