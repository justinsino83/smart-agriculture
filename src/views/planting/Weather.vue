<template>
  <div class="weather-page">
    <!-- 第一行：筛选条件 -->
    <div class="filter-row">
      <el-select v-model="filterForm.clientId" placeholder="选择设备号" clearable size="default" style="width: 240px"
        :disabled="loading"
        @change="handleFilterChange">
        <el-option v-for="device in devices" :key="device.clientId" :label="device.clientId" :value="device.clientId" />
      </el-select>

      <el-date-picker v-model="filterForm.dateRange" type="datetimerange" range-separator="至" start-placeholder="开始时间"
        end-placeholder="结束时间" size="default" style="width: 380px" :disabled="loading" @change="handleFilterChange" />

      <el-button type="primary" :loading="loading" @click="handleRefresh">
        <el-icon>
          <Refresh />
        </el-icon> 刷新数据
      </el-button>

      <el-button :disabled="loading" @click="handleExport">
        <el-icon>
          <Download />
        </el-icon> 导出数据
      </el-button>
    </div>

    <div class="content-wrapper" v-loading="loading" element-loading-text="设备数据加载中...">
    <!-- 第二行：实时数据卡片 -->
    <div class="realtime-cards">
      <div class="realtime-card">
        <div class="card-icon blue">
          <el-icon>
            <Sunny />
          </el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ formatValue(currentData.temperature, '°C') }}</div>
          <div class="card-label">温度</div>
        </div>
      </div>

      <div class="realtime-card">
        <div class="card-icon cyan">
          <el-icon>
            <Sunrise />
          </el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ formatValue(currentData.humidity, '%') }}</div>
          <div class="card-label">湿度</div>
        </div>
      </div>

      <div class="realtime-card">
        <div class="card-icon green">
          <el-icon>
            <WindPower />
          </el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ formatValue(currentData.windSpeed, 'm/s') }}</div>
          <div class="card-label">风速</div>
        </div>
      </div>

      <div class="realtime-card">
        <div class="card-icon orange">
          <el-icon>
            <Compass />
          </el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ currentData.windDirectionName || '—' }}</div>
          <div class="card-label">风向</div>
        </div>
      </div>

      <div class="realtime-card">
        <div class="card-icon purple">
          <el-icon>
            <Odometer />
          </el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ formatValue(currentData.pressure, 'hPa') }}</div>
          <div class="card-label">气压</div>
        </div>
      </div>

      <div class="realtime-card">
        <div class="card-icon cyan">
          <el-icon>
            <Download />
          </el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ formatValue(currentData.rainfall, 'mm') }}</div>
          <div class="card-label">雨量</div>
        </div>
      </div>

      <div class="realtime-card">
        <div class="card-icon orange">
          <el-icon>
            <Sunny />
          </el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ formatValue(currentData.lightIntensity, 'lux') }}</div>
          <div class="card-label">光照强度</div>
        </div>
      </div>

      <div class="realtime-card">
        <div class="card-icon gray">
          <el-icon>
            <QuartzWatch />
          </el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ formatValue(currentData.dewTemp, '°C') }}</div>
          <div class="card-label">露点温度</div>
        </div>
      </div>

      <div class="realtime-card">
        <div class="card-icon purple">
          <el-icon>
            <Odometer />
          </el-icon>
        </div>
        <div class="card-content">
          <div class="card-value">{{ formatValue(currentData.co2, 'ppm') }}</div>
          <div class="card-label">二氧化碳</div>
        </div>
      </div>
    </div>

    <!-- 第三行：24小时温湿度走势 -->
    <div class="charts-row">
      <div class="card">
        <div class="card-header">
          <h3>24小时温度走势</h3>
        </div>
        <div class="card-body">
          <div ref="tempChart" class="chart"></div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <h3>24小时湿度走势</h3>
        </div>
        <div class="card-body">
          <div ref="humidityChart" class="chart"></div>
        </div>
      </div>
    </div>

    <!-- 第四行：24小时风力风向 -->
    <div class="card">
      <div class="card-header">
        <h3>24小时风力风向统计</h3>
      </div>
      <div class="card-body">
        <div ref="windChart" class="chart"></div>
      </div>
    </div>

    <!-- 第五行：每小时风向变化表 -->
    <div class="card">
      <div class="card-header">
        <h3>每小时风向变化</h3>
      </div>
      <div class="card-body">
        <el-table :data="hourlyWindData" stripe style="width: 100%" v-loading="loading"
          :cell-style="{ padding: '10px 0' }"
          :header-cell-style="{ padding: '12px 0', background: '#fafafa', color: '#262626' }">
          <el-table-column prop="time" label="时间" min-width="140" />

          <el-table-column prop="directionName" label="风向" min-width="120">
            <template #default="{ row }">
              <span :style="{ color: getWindDirectionColor(row.direction), fontWeight: 500 }">
                {{ row.directionName }}
              </span>
            </template>
          </el-table-column>

          <el-table-column prop="direction" label="角度" min-width="120" align="right">
            <template #default="{ row }">
              <span style="padding-right: 15px;">
                {{ row.direction ? row.direction + '°' : '—' }}
              </span>
            </template>
          </el-table-column>

          <el-table-column prop="windSpeed" label="风速(m/s)" min-width="120" align="right">
            <template #default="{ row }">
              <span style="padding-right: 15px; font-weight: 500;">
                {{ formatValue(row.windSpeed, 'm/s') }}
              </span>
            </template>
          </el-table-column>

          <el-table-column prop="recordCount" label="记录数" min-width="100" align="right" />

          <el-table-column label="风向示意" min-width="180" align="center">
            <template #default="{ row }">
              <div v-if="row.direction" class="wind-arrow" :style="{ transform: `rotate(${row.direction}deg)` }">
                ↑
              </div>
              <span v-else>—</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <el-pagination
            v-model:current-page="windPage.current"
            v-model:page-size="windPage.size"
            :total="windPage.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="updateHourlyWindPage"
            @size-change="handleWindPageSizeChange"
          />
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { Refresh, Download, Sunny, WindPower, Compass, Odometer, QuartzWatch, Sunrise } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { devicePushApi, weatherApi } from '@/api'
import { ElMessage } from 'element-plus'

