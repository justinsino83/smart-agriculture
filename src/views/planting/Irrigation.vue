<template>
  <div class="irrigation-page">
    <!-- 第一行：顶部概览 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon blue"><el-icon>
            <Watermelon />
          </el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ statsData.totalDevices || 0 }}<span class="unit">个</span></div>
          <div class="stat-label">设备总数</div>
          <div class="stat-sub">在线{{ statsData.onlineDevices || 0 }} | 运行{{ statsData.runningDevices || 0 }}</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon green"><el-icon>
            <CircleCheck />
          </el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ statsData.todayCount || 0 }}<span class="unit">次</span></div>
          <div class="stat-label">今日灌溉</div>
          <div class="stat-sub">累计{{ statsData.todayUsage || 0 }}m³</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon orange"><el-icon>
            <Timer />
          </el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ statsData.runningCount || 0 }}<span class="unit">个</span></div>
          <div class="stat-label">执行中任务</div>
          <div class="stat-sub">待执行{{ statsData.planCount || 0 }}个</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon cyan"><el-icon>
            <TrendCharts />
          </el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ statsData.savingRate || 0 }}%</div>
          <div class="stat-label">节水率</div>
          <div class="stat-sub">较传统灌溉</div>
        </div>
      </div>
    </div>

    <!-- 第二行：设备状态分布 + 用水统计 -->
    <div class="charts-row">
      <div class="card">
        <div class="card-header">
          <h3>设备状态分布</h3>
        </div>
        <div class="card-body">
          <div ref="deviceStatusChart" class="status-chart"></div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <h3>用水统计</h3>
          <el-radio-group v-model="waterStatType" size="small">
            <el-radio-button label="week">本周</el-radio-button>
            <el-radio-button label="month">本月</el-radio-button>
          </el-radio-group>
        </div>
        <div class="card-body">
          <div ref="waterChart" class="water-chart"></div>
        </div>
      </div>
    </div>

    <!-- 第三行：设备列表 -->
    <div class="card">
      <div class="card-header">
        <h3>灌溉设备</h3>
        <el-button type="primary" size="small" @click="showScheduleDialog = true">
          + 新建计划
        </el-button>
      </div>
      <div class="card-body">
        <el-table :data="devices" stripe style="width: 100%" v-loading="loading" :cell-style="{ padding: '10px 0' }"
          :header-cell-style="{ padding: '12px 0', background: '#fafafa', color: '#262626' }">
          <el-table-column prop="deviceName" label="设备名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="deviceCode" label="设备编号" min-width="110" show-overflow-tooltip />
          <el-table-column prop="location" label="位置" min-width="150" show-overflow-tooltip />

          <el-table-column prop="flowRate" label="流量(m³/h)" min-width="110" align="right">
            <template #default="{ row }">
              <span style="padding-right: 15px; font-weight: 500;">{{ row.flowRate }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="totalRunTime" label="累计运行时长" min-width="140" align="right">
            <template #default="{ row }">
              <span style="padding-right: 15px;">{{ row.totalRunTime || 0 }} 分钟</span>
            </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" min-width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="100" fixed="right" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.status" :active-value="2" :inactive-value="1"
                @change="(val) => toggleDevice(row, val)" />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 第四行：灌溉任务列表 -->
    <div class="card">
      <div class="card-header">
        <h3>灌溉任务</h3>
        <el-radio-group v-model="taskFilter" size="small">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="0">待执行</el-radio-button>
          <el-radio-button label="1">执行中</el-radio-button>
          <el-radio-button label="2">已完成</el-radio-button>
        </el-radio-group>
      </div>
      <div class="card-body">
        <el-table :data="filteredTasks" stripe style="width: 100%" v-loading="taskLoading"
          :cell-style="{ padding: '10px 0' }"
          :header-cell-style="{ padding: '12px 0', background: '#fafafa', color: '#262626' }">
          <el-table-column prop="taskName" label="任务名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="deviceId" label="设备ID" min-width="100" show-overflow-tooltip />
          <el-table-column prop="planStartTime" label="计划开始" min-width="160" />

          <el-table-column prop="duration" label="计划时长" min-width="110" align="right">
            <template #default="{ row }">
              <span style="padding-right: 15px;">{{ row.duration }} 分钟</span>
            </template>
          </el-table-column>

          <el-table-column prop="waterUsage" label="用水量" min-width="110" align="right">
            <template #default="{ row }">
              <span style="padding-right: 15px; color: #1890ff; font-weight: 500;">
                {{ row.waterUsage ? row.waterUsage + ' m³' : '-' }}
              </span>
            </template>
          </el-table-column>

          <el-table-column prop="triggerType" label="触发方式" min-width="100" align="center">
            <template #default="{ row }">
              {{ row.triggerType === 1 ? '手动' : '自动' }}
            </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" min-width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getTaskStatusType(row.status)" size="small">
                {{ getTaskStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="160" fixed="right" align="center">
            <template #default="{ row }">
              <el-button v-if="row.status === 0" type="primary" size="small" link
                @click="executeTask(row.id)">执行</el-button>
              <el-button v-if="row.status === 1" type="success" size="small" link
                @click="completeTask(row.id)">完成</el-button>
              <el-button v-if="row.status === 0" type="danger" size="small" link
                @click="cancelTask(row.id)">取消</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 新建计划弹窗 -->
    <el-dialog v-model="showScheduleDialog" title="新建灌溉计划" width="600px">
      <el-form :model="scheduleForm" label-width="100px">
        <el-form-item label="计划名称">
          <el-input v-model="scheduleForm.name" placeholder="请输入计划名称" />
        </el-form-item>

        <el-form-item label="灌溉设备">
          <el-select v-model="scheduleForm.device" placeholder="选择设备" style="width: 100%">
            <el-option v-for="d in devices" :key="d.id" :label="d.deviceName" :value="d.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="开始时间">
          <el-date-picker v-model="scheduleForm.startTime" type="datetime" placeholder="选择开始时间" style="width: 100%" />
        </el-form-item>

        <el-form-item label="灌溉时长">
          <el-input-number v-model="scheduleForm.duration" :min="10" :max="120" />
          <span class="unit">分钟</span>
        </el-form-item>

        <el-form-item label="触发条件">
          <el-radio-group v-model="scheduleForm.trigger">
            <el-radio label="manual">手动</el-radio>
            <el-radio label="auto">自动（土壤湿度&lt;30%）</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showScheduleDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSchedule">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Watermelon, CircleCheck, Timer, TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { irrigationApi } from '@/api'

const waterStatType = ref('week')
const taskFilter = ref('all')
const showScheduleDialog = ref(false)
const waterChart = ref(null)
const deviceStatusChart = ref(null)
let waterChartInstance = null
let deviceStatusChartInstance = null
const loading = ref(false)
const taskLoading = ref(false)

const statsData = ref({})
const devices = ref([])
const tasks = ref([])

const scheduleForm = reactive({
  name: '',
  device: '',
  startTime: null,
  duration: 30,
  trigger: 'manual'
})

const filteredTasks = computed(() => {
  if (taskFilter.value === 'all') return tasks.value
  return tasks.value.filter(t => String(t.status) === taskFilter.value)
})

function getStatusType(status) {
  if (status === 2) return 'success'
  if (status === 1) return 'warning'
  return 'info'
}

function getStatusText(status) {
  if (status === 2) return '运行中'
  if (status === 1) return '在线'
  return '离线'
}

function getTaskStatusType(status) {
  if (status === 2) return 'success'
  if (status === 1) return 'warning'
  if (status === 3) return 'danger'
  return 'info'
}

function getTaskStatusText(status) {
  const map = { 0: '待执行', 1: '执行中', 2: '已完成', 3: '已取消' }
  return map[status] || '未知'
}

function formatDateTime(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const initWaterChart = () => {
  if (!waterChart.value) return
  waterChartInstance = echarts.init(waterChart.value)
  waterChartInstance.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['计划用水', '实际用水'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'], axisLine: { lineStyle: { color: '#d9d9d9' } } },
    yAxis: { type: 'value', name: '用水量(m³)', axisLine: { show: false }, splitLine: { lineStyle: { color: '#f0f0f0' } } },
    series: [
      { name: '计划用水', type: 'bar', data: [12, 15, 10, 18, 14, 8, 10], itemStyle: { color: '#d9d9d9' }, barWidth: '35%' },
      { name: '实际用水', type: 'bar', data: [10, 12, 8, 15, 12, 7, 9], itemStyle: { color: '#1890ff' }, barWidth: '35%' }
    ]
  })
}

const initDeviceStatusChart = () => {
  if (!deviceStatusChart.value) return
  deviceStatusChartInstance = echarts.init(deviceStatusChart.value)
  const runningCount = statsData.value.runningDevices || 0
  const onlineCount = (statsData.value.onlineDevices || 0) - runningCount
  const offlineCount = (statsData.value.totalDevices || 0) - (statsData.value.onlineDevices || 0)
  deviceStatusChartInstance.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      data: [
        { value: runningCount, name: '运行中', itemStyle: { color: '#52c41a' } },
        { value: onlineCount, name: '在线待命', itemStyle: { color: '#faad14' } },
        { value: offlineCount, name: '离线', itemStyle: { color: '#d9d9d9' } }
      ]
    }]
  })
}

