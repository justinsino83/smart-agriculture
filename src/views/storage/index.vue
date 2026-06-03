<template>
  <div class="storage-page">
    <div class="page-header">
      <div class="header-left">
        <h2>智慧仓储</h2>
        <el-tag type="info">总库存: {{ formatNumber(totalStock) }} 吨</el-tag>
      </div>

      <div class="header-actions">
        <el-button type="primary" @click="showInDialog = true">入库登记</el-button>
        <el-button @click="showOutDialog = true">出库登记</el-button>
      </div>
    </div>

    <div class="main-content">
      <!-- 库存概览 -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon yellow">
            <el-icon>
              <Box />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ formatNumber(totalStock) }}<span class="unit">吨</span></div>
            <div class="stat-label">当前库存</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon blue">
            <el-icon>
              <ArrowDown />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ formatNumber(todayIn) }}<span class="unit">吨</span></div>
            <div class="stat-label">今日入库</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon green">
            <el-icon>
              <ArrowUp />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ formatNumber(todayOut) }}<span class="unit">吨</span></div>
            <div class="stat-label">今日出库</div>
          </div>
        </div>

        <div class="stat-card" style="cursor: pointer;" @click="loadAlerts(); showAlertDialog = true">
          <div class="stat-icon red">
            <el-icon>
              <Warning />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ warningCount }}<span class="unit">条</span></div>
            <div class="stat-label">库存预警</div>
          </div>
        </div>
      </div>

      <!-- 库存列表 -->
      <div class="card">
        <div class="card-header">
          <h3>库存明细</h3>

          <div class="header-filters">
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
              end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 300px; margin-right: 10px;"
              @change="handlePageChange" />
            <el-input v-model="searchKeyword" placeholder="搜索品种/批次号" style="width: 200px" clearable>
              <template #prefix>
                <el-icon>
                  <Search />
                </el-icon>
              </template>
            </el-input>
          </div>
        </div>

        <div class="card-body">
          <el-table :data="filteredStockList" stripe style="width: 100%" height="calc(100vh - 400px)"
            v-loading="loading" :cell-style="{ padding: '10px 0' }"
            :header-cell-style="{ padding: '12px 0', background: '#fafafa', color: '#262626' }">
            <el-table-column prop="grainType" label="粮食品种" min-width="120" show-overflow-tooltip />
            <el-table-column prop="batchNo" label="批次号" min-width="160" show-overflow-tooltip />
            <el-table-column prop="warehouse" label="仓库位置" min-width="150" show-overflow-tooltip />

            <el-table-column prop="quantity" label="数量(吨)" min-width="120" align="right">
              <template #default="{ row }">
                <span :class="{ 'text-warning': row.quantity < 10 }" style="padding-right: 15px; font-weight: 500;">
                  {{ row.quantity }}
                </span>
              </template>
            </el-table-column>

            <el-table-column prop="moisture" label="含水率(%)" min-width="120" align="right">
              <template #default="{ row }">
                <span style="padding-right: 15px;">{{ row.moisture }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="quality" label="质量等级" min-width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.quality === '一等' ? 'success' : 'info'">{{ row.quality }}</el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="status" label="状态" min-width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === '在库' ? 'success' : 'info'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="entryDate" label="入库日期" min-width="160" />

            <el-table-column prop="exitDate" label="出库日期" min-width="160">
              <template #default="{ row }">
                {{ row.exitDate || '-' }}
              </template>
            </el-table-column>

            <el-table-column label="操作" width="280" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
                <el-button link type="primary" @click="viewTrace(row)">追溯</el-button>
                <el-button link type="danger" @click="handleOut(row)" v-if="row.status === '在库'">出库</el-button>
                <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total"
              :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="handlePageChange"
              @current-change="handlePageChange" />
          </div>
        </div>
      </div>
    </div>

    <!-- 入库弹窗 -->
    <el-dialog v-model="showInDialog" title="入库登记" width="600px">
      <el-form :model="inForm" label-width="100px">
        <el-form-item label="粮食品种">
          <el-input v-model="inForm.grainType" placeholder="请输入粮食品种（如：水稻、小麦等）" clearable />
        </el-form-item>

        <el-form-item label="入库数量">
          <el-input-number v-model="inForm.quantity" :min="1" style="width: 100%" />
          <span class="unit">吨</span>
        </el-form-item>

        <el-form-item label="仓库位置">
          <el-input v-model="inForm.warehouse" placeholder="请输入仓库位置（如：1号仓库-A区）" clearable />
        </el-form-item>

        <el-form-item label="含水率">
          <el-input-number v-model="inForm.moisture" :min="10" :max="20" :precision="1" style="width: 100%" />
          <span class="unit">%</span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showInDialog = false">取消</el-button>
        <el-button type="primary" @click="saveIn" :loading="savingIn">确认入库</el-button>
      </template>
    </el-dialog>

    <!-- 出库弹窗 -->
    <el-dialog v-model="showOutDialog" title="出库登记" width="600px">
      <el-form :model="outForm" label-width="100px">
        <el-form-item label="批次号">
          <el-input v-model="outForm.batchNo" placeholder="请输入出库批次号" clearable />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showOutDialog = false">取消</el-button>
        <el-button type="primary" @click="saveOut" :loading="savingOut">确认出库</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="showDetailDialog" title="库存详情" width="700px">
      <el-descriptions v-if="selectedItem" :column="2" border>
        <el-descriptions-item label="批次号">{{ selectedItem.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="粮食品种">{{ selectedItem.grainType }}</el-descriptions-item>
        <el-descriptions-item label="仓库位置">{{ selectedItem.warehouse }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ selectedItem.quantity }} 吨</el-descriptions-item>
        <el-descriptions-item label="含水率">{{ selectedItem.moisture }}%</el-descriptions-item>
        <el-descriptions-item label="质量等级">
          <el-tag :type="selectedItem.quality === '一等' ? 'success' : 'info'">{{ selectedItem.quality }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="selectedItem.status === '在库' ? 'success' : 'info'">{{ selectedItem.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="入库日期">{{ selectedItem.entryDate }}</el-descriptions-item>
        <el-descriptions-item label="出库日期" v-if="selectedItem.exitDate">
          {{ selectedItem.exitDate }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 预警弹窗 -->
    <el-dialog v-model="showAlertDialog" title="库存预警" width="850px" destroy-on-close class="alert-dialog">
      <el-table :data="alertList" stripe style="width: 100%"
        :header-cell-style="{ background: '#fff5f5', color: '#1f2d3d', fontWeight: '600' }">
        <el-table-column prop="batchNo" label="批次号" min-width="140" show-overflow-tooltip />

        <el-table-column prop="grainType" label="粮食品种" min-width="100" />

        <el-table-column prop="warehouse" label="仓库位置" min-width="130" show-overflow-tooltip />

        <el-table-column prop="quantity" label="数量(吨)" min-width="100" align="right">
          <template #default="{ row }">
            <span class="alert-quantity">{{ row.quantity }}</span>
          </template>
        </el-table-column>

        <el-table-column label="预警原因" min-width="160">
          <template #default="{ row }">
            <div class="reason-tags">
              <el-tag v-for="(reason, idx) in row.reasons" :key="idx" type="danger" effect="light" round size="small">
                {{ reason }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="处置建议" min-width="200">
          <template #default="{ row }">
            <div class="suggestion-container">
              <div v-for="(suggestion, idx) in row.suggestions" :key="idx" class="suggestion-item">
                <span class="dot"></span>
                <span class="text">{{ suggestion }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 追溯弹窗 -->
    <el-dialog v-model="showTraceDialog" title="库存追溯" width="700px">
      <div v-if="traceData.basicInfo">
        <h4>基本信息</h4>
        <el-descriptions :column="2" border style="margin-bottom: 20px;">
          <el-descriptions-item label="批次号">{{ traceData.basicInfo.batchNo }}</el-descriptions-item>
          <el-descriptions-item label="粮食品种">{{ traceData.basicInfo.grainType }}</el-descriptions-item>
          <el-descriptions-item label="仓库位置">{{ traceData.basicInfo.warehouse }}</el-descriptions-item>
          <el-descriptions-item label="数量">{{ traceData.basicInfo.quantity }} 吨</el-descriptions-item>
        </el-descriptions>

        <h4>追溯时间线</h4>
        <el-timeline>
          <el-timeline-item v-for="(item, index) in traceData.timeline" :key="index"
            :type="item.type === '入库' ? 'primary' : 'success'" :timestamp="formatTime(item.time)">
            {{ item.description }}
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, ArrowUp, Warning, Search, Box } from '@element-plus/icons-vue'
import { storageApi } from '@/api'

const searchKeyword = ref('')
const dateRange = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const showInDialog = ref(false)
const showOutDialog = ref(false)
const showDetailDialog = ref(false)
const showAlertDialog = ref(false)
const showTraceDialog = ref(false)
const selectedItem = ref(null)
const loading = ref(false)
const savingIn = ref(false)
const savingOut = ref(false)
const deleting = ref(false)

const totalStock = ref(0)
const todayIn = ref(0)
const todayOut = ref(0)
const warningCount = ref(0)

const stockList = ref([])
const alertList = ref([])
const traceData = ref({})

const inForm = ref({
  grainType: '',
  quantity: 10,
  warehouse: '',
  moisture: 13.5
})

const outForm = ref({
  batchNo: ''
})

// 将后端数据转换为前端需要的格式
const formatStockItem = (item) => {
  let qualityText = '一等';
  if (item.quality === 2) qualityText = '二等';
  else if (item.quality === 3) qualityText = '三等';
  else if (typeof item.quality === 'string') qualityText = item.quality;

  return {
    id: item.id,
    grainType: item.grainType || '未知',
    batchNo: item.batchNo || '',
    warehouse: item.warehouse || '未分配',
    quantity: item.quantity || item.weight || 0,
    moisture: item.moisture || 0,
    quality: qualityText,
    entryDate: item.entryDate ? item.entryDate.toString().split('T')[0] : '',
    exitDate: item.exitDate ? item.exitDate.toString().split('T')[0] : '',
    status: item.status === 1 ? '已出库' : '在库'
  };
}

const filteredStockList = computed(() => {
  // 关键词搜索改为后端处理
  let result = stockList.value;

  // 按日期范围筛选（前端处理）
  if (dateRange.value && dateRange.value.length === 2) {
    const [startDate, endDate] = dateRange.value;
    result = result.filter(item => {
      if (!item.entryDate) return false;
      return item.entryDate >= startDate && item.entryDate <= endDate;
    })
  }

  return result;
})

const viewDetail = (row) => {
  selectedItem.value = row
  showDetailDialog.value = true
}

const viewTrace = async (row) => {
  try {
    const data = await storageApi.getTrace(row.id)
    traceData.value = data || {}
    showTraceDialog.value = true
  } catch (error) {
    console.error('获取追溯信息失败:', error)
    ElMessage.error('获取追溯信息失败')
  }
}

const handleOut = (row) => {
  outForm.value.batchNo = row.batchNo
  showOutDialog.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除批次号为「${row.batchNo}」的库存记录吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    deleting.value = true
    await storageApi.deleteStock(row.id)
    ElMessage.success('删除成功')
    loadStorageData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    deleting.value = false
  }
}

const saveIn = async () => {
  if (!inForm.value.grainType) {
    ElMessage.warning('请输入粮食品种')
    return
  }
  if (!inForm.value.warehouse) {
    ElMessage.warning('请输入仓库位置')
    return
  }

  savingIn.value = true
  try {
    await storageApi.stockIn(inForm.value)
    ElMessage.success('入库登记成功')
    showInDialog.value = false
    inForm.value = {
      grainType: '',
      quantity: 10,
      warehouse: '',
      moisture: 13.5
    }
    loadStorageData()
  } catch (error) {
    console.error('入库失败:', error)
    ElMessage.error(error.message || '入库失败')
  } finally {
    savingIn.value = false
  }
}

const saveOut = async () => {
  if (!outForm.value.batchNo) {
    ElMessage.warning('请输入批次号')
    return
  }

  savingOut.value = true
  try {
    await storageApi.stockOut(outForm.value)
    ElMessage.success('出库登记成功')
    showOutDialog.value = false
    outForm.value.batchNo = ''
    loadStorageData()
  } catch (error) {
    console.error('出库失败:', error)
    ElMessage.error(error.message || '出库失败')
  } finally {
    savingOut.value = false
  }
}

const loadAlerts = async () => {
  try {
    const data = await storageApi.getAlerts()
    alertList.value = data || []
  } catch (error) {
    console.error('加载预警信息失败:', error)
  }
}

const handlePageChange = () => {
  loadStorageData()
}

const formatNumber = (num) => {
  if (!num) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(2) + '万'
  }
  return num.toLocaleString()
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString()
}

const loadStorageData = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    const [stockData, overviewData] = await Promise.all([
      storageApi.getStockList(params),
      storageApi.getOverview()
    ])

    if (stockData) {
      const data = stockData.list || stockData.records || []
      stockList.value = data.map(formatStockItem)
      total.value = stockData.total || stockList.value.length
    }

    if (overviewData) {
      totalStock.value = overviewData.totalStock || 0
      todayIn.value = overviewData.todayIn || 0
      todayOut.value = overviewData.todayOut || 0
      warningCount.value = overviewData.warningCount || 0
    }
  } catch (error) {
    console.error('加载仓储数据失败:', error)
    ElMessage.error('加载仓储数据失败，请检查后端服务')
  } finally {
    loading.value = false
  }
}

// 监听搜索关键词变化，重新加载数据
watch(searchKeyword, () => {
  currentPage.value = 1
  loadStorageData()
})

// 监听日期范围变化
watch(dateRange, () => {
  currentPage.value = 1
  // 日期筛选在前端处理，不需要重新请求后端
})

onMounted(() => {
  loadStorageData()
})
</script>

<style scoped>
.storage-page {
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left h2 {
  margin: 0;
}

.header-filters {
  display: flex;
  align-items: center;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.stat-icon.yellow {
  background: #faad14;
}

.stat-icon.blue {
  background: #1890ff;
}

.stat-icon.green {
  background: #52c41a;
}

.stat-icon.red {
  background: #f5222d;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #262626;
}

.stat-value .unit {
  font-size: 14px;
  font-weight: 400;
  color: #8c8c8c;
  margin-left: 4px;
}

.stat-label {
  font-size: 14px;
  color: #8c8c8c;
  margin-top: 4px;
}

.card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.card-body {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
}

.text-warning {
  color: #faad14;
}

.unit {
  margin-left: 8px;
  color: #8c8c8c;
}

h4 {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #262626;
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* 预警弹窗专属优化 */
:deep(.alert-dialog) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.alert-dialog .el-dialog__header) {
  margin-right: 0;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 16px;
}

.alert-quantity {
  font-weight: 600;
  color: #ff4d4f;
  padding-right: 10px;
}

.reason-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.suggestion-container {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.suggestion-item {
  display: flex;
  align-items: flex-start;
  font-size: 13px;
  line-height: 1.4;
  color: #595959;
}

.suggestion-item .dot {
  width: 5px;
  height: 5px;
  background-color: #ff4d4f;
  border-radius: 50%;
  margin-top: 6px;
  margin-right: 8px;
  flex-shrink: 0;
}

.suggestion-item .text {
  flex: 1;
  word-break: break-all;
}
</style>