const tempChart = ref(null)
const humidityChart = ref(null)
const windChart = ref(null)
let tempChartInstance = null
let humidityChartInstance = null
let windChartInstance = null
const loading = ref(false)
const devices = ref([])

const currentData = reactive({
  temperature: '--',
  humidity: '--',
  windSpeed: '--',
  windDirection: null,
  windDirectionName: '--',
  pressure: '--',
  rainfall: '--',
  lightIntensity: '--',
  dewTemp: '--',
  co2: '--',
  weatherText: '--'
})

const filterForm = reactive({
  clientId: '',
  dateRange: null
})

const allHourlyWindData = ref([])
const hourlyWindData = ref([])
const windPage = reactive({
  current: 1,
  size: 10,
  total: 0
})

function getDirectionName(degree) {
  if (!degree) return '--'
  const directions = ['北风', '东北风', '东风', '东南风', '南风', '西南风', '西风', '西北风']
  const index = Math.round((degree % 360) / 45) % 8
  return directions[index]
}

function getWindDirectionColor(degree) {
  if (!degree) return '#8c8c8c'
  const colors = ['#1890ff', '#36cfc9', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#eb2f96', '#fa8c16']
  const index = Math.round((degree % 360) / 45) % 8
  return colors[index]
}

function formatValue(value, unit = '') {
  if (value === null || value === undefined || value === '' || value === '--') {
    return '—'
  }
  return `${value} ${unit}`.trim()
}

function extractHourKey(time) {
  if (!time) return ''

  if (/^\d{2}:\d{2}$/.test(time)) {
    return `${time.slice(0, 2)}:00`
  }

  if (/^\d{4}-\d{2}-\d{2}[ T]\d{2}/.test(time)) {
    const normalized = time.replace(' ', 'T')
    return `${normalized.slice(0, 13)}:00`
  }

  const date = new Date(time)
  if (Number.isNaN(date.getTime())) {
    return String(time)
  }

  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  return `${month}-${day} ${hour}:00`
}

function aggregateHourlyWindData(records) {
  const grouped = new Map()

  ;(records || []).forEach((item) => {
    if (!item?.time) return
    const key = extractHourKey(item.time)
    if (!key) return
    if (!grouped.has(key)) {
      grouped.set(key, {
        time: key,
        sinSum: 0,
        cosSum: 0,
        speedSum: 0,
        speedCount: 0,
        recordCount: 0
      })
    }

    const bucket = grouped.get(key)
    bucket.recordCount += 1

    if (item.direction != null) {
      const radians = (item.direction * Math.PI) / 180
      bucket.sinSum += Math.sin(radians)
      bucket.cosSum += Math.cos(radians)
    }

    if (item.windSpeed != null && item.windSpeed !== '') {
      bucket.speedSum += Number(item.windSpeed)
      bucket.speedCount += 1
    }
  })

  return Array.from(grouped.values())
    .map((bucket) => {
      let direction = null
      if (bucket.sinSum !== 0 || bucket.cosSum !== 0) {
        direction = Math.round((Math.atan2(bucket.sinSum, bucket.cosSum) * 180) / Math.PI)
        if (direction < 0) direction += 360
      }

      const windSpeed = bucket.speedCount > 0 ? Number((bucket.speedSum / bucket.speedCount).toFixed(2)) : null

      return {
        time: bucket.time,
        direction,
        directionName: direction != null ? getDirectionName(direction) : '—',
        windSpeed,
        recordCount: bucket.recordCount
      }
    })
    .sort((a, b) => b.time.localeCompare(a.time))
}

function updateHourlyWindPage() {
  const start = (windPage.current - 1) * windPage.size
  const end = start + windPage.size
  hourlyWindData.value = allHourlyWindData.value.slice(start, end)
}

function handleWindPageSizeChange(size) {
  windPage.size = size
  windPage.current = 1
  updateHourlyWindPage()
}

async function loadDevices() {
  try {
    const list = await devicePushApi.getActiveDevices()
    const weatherDevices = (list || []).filter(item =>
      item.clientId && (
        item.ambientTemperature != null ||
        item.ambientHumidity != null ||
        item.pressure != null ||
        item.windSpeed != null ||
        item.windDirection != null ||
        item.rainfall != null ||
        item.lightIntensity != null ||
        item.dewTemp != null ||
        item.co2 != null
      )
    )

    devices.value = weatherDevices
      .map(item => ({ clientId: item.clientId }))
      .filter((item, index, arr) => arr.findIndex(v => v.clientId === item.clientId) === index)

    if (!filterForm.clientId && devices.value.length > 0) {
      filterForm.clientId = devices.value[0].clientId
    }
  } catch (error) {
    console.error('加载气象设备列表失败', error)
  }
}

function initTempChart(data) {
  if (!tempChart.value) return
  tempChartInstance = echarts.init(tempChart.value)

  const labels = data?.labels || []
  const temperatures = data?.temperatures || []

  const option = {
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#d9d9d9' } }
    },
    yAxis: {
      type: 'value',
      name: '温度(°C)',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f0f0f0' } }
    },
    grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
    tooltip: { trigger: 'axis' },
    series: [{
      data: temperatures,
      type: 'line',
      smooth: true,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(250, 173, 20, 0.3)' },
          { offset: 1, color: 'rgba(250, 173, 20, 0.05)' }
        ])
      },
      itemStyle: { color: '#faad14' },
      lineStyle: { width: 2 },
      symbol: 'circle',
      symbolSize: 4
    }]
  }
  tempChartInstance.setOption(option)
}