async function loadDevices() {
  try {
    loading.value = true
    const res = await irrigationApi.getDevices()
    devices.value = res || []
  } catch (e) {
    ElMessage.error('加载设备失败')
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res = await irrigationApi.getStatistics('day')
    if (res) statsData.value = res
  } catch (e) {
    console.error('加载统计失败', e)
  }
}

async function loadTasks() {
  try {
    taskLoading.value = true
    const res = await irrigationApi.getTasks()
    tasks.value = res || []
  } catch (e) {
    console.error('加载任务失败', e)
  } finally {
    taskLoading.value = false
  }
}

async function loadWaterStatistics() {
  try {
    const days = waterStatType.value === 'week' ? 7 : 30
    const trend = await irrigationApi.getTrend(days)
    if (trend && trend.length > 0) updateWaterChart(trend)
  } catch (e) { }
}

function updateWaterChart(trend) {
  if (!waterChartInstance) return
  const labels = trend.map(t => t.date || '')
  const waterData = trend.map(t => t.water || 0)
  waterChartInstance.setOption({
    xAxis: { type: 'category', data: labels, axisLine: { lineStyle: { color: '#d9d9d9' } } },
    series: [
      { name: '计划用水', type: 'bar', data: waterData.map(v => Math.round(v * 1.2 * 10) / 10), itemStyle: { color: '#d9d9d9' }, barWidth: '35%' },
      { name: '实际用水', type: 'bar', data: waterData, itemStyle: { color: '#1890ff' }, barWidth: '35%' }
    ]
  })
}

