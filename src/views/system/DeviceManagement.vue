<template>
  <div class="device-page">
    <div class="page-header">
      <div class="header-left">
        <h2>设备管理</h2>
        <el-tag type="info">共 {{ deviceList.length }} 台设备</el-tag>
      </div>
      <div class="header-actions">
        <el-button type="primary">
          <el-icon><Plus /></el-icon> 添加设备
        </el-button>
      </div>
    </div>

    <div class="main-content">
      <!-- 设备概览 -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon green">
            <el-icon><VideoCamera /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ overviewData.onlineDevices }}/{{ overviewData.totalDevices }}</div>
            <div class="stat-label">在线设备</div>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-icon red">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ overviewData.systemAlerts }}</div>
            <div class="stat-label">系统告警</div>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-icon yellow">
            <el-icon><Timer /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ overviewData.uptime }}d</div>
            <div class="stat-label">运行时长</div>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-icon blue">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ overviewData.availability }}%</div>
            <div class="stat-label">服务可用性</div>
          </div>
        </div>
      </div>

      <div class="main-grid">
        <!-- 设备列表 -->
        <div class="card">
          <div class="card-header">
            <h3>设备列表</h3>
            <div class="header-actions">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索设备名称"
                style="width: 220px"
                clearable
              >
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
            </div>
          </div>

          <div class="card-body">
            <el-table :data="filteredDeviceList" stripe style="width: 100%" height="calc(100vh - 400px)">
              <el-table-column type="index" label="#" width="60" align="center" />
              <el-table-column prop="name" label="设备名称" min-width="180" />
              <el-table-column prop="type" label="类型" width="120">
                <template #default="{ row }">
                  <el-tag type="info" size="small">{{ row.type }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="location" label="位置" min-width="150" />
              <el-table-column prop="status" label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === '在线' ? 'success' : 'danger'" size="small">
                    {{ row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="lastOnline" label="最后在线" width="180" />
              <el-table-column label="操作" width="150" fixed="right" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="viewDevice(row)">查看</el-button>
                  <el-button link type="warning" size="small" @click="restartDevice(row)">重启</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <!-- 系统日志 -->
        <div class="card">
          <div class="card-header">
            <h3>系统日志</h3>
            <el-button link type="primary">查看全部</el-button>
          </div>

          <div class="card-body">
            <div class="log-list">
              <div v-for="log in systemLogs" :key="log.id" class="log-item" :class="log.level">
                <div class="log-time">{{ log.time }}</div>
                <div class="log-content">
                  <span class="log-tag">[{{ log.module }}]</span>
                  <span class="log-message">{{ log.message }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 设备详情弹窗 -->
    <el-dialog v-model="showDeviceDetail" title="设备详情" width="700px">
      <el-descriptions v-if="selectedDevice" :column="2" border>
        <el-descriptions-item label="设备名称">{{ selectedDevice.name }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">{{ selectedDevice.type }}</el-descriptions-item>
        <el-descriptions-item label="安装位置">{{ selectedDevice.location }}</el-descriptions-item>
        <el-descriptions-item label="设备状态">
          <el-tag :type="selectedDevice.status === '在线' ? 'success' : 'danger'">
            {{ selectedDevice.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最后在线">{{ selectedDevice.lastOnline }}</el-descriptions-item>
        <el-descriptions-item label="设备ID">DEV-{{ String(selectedDevice.id).padStart(4, '0') }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { VideoCamera, Warning, Timer, Plus, Search, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { systemApi, facilityApi } from '@/api'

const searchKeyword = ref('')
const showDeviceDetail = ref(false)
const selectedDevice = ref(null)
const loading = ref(false)

// 设施类型映射
const typeMap = {
  1: '试验田',
  2: '仓库',
  3: '烘干车间'
}

// 设施状态映射
const statusMap = {
  0: '离线',
  1: '在线'
}

const defaultSystemLogs = [
  { id: 1, time: '10:30:15', module: '设备', level: 'info', message: '设施数据上报正常' },
  { id: 2, time: '10:28:42', module: '系统', level: 'success', message: '数据同步完成' },
  { id: 3, time: '10:25:08', module: '监控', level: 'info', message: '温度达到设定值' },
  { id: 4, time: '10:20:33', module: '系统', level: 'warning', message: '连接超时' },
  { id: 5, time: '10:15:20', module: '用户', level: 'info', message: '管理员 admin 登录系统' }
]

const systemLogs = ref([...defaultSystemLogs])
const deviceList = ref([])

// 格式化设施数据为设备列表格式
const formatFacility = (facility) => ({
  id: facility.id,
  name: facility.name,
  type: typeMap[facility.type] || '未知',
  location: facility.locationName || facility.location || '-',
  status: statusMap[facility.status] || '未知',
  lastOnline: facility.updateTime ? facility.updateTime.substring(0, 19) : '-'
})

// 获取设施列表
const loadFacilities = async () => {
  loading.value = true
  try {
    const result = await facilityApi.getAll()
    if (result && Array.isArray(result)) {
      deviceList.value = result.map(formatFacility)
    } else {
      deviceList.value = []
    }
  } catch (error) {
    console.error('加载设施数据失败', error)
    deviceList.value = []
  } finally {
    loading.value = false
  }
}

// 计算属性：根据实际列表计算统计数据
const overviewData = computed(() => ({
  onlineDevices: deviceList.value.filter(d => d.status === '在线').length,
  totalDevices: deviceList.value.length,
  systemAlerts: deviceList.value.filter(d => d.status === '离线').length,
  uptime: 15,
  availability: deviceList.value.length > 0 
    ? Math.round((deviceList.value.filter(d => d.status === '在线').length / deviceList.value.length) * 1000) / 10
    : 99.9
}))

const filteredDeviceList = computed(() => {
  if (!searchKeyword.value) return deviceList.value
  const keyword = searchKeyword.value.toLowerCase()
  return deviceList.value.filter(item => 
    item.name.toLowerCase().includes(keyword)
  )
})

const viewDevice = (row) => {
  selectedDevice.value = row
  showDeviceDetail.value = true
}

const restartDevice = (row) => {
  ElMessage.success(`设备 ${row.name} 重启指令已发送`)
}

const loadSystemData = async () => {
  await Promise.all([
    loadFacilities(),
    // 暂时保持日志使用示例数据
  ])
}

onMounted(() => {
  loadSystemData()
})
</script>

<style scoped>
.device-page {
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
  font-size: 20px;
  color: #262626;
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

.stat-icon.blue { background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%); }
.stat-icon.green { background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%); }
.stat-icon.yellow { background: linear-gradient(135deg, #faad14 0%, #ffc53d 100%); }
.stat-icon.red { background: linear-gradient(135deg, #f5222d 0%, #ff4d4f 100%); }

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #262626;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #8c8c8c;
  margin-top: 4px;
}

.main-grid {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
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
  color: #262626;
}

.card-body {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 日志列表 */
.log-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 300px;
  overflow-y: auto;
  flex: 1;
}

.log-item {
  display: flex;
  gap: 12px;
  padding: 10px 12px;
  background: #f6ffed;
  border-radius: 6px;
  font-size: 13px;
}

.log-item.warning {
  background: #fffbe6;
}

.log-item.danger {
  background: #fff1f0;
}

.log-item.info {
  background: #f0f5ff;
}

.log-time {
  color: #8c8c8c;
  font-family: monospace;
  flex-shrink: 0;
}

.log-tag {
  color: #1890ff;
  font-weight: 500;
  margin-right: 8px;
}

.log-message {
  color: #262626;
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .main-grid {
    grid-template-columns: 1fr;
  }
}
</style>
