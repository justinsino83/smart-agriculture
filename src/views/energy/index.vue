<template>
  <div class="energy-page">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else>
      <!-- 顶部统计 -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon yellow"><el-icon>
              <Lightning />
            </el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ todayPower }}<span class="unit">kWh</span></div>
            <div class="stat-label">今日用电</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon green"><el-icon>
              <Money />
            </el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ todayCost }}<span class="unit">元</span></div>
            <div class="stat-label">今日电费</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon blue"><el-icon>
              <TrendCharts />
            </el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ avgEfficiency }}<span class="unit">%</span></div>
            <div class="stat-label">能效指数</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon cyan"><el-icon>
              <Odometer />
            </el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ carbonSaved }}<span class="unit">kg</span></div>
            <div class="stat-label">累计减碳</div>
          </div>
        </div>
      </div>

      <div class="main-grid">
        <!-- 用电趋势 -->
        <div class="card">
          <div class="card-header">
            <h3>用电趋势分析</h3>

            <el-radio-group v-model="chartPeriod" size="small" @change="onPeriodChange">
              <el-radio-button label="day">今日</el-radio-button>
              <el-radio-button label="week">本周</el-radio-button>
              <el-radio-button label="month">本月</el-radio-button>
            </el-radio-group>
          </div>

          <div class="card-body">
            <div ref="powerChart" class="chart"></div>
          </div>
        </div>

        <!-- 设备能耗占比 -->
        <div class="card">
          <div class="card-header">
            <h3>设备能耗占比</h3>
          </div>

          <div class="card-body">
            <div ref="deviceChart" class="chart pie-chart"></div>
          </div>
        </div>
      </div>

      <!-- 能耗明细表 -->
      <div class="card">
        <div class="card-header">
          <h3>能耗明细</h3>
        </div>

        <div class="card-body">
          <el-table :data="energyList" stripe v-loading="tableLoading" style="width: 100%"
            :cell-style="{ padding: '10px 0' }"
            :header-cell-style="{ padding: '12px 0', background: '#fafafa', color: '#262626' }">
            <el-table-column prop="time" label="时间" min-width="170" />

            <el-table-column prop="device" label="设备名称" min-width="160" show-overflow-tooltip />

            <el-table-column prop="type" label="能耗类型" min-width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.type === '电' ? 'primary' : 'success'">{{ row.type }}</el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="usage" label="用量" min-width="120" align="right">
              <template #default="{ row }">
                <span style="font-weight: 500; padding-right: 10px;">{{ row.usage }} {{ row.unit }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="cost" label="费用(元)" min-width="120" align="right">
              <template #default="{ row }">
                <span class="text-primary" style="padding-right: 10px;">{{ row.cost }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="efficiency" label="能效等级" min-width="150" align="center">
              <template #default="{ row }">
                <div style="display: inline-block; white-space: nowrap;">
                  <el-rate v-model="row.efficiency" disabled />
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
          </el-table>

          <div class="pagination">
            <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total"
              layout="total, prev, pager, next" @current-change="loadEnergyList" />
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick, onUnmounted } from 'vue'
import { Lightning, Money, TrendCharts, Odometer } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { energyApi } from '@/api'

// 加载状态
const loading = ref(true)
const tableLoading = ref(false)

// 统计数据（从API获取）
const todayPower = ref(0)
const todayCost = ref(0)
const avgEfficiency = ref(0)
const carbonSaved = ref(0)

// 图表相关
const chartPeriod = ref('day')
const powerChart = ref(null)
const deviceChart = ref(null)
let powerChartInstance = null
let deviceChartInstance = null

// 能耗明细列表
const energyList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 加载统计数据
const loadStats = async () => {
  try {
    const response = await energyApi.getTodayStats()
    if (response) {
      todayPower.value = response.todayPower || 0
      todayCost.value = response.todayCost || 0
      avgEfficiency.value = response.avgEfficiency || 0
      carbonSaved.value = response.carbonSaved || 0
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    // 使用默认值
    todayPower.value = 856.5
    todayCost.value = 513.9
    avgEfficiency.value = 87
    carbonSaved.value = 2450
  }
}

// 加载能耗明细
const loadEnergyList = async () => {
  tableLoading.value = true
  try {
    // 调用实际API获取能耗明细
    const response = await energyApi.getList({
      page: currentPage.value,
      size: pageSize.value
    })
    if (response) {
      energyList.value = response.list || response.records || []
      total.value = response.total || 0
    }
  } catch (error) {
    console.error('加载能耗明细失败:', error)
    // 使用静态数据作为后备
    energyList.value = [
      { time: '2024-03-20 10:00', device: '热泵烘干机-01', type: '电', usage: 45.6, unit: 'kWh', cost: 27.4, efficiency: 4, remark: '正常运行' },
      { time: '2024-03-20 10:00', device: '灌溉设备-01', type: '水', usage: 12.5, unit: 'm³', cost: 25.0, efficiency: 5, remark: '节水模式' },
      { time: '2024-03-20 09:00', device: '热泵烘干机-02', type: '电', usage: 52.3, unit: 'kWh', cost: 31.4, efficiency: 3, remark: '高负荷运行' },
      { time: '2024-03-20 09:00', device: '1号仓空调', type: '电', usage: 28.7, unit: 'kWh', cost: 17.2, efficiency: 4, remark: '温控运行' },
      { time: '2024-03-20 08:00', device: '照明系统', type: '电', usage: 8.2, unit: 'kWh', cost: 4.9, efficiency: 5, remark: '节能模式' }
    ]
    total.value = 100
  } finally {
    tableLoading.value = false
  }
}

// 加载图表数据
const loadChartData = async () => {
  try {
    const [deviceUsage, trendData] = await Promise.all([
      energyApi.getDeviceUsage(),
      energyApi.getTrend(chartPeriod.value)
    ])

    if (deviceUsage && deviceUsage.length > 0) {
      initDeviceChart(deviceUsage)
    } else {
      initDeviceChart(generateDefaultDeviceUsage())
    }

    if (trendData && trendData.length > 0) {
      initPowerChart(trendData)
    } else {
      initPowerChart(generateDefaultTrend())
    }
  } catch (error) {
    console.error('加载图表数据失败:', error)
    initDeviceChart(generateDefaultDeviceUsage())
    initPowerChart(generateDefaultTrend())
  }
}

// 生成默认趋势数据
const generateDefaultTrend = () => {
  return [25, 22, 20, 18, 20, 28, 35, 42, 48, 52, 55, 58, 60, 62, 58, 55, 52, 48, 45, 42, 38, 35, 30, 28]
}

// 生成默认设备占比数据
const generateDefaultDeviceUsage = () => {
  return [
    { value: 320, name: '烘干设备', itemStyle: { color: '#ff4d4f' } },
    { value: 180, name: '仓储系统', itemStyle: { color: '#1890ff' } },
    { value: 150, name: '灌溉系统', itemStyle: { color: '#52c41a' } },
    { value: 120, name: '照明系统', itemStyle: { color: '#faad14' } },
    { value: 86, name: '其他', itemStyle: { color: '#722ed1' } }
  ]
}

const initPowerChart = (data) => {
  if (!powerChart.value) return

  powerChartInstance = echarts.init(powerChart.value)
  const hours = Array.from({ length: 24 }, (_, i) => `${i}:00`)

  const chartData = data && data.length > 0 ? data : generateDefaultTrend()

  const option = {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: hours,
      axisLine: { lineStyle: { color: '#d9d9d9' } },
      axisLabel: { color: '#8c8c8c' }
    },
    yAxis: {
      type: 'value',
      name: '功率(kW)',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f0f0f0' } }
    },
    series: [{
      name: '实时功率',
      type: 'line',
      smooth: true,
      data: chartData,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(250, 173, 20, 0.4)' },
          { offset: 1, color: 'rgba(250, 173, 20, 0.05)' }
        ])
      },
      itemStyle: { color: '#faad14' },
      lineStyle: { width: 3 }
    }]
  }
  powerChartInstance.setOption(option)
}