const toggleDevice = async (device, val) => {
  try {
    await irrigationApi.controlDevice(device.id, val === 2)
    ElMessage.success(`${device.deviceName}已${val === 2 ? '启动' : '停止'}`)
  } catch (e) {
    ElMessage.error('控制失败: ' + e.message)
    loadDevices()
  }
}

const saveSchedule = async () => {
  try {
    await irrigationApi.createTask({
      taskName: scheduleForm.name,
      deviceId: scheduleForm.device,
      planStartTime: scheduleForm.startTime,
      duration: scheduleForm.duration,
      triggerType: scheduleForm.trigger === 'auto' ? 2 : 1
    })
    ElMessage.success('灌溉计划已创建')
    showScheduleDialog.value = false
    loadTasks()
    loadStats()
  } catch (e) {
    ElMessage.error('创建失败: ' + e.message)
  }
}

const executeTask = async (taskId) => {
  try {
    await irrigationApi.executeTask(taskId)
    ElMessage.success('任务已开始执行')
    loadTasks()
    loadDevices()
    loadStats()
  } catch (e) {
    ElMessage.error('执行失败: ' + e.message)
  }
}

const completeTask = async (taskId) => {
  try {
    await irrigationApi.completeTask(taskId)
    ElMessage.success('任务已完成')
    loadTasks()
    loadDevices()
    loadStats()
  } catch (e) {
    ElMessage.error('完成失败: ' + e.message)
  }
}

const cancelTask = async (taskId) => {
  try {
    await irrigationApi.cancelTask(taskId)
    ElMessage.success('任务已取消')
    loadTasks()
    loadStats()
  } catch (e) {
    ElMessage.error('取消失败: ' + e.message)
  }
}

onMounted(async () => {
  initWaterChart()
  await Promise.all([loadDevices(), loadStats(), loadTasks()])
  initDeviceStatusChart()
  await loadWaterStatistics()
  window.addEventListener('resize', () => {
    waterChartInstance?.resize()
    deviceStatusChartInstance?.resize()
  })
})

watch(waterStatType, loadWaterStatistics)
watch(() => statsData.value, () => {
  if (deviceStatusChartInstance) initDeviceStatusChart()
}, { deep: true })
</script>

<style scoped>
.irrigation-page {
  padding: 0;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
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

.stat-icon.blue {
  background: #1890ff;
}

.stat-icon.green {
  background: #52c41a;
}

.stat-icon.orange {
  background: #faad14;
}

.stat-icon.cyan {
  background: #13c2c2;
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

.stat-sub {
  font-size: 12px;
  color: #52c41a;
  margin-top: 4px;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 20px;
  margin-bottom: 20px;
}

.card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin: 0;
}

.card-body {
  padding: 16px 20px;
}

.status-chart {
  height: 240px;
}

.water-chart {
  height: 240px;
}

.unit {
  margin-left: 8px;
  color: #8c8c8c;
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .charts-row {
    grid-template-columns: 1fr;
  }
}
</style>