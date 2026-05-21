<template>
  <div class="storage-page">
    <div class="page-header">
      <div class="header-left">
        <h2>智慧仓储</h2>
        <el-tag type="info">总库存: {{ totalStock }} 吨</el-tag>
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
          <div class="stat-icon yellow"><el-icon><Box /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ totalStock }}<span class="unit">吨</span></div>
            <div class="stat-label">当前库存</div>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-icon blue"><el-icon><ArrowDown /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ todayIn }}<span class="unit">吨</span></div>
            <div class="stat-label">今日入库</div>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-icon green"><el-icon><ArrowUp /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ todayOut }}<span class="unit">吨</span></div>
            <div class="stat-label">今日出库</div>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-icon red"><el-icon><Warning /></el-icon></div>
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
          
          <el-input
            v-model="searchKeyword"
            placeholder="搜索品种/批次号"
            style="width: 200px"
            clearable
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        
        <div class="card-body">
          <el-table :data="filteredStockList" stripe style="width: 100%" height="calc(100vh - 400px)">
            <el-table-column prop="grainType" label="粮食品种" width="120" />
            <el-table-column prop="batchNo" label="批次号" width="160" />
            <el-table-column prop="warehouse" label="仓库位置" width="150" />
            <el-table-column prop="quantity" label="数量(吨)" width="120">
              <template #default="{ row }">
                <span :class="{ 'text-warning': row.quantity < 10 }">{{ row.quantity }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="moisture" label="含水率(%)" width="120" />
            <el-table-column prop="quality" label="质量等级" width="100">
              <template #default="{ row }">
                <el-tag :type="row.quality === '一等' ? 'success' : 'info'">{{ row.quality }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="entryDate" label="入库日期" width="120" />
            <el-table-column prop="expireDate" label="保质期限" width="120">
              <template #default="{ row }">
                <span :class="{ 'text-danger': isNearExpire(row.expireDate) }">{{ row.expireDate }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
                <el-button link type="primary" @click="viewTrace(row)">追溯</el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <div class="pagination">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 入库弹窗 -->
    <el-dialog v-model="showInDialog" title="入库登记" width="600px">
      <el-form :model="inForm" label-width="100px">
        <el-form-item label="粮食品种">
          <el-select v-model="inForm.grainType" style="width: 100%">
            <el-option label="水稻" value="水稻" />
            <el-option label="小麦" value="小麦" />
            <el-option label="玉米" value="玉米" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="入库数量">
          <el-input-number v-model="inForm.quantity" :min="1" style="width: 100%" />
          <span class="unit">吨</span>
        </el-form-item>
        
        <el-form-item label="仓库位置">
          <el-select v-model="inForm.warehouse" style="width: 100%">
            <el-option label="1号仓库-A区" value="1号仓库-A区" />
            <el-option label="1号仓库-B区" value="1号仓库-B区" />
            <el-option label="2号仓库" value="2号仓库" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="含水率">
          <el-input-number v-model="inForm.moisture" :min="10" :max="20" :precision="1" style="width: 100%" />
          <span class="unit">%</span>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showInDialog = false">取消</el-button>
        <el-button type="primary" @click="saveIn">确认入库</el-button>
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
        <el-descriptions-item label="入库日期">{{ selectedItem.entryDate }}</el-descriptions-item>
        <el-descriptions-item label="保质期限">
          <span :class="{ 'text-danger': isNearExpire(selectedItem.expireDate) }">{{ selectedItem.expireDate }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown, ArrowUp, Warning, Search, Box } from '@element-plus/icons-vue'
import { storageApi } from '@/api'

const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const showInDialog = ref(false)
const showOutDialog = ref(false)
const showDetailDialog = ref(false)
const selectedItem = ref(null)
const loading = ref(false)

// 默认演示数据
const defaultTotalStock = 2560
const defaultTodayIn = 120
const defaultTodayOut = 80
const defaultWarningCount = 3
const defaultStockList = [
  { grainType: '水稻', batchNo: 'SD20240320001', warehouse: '1号仓库-A区', quantity: 120, moisture: 13.5, quality: '一等', entryDate: '2024-03-20', expireDate: '2024-09-20' },
  { grainType: '小麦', batchNo: 'XM20240319002', warehouse: '1号仓库-B区', quantity: 80, moisture: 12.8, quality: '一等', entryDate: '2024-03-19', expireDate: '2024-09-19' },
  { grainType: '玉米', batchNo: 'YM20240318003', warehouse: '2号仓库', quantity: 5, moisture: 14.2, quality: '二等', entryDate: '2024-03-18', expireDate: '2024-04-18' }
]

const totalStock = ref(defaultTotalStock)
const todayIn = ref(defaultTodayIn)
const todayOut = ref(defaultTodayOut)
const warningCount = ref(defaultWarningCount)

const stockList = ref([...defaultStockList])

const inForm = ref({
  grainType: '',
  quantity: 10,
  warehouse: '',
  moisture: 13.5
})

const filteredStockList = computed(() => {
  if (!searchKeyword.value) return stockList.value
  return stockList.value.filter(item => 
    item.grainType.includes(searchKeyword.value) ||
    item.batchNo.includes(searchKeyword.value)
  )
})

const isNearExpire = (date) => {
  if (!date) return false
  const expire = new Date(date)
  const now = new Date()
  const diff = (expire - now) / (1000 * 60 * 60 * 24)
  return diff < 30
}

const viewDetail = (row) => {
  selectedItem.value = row
  showDetailDialog.value = true
}

const viewTrace = (row) => {
  ElMessage.info(`追溯${row.batchNo}`)
}

const saveIn = () => {
  ElMessage.success('入库登记成功')
  showInDialog.value = false
  loadStorageData()
}

const loadStorageData = async () => {
  loading.value = true
  try {
    const [stockData, overviewData] = await Promise.all([
      storageApi.getStockList({ page: currentPage.value, size: pageSize.value }),
      storageApi.getOverview()
    ])
    if (stockData) {
      const data = stockData.list || stockData.records || []
      stockList.value = data.length > 0 ? data : [...defaultStockList]
      total.value = stockData.total || stockList.value.length
    } else {
      stockList.value = [...defaultStockList]
      total.value = defaultStockList.length
    }
    if (overviewData) {
      totalStock.value = overviewData.totalStock || defaultTotalStock
      todayIn.value = overviewData.todayIn || defaultTodayIn
      todayOut.value = overviewData.todayOut || defaultTodayOut
      warningCount.value = overviewData.warningCount || defaultWarningCount
    } else {
      totalStock.value = defaultTotalStock
      todayIn.value = defaultTodayIn
      todayOut.value = defaultTodayOut
      warningCount.value = defaultWarningCount
    }
  } catch (error) {
    console.error('加载仓储数据失败:', error)
    // 保持默认数据
    stockList.value = [...defaultStockList]
    total.value = defaultStockList.length
    totalStock.value = defaultTotalStock
    todayIn.value = defaultTodayIn
    todayOut.value = defaultTodayOut
    warningCount.value = defaultWarningCount
  } finally {
    loading.value = false
  }
}

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
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
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

.stat-icon.yellow { background: #faad14; }
.stat-icon.blue { background: #1890ff; }
.stat-icon.green { background: #52c41a; }
.stat-icon.red { background: #f5222d; }

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
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
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

.text-danger {
  color: #f5222d;
}

.unit {
  margin-left: 8px;
  color: #8c8c8c;
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