const initDeviceChart = (data) => {
  if (!deviceChart.value) return

  deviceChartInstance = echarts.init(deviceChart.value)

  const chartData = data && data.length > 0 ? data : generateDefaultDeviceUsage()

  const option = {
    tooltip: { trigger: 'item', formatter: '{b}: {c}kWh ({d}%)' },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { color: '#595959' }
    },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      label: { show: false },
      labelLine: { show: false },
      data: chartData
    }]
  }
  deviceChartInstance.setOption(option)
}

const onPeriodChange = (period) => {
  console.log('切换周期:', period)
  // 切换周期时重新加载数据
  loadChartData()
}

// 初始化图表
const initCharts = () => {
  nextTick(() => {
    initPowerChart()
    initDeviceChart()
  })
}

onMounted(async () => {
  try {
    // 1. 先加载不依赖特定 DOM 尺寸的数据
    await Promise.all([
      loadStats(),
      loadEnergyList()
    ])
    
    // 2. 核心修复：数据拿到后，先把 loading 关掉
    // 这样 v-else 里的图表 DOM 就会被 Vue 挂载到页面上
    loading.value = false
    
    // 3. 等待 Vue 将 DOM 实际渲染完成
    await nextTick()
    
    // 4. 此时图表 DOM (powerChart.value 和 deviceChart.value) 已经存在，安全加载图表
    await loadChartData()
    
  } catch (error) {
    console.error('初始化数据失败:', error)
    ElMessage.error('数据加载失败，请检查网络')
    loading.value = false // 发生错误时也要确保关掉 loading
  }
  
  // 响应式调整
  window.addEventListener('resize', () => {
    powerChartInstance?.resize()
    deviceChartInstance?.resize()
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', () => {
    powerChartInstance?.resize()
    deviceChartInstance?.resize()
  })
  powerChartInstance?.dispose()
  deviceChartInstance?.dispose()
})
</script>

<style scoped>
.energy-page {
  padding: 0;
}

.loading-container {
  padding: 20px;
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

.stat-icon.green {
  background: #52c41a;
}

.stat-icon.blue {
  background: #1890ff;
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

.main-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.card-body {
  padding: 20px;
}

.chart {
  height: 320px;
}

.pie-chart {
  height: 280px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.text-primary {
  color: #1890ff;
  font-weight: 600;
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