function initHumidityChart(data) {
  if (!humidityChart.value) return
  humidityChartInstance = echarts.init(humidityChart.value)

  const labels = data?.labels || []
  const humidity = data?.humidity || []

  const option = {
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#d9d9d9' } }
    },
    yAxis: {
      type: 'value',
      name: '湿度(%)',
      min: 0,
      max: 100,
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f0f0f0' } }
    },
    grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
    tooltip: { trigger: 'axis' },
    series: [{
      data: humidity,
      type: 'line',
      smooth: true,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(24, 144, 255, 0.3)' },
          { offset: 1, color: 'rgba(24, 144, 255, 0.05)' }
        ])
      },
      itemStyle: { color: '#1890ff' },
      lineStyle: { width: 2 },
      symbol: 'circle',
      symbolSize: 4
    }]
  }
  humidityChartInstance.setOption(option)
}

function initWindChart(data) {
  if (!windChart.value) return
  windChartInstance = echarts.init(windChart.value)

  const directions = data?.directions || ['北风', '东北风', '东风', '东南风', '南风', '西南风', '西风', '西北风']
  const windScale = data?.windScale || []

  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: directions,
      axisLine: { lineStyle: { color: '#d9d9d9' } }
    },
    yAxis: {
      type: 'value',
      name: '频次',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f0f0f0' } }
    },
    grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
    series: [{
      data: windScale,
      type: 'bar',
      barWidth: '50%',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#52c41a' },
          { offset: 1, color: '#d9d9d9' }
        ])
      }
    }]
  }
  windChartInstance.setOption(option)
}

