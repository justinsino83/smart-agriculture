<template>
  <div class="system-page">
    <!-- 系统概览 -->
    <div class="overview-grid">
      <div class="overview-card">
        <div class="overview-icon green"><el-icon><VideoCamera /></el-icon></div>
        <div class="overview-info">
          <div class="overview-value">{{ overviewData.onlineDevices }}/{{ overviewData.totalDevices }}</div>
          <div class="overview-label">在线设备</div>
        </div>
      </div>

      <div class="overview-card">
        <div class="overview-icon blue"><el-icon><Warning /></el-icon></div>
        <div class="overview-info">
          <div class="overview-value">{{ overviewData.systemAlerts }}</div>
          <div class="overview-label">系统告警</div>
        </div>
      </div>

      <div class="overview-card">
        <div class="overview-icon orange"><el-icon><Timer /></el-icon></div>
        <div class="overview-info">
          <div class="overview-value">{{ overviewData.uptime }}d</div>
          <div class="overview-label">运行时长</div>
        </div>
      </div>

      <div class="overview-card">
        <div class="overview-icon cyan"><el-icon><Cloudy /></el-icon></div>
        <div class="overview-info">
          <div class="overview-value">{{ overviewData.availability }}%</div>
          <div class="overview-label">服务可用性</div>
        </div>
      </div>
    </div>

    <div class="main-content">
      <div class="main-grid">
        <!-- 设备管理 -->
        <div class="card">
          <div class="card-header">
            <h3>设备管理</h3>
            
            <el-button type="primary" size="small">
              <el-icon><Plus /></el-icon> 添加设备
            </el-button>
          </div>

          <div class="card-body">
            <el-table :data="deviceList" stripe style="width: 100%" height="calc(100vh - 450px)">
              <el-table-column prop="name" label="设备名称" width="180" />
              <el-table-column prop="type" label="类型" width="120" />
              <el-table-column prop="location" label="位置" width="150" />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.status === '在线' ? 'success' : 'danger'">{{ row.status }}</el-tag>
                </template>
              </el-table-column>

              <el-table-column prop="lastOnline" label="最后在线" width="180" />

              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="viewDevice(row)">查看</el-button>
                  <el-button link type="primary" @click="restartDevice(row)">重启</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <!-- 系统日志 -->
        <div class="card">
          <div class="card-header">
            <h3>系统日志</h3>
            
            <el-button link>查看全部</el-button>
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

      <!-- 用户权限管理 -->
      <div class="card">
        <div class="card-header">
          <h3>用户权限管理</h3>
          
          <el-button type="primary" size="small">
            <el-icon><Plus /></el-icon> 添加用户
          </el-button>
        </div>

        <div class="card-body">
          <el-table :data="userList" stripe style="width: 100%">
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="realName" label="姓名" width="100" />
            <el-table-column prop="role" label="角色" width="120">
              <template #default="{ row }">
                <el-tag :type="getRoleType(row.role)">{{ row.role }}</el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="department" label="部门" width="120" />
            <el-table-column prop="phone" label="手机号" width="130" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-switch v-model="row.status" active-value="启用" inactive-value="禁用" />
              </template>
            </el-table-column>

            <el-table-column prop="lastLogin" label="最后登录" width="180" />

            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button link type="primary">编辑</el-button>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
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
          <el-tag :type="selectedDevice.status === '在线' ? 'success' : 'danger'">{{ selectedDevice.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最后在线">{{ selectedDevice.lastOnline }}</el-descriptions-item>
        <el-descriptions-item label="设备ID">DEV-{{ String(selectedDevice.id).padStart(4, '0') }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { VideoCamera, Warning, Timer, Cloudy, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { systemApi } from '@/api'

const showDeviceDetail = ref(false)
const selectedDevice = ref(null)
const loading = ref(false)

// 默认演示数据
const defaultOverviewData = {
  onlineDevices: 48,
  totalDevices: 50,
  systemAlerts: 3,
  uptime: 15,
  availability: 99.9
}

const defaultDeviceList = [
  { id: 1, name: '土壤传感器-01', type: '传感器', location: '1号田', status: '在线', lastOnline: '2024-03-20 10:30' },
  { id: 2, name: '灌溉设备-01', type: '控制器', location: '1号田', status: '在线', lastOnline: '2024-03-20 10:28' },
  { id: 3, name: '热泵烘干机-01', type: '烘干设备', location: '烘干车间', status: '在线', lastOnline: '2024-03-20 10:25' },
  { id: 4, name: '摄像头-01', type: '监控设备', location: '仓库入口', status: '离线', lastOnline: '2024-03-19 18:00' }
]

const defaultSystemLogs = [
  { id: 1, time: '10:30:15', module: '设备', level: 'info', message: '土壤传感器-01 数据上报正常' },
  { id: 2, time: '10:28:42', module: '灌溉', level: 'success', message: '灌溉设备-01 任务执行完成' },
  { id: 3, time: '10:25:08', module: '烘干', level: 'info', message: '热泵烘干机-01 温度达到设定值' },
  { id: 4, time: '10:20:33', module: '系统', level: 'warning', message: '摄像头-01 连接超时' },
  { id: 5, time: '10:15:20', module: '用户', level: 'info', message: '管理员 admin 登录系统' }
]

const defaultUserList = [
  { username: 'admin', realName: '系统管理员', role: '超级管理员', department: '技术部', phone: '138****8888', status: '启用', lastLogin: '2024-03-20 10:15' },
  { username: 'operator1', realName: '张三', role: '操作员', department: '生产部', phone: '139****6666', status: '启用', lastLogin: '2024-03-20 09:30' },
  { username: 'viewer1', realName: '李四', role: '查看员', department: '质检部', phone: '137****5555', status: '启用', lastLogin: '2024-03-19 16:20' }
]

const deviceList = ref([...defaultDeviceList])
const systemLogs = ref([...defaultSystemLogs])
const userList = ref([...defaultUserList])

const overviewData = ref({ ...defaultOverviewData })

const getRoleType = (role) => {
  const map = { '超级管理员': 'danger', '管理员': 'warning', '操作员': 'primary', '查看员': 'info' }
  return map[role] || 'info'
}

const viewDevice = (row) => {
  selectedDevice.value = row
  showDeviceDetail.value = true
}

const restartDevice = (row) => {
  ElMessage.success(`设备 ${row.name} 重启指令已发送`)
}

const loadSystemData = async () => {
  loading.value = true
  try {
    const [devices, logs] = await Promise.all([
      systemApi.getDevices(),
      systemApi.getLogs({ limit: 20 })
    ])
    if (devices) {
      const data = devices.list || devices.records || []
      deviceList.value = data.length > 0 ? data : [...defaultDeviceList]
      overviewData.value.onlineDevices = devices.onlineCount || defaultOverviewData.onlineDevices
      overviewData.value.totalDevices = devices.totalCount || deviceList.value.length
    } else {
      deviceList.value = [...defaultDeviceList]
      overviewData.value.onlineDevices = defaultOverviewData.onlineDevices
      overviewData.value.totalDevices = defaultOverviewData.totalDevices
    }
    if (logs) {
      const logData = logs.list || logs.records || []
      systemLogs.value = logData.length > 0 ? logData : [...defaultSystemLogs]
    } else {
      systemLogs.value = [...defaultSystemLogs]
    }
  } catch (error) {
    console.error('加载系统数据失败:', error)
    // 保持默认数据
    deviceList.value = [...defaultDeviceList]
    systemLogs.value = [...defaultSystemLogs]
    userList.value = [...defaultUserList]
    overviewData.value = { ...defaultOverviewData }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadSystemData()
})
</script>

<style scoped>
.system-page {
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

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.overview-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.overview-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;
}

.overview-icon.green { background: #52c41a; }
.overview-icon.blue { background: #1890ff; }
.overview-icon.orange { background: #faad14; }
.overview-icon.cyan { background: #13c2c2; }

.overview-value {
  font-size: 28px;
  font-weight: 600;
  color: #262626;
}

.overview-label {
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
  padding: 16px 20px;
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
  .overview-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .main-grid {
    grid-template-columns: 1fr;
  }
}
</style>