async function loadWeatherData() {
  try {
    loading.value = true

    let startTime = null
    let endTime = null
    if (filterForm.dateRange && filterForm.dateRange.length === 2) {
      startTime = filterForm.dateRange[0].toISOString()
      endTime = filterForm.dateRange[1].toISOString()
    }

    const clientId = filterForm.clientId || null

    const [current, tempTrend, humidityTrend, windTrend] = await Promise.all([
      weatherApi.getCurrentWeather(clientId),
      weatherApi.get24HourTrend(clientId, startTime, endTime),
      weatherApi.get24HourHumidityTrend(clientId, startTime, endTime),
      weatherApi.get24HourWindDirectionTrend(clientId, startTime, endTime)
    ])

    // 更新当前数据
    if (current && current.temperature != null) {
      currentData.temperature = current.temperature
      currentData.humidity = current.humidity
      currentData.windSpeed = current.windSpeed
      currentData.windDirection = current.windDirection
      currentData.windDirectionName = current.windDirection ? getDirectionName(current.windDirection) : '--'
      currentData.pressure = current.pressure
      currentData.rainfall = current.rainfall
      currentData.lightIntensity = current.lightIntensity
      currentData.dewTemp = current.dewTemp
      currentData.co2 = current.co2
      currentData.weatherText = current.weatherText || '--'
    } else {
      currentData.temperature = '--'
      currentData.humidity = '--'
      currentData.windSpeed = '--'
      currentData.windDirection = null
      currentData.windDirectionName = '--'
      currentData.pressure = '--'
      currentData.rainfall = '--'
      currentData.lightIntensity = '--'
      currentData.dewTemp = '--'
      currentData.co2 = '--'
      currentData.weatherText = '--'
    }

    // 更新图表
    if (!tempTrend || !tempTrend.temperatures || tempTrend.temperatures.length === 0) {
      tempChartInstance?.clear()
    } else {
      initTempChart(tempTrend)
    }

    if (!humidityTrend || !humidityTrend.humidity || humidityTrend.humidity.length === 0) {
      humidityChartInstance?.clear()
    } else {
      initHumidityChart(humidityTrend)
    }

    if (!windTrend || !windTrend.windScale || windTrend.windScale.length === 0) {
      windChartInstance?.clear()
    } else {
      initWindChart(windTrend)
    }

    // 更新每小时风向数据
    allHourlyWindData.value = aggregateHourlyWindData(windTrend?.hourlyWindDirection || [])
    windPage.total = allHourlyWindData.value.length
    windPage.current = 1
    updateHourlyWindPage()
  } catch (e) {
    ElMessage.error('加载天气数据失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

function handleRefresh() {
  loadWeatherData()
}

function handleFilterChange() {
  loadWeatherData()
}

function handleExport() {
  ElMessage.info('导出功能开发中')
}

function handleResize() {
  tempChartInstance?.resize()
  humidityChartInstance?.resize()
  windChartInstance?.resize()
}

onMounted(async () => {
  await loadDevices()
  await loadWeatherData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  tempChartInstance?.dispose()
  humidityChartInstance?.dispose()
  windChartInstance?.dispose()
})
</script>

<style scoped>
.weather-page {
  padding: 0;
}

.filter-row {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.realtime-cards {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.realtime-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.card-icon.blue {
  background: #1890ff;
}

.card-icon.cyan {
  background: #13c2c2;
}

.card-icon.green {
  background: #52c41a;
}

.card-icon.orange {
  background: #faad14;
}

.card-icon.purple {
  background: #722ed1;
}

.card-icon.gray {
  background: #8c8c8c;
}

.card-value {
  font-size: 24px;
  font-weight: 600;
  color: #262626;
}

.card-label {
  font-size: 14px;
  color: #8c8c8c;
  margin-top: 4px;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.card-body {
  padding: 16px 20px;
}

.chart {
  height: 260px;
}

.content-wrapper {
  min-height: 320px;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.wind-arrow {
  display: inline-block;
  font-size: 20px;
  color: #52c41a;
  transition: transform 0.3s;
}

@media (max-width: 1400px) {
  .realtime-cards {
    grid-template-columns: repeat(3, 1fr);
  }

  .charts-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .realtime-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .filter-row {
    flex-wrap: wrap;
  }
}
</style>
