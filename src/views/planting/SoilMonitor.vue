<template>
  <div class="soil-monitor">
    <!-- 顶部筛选区 -->
    <div class="filter-bar">
      <el-select v-model="selectedSensor" placeholder="选择设备编号" size="large" style="width: 200px">
        <el-option v-for="sensor in sensors" :key="sensor.id" :label="sensor.deviceCode" :value="sensor.id" />
      </el-select>

      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
        end-placeholder="结束日期" size="large" />

      <el-button type="primary" size="large" @click="refreshData">
        <el-icon>
          <Refresh />
        </el-icon> 刷新数据
      </el-button>

      <el-button size="large" @click="exportData">
        <el-icon>
          <Download />
        </el-icon> 导出数据
      </el-button>
    </div>

    <!-- 实时监测仪表盘 -->
    <div class="section">
      <div class="section-title">
        <h3>实时监测数据</h3>
        <el-tag type="success">数据更新时间: {{ updateTime }}</el-tag>
      </div>

      <div class="gauge-grid">
        <div v-for="item in gaugeData" :key="item.name" class="gauge-item">
          <div class="gauge-chart">
            <div class="gauge-value-container">
              <div class="gauge-value" :style="{ color: item.color }">{{ item.value }}</div>
              <div class="gauge-unit">{{ item.unit }}</div>
            </div>
            <el-progress type="dashboard" :percentage="item.percentage" :color="item.color" :stroke-width="10"
              :width="140" :show-text="false" />
          </div>
          <div class="gauge-info">
            <div class="gauge-name">{{ item.name }}</div>
            <div class="gauge-range">
              适宜范围: {{ item.min }}-{{ item.max }}{{ item.unit }}
            </div>
            <el-tag :type="getTagType(item.status)" size="small">
              {{ item.statusText }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 历史趋势图 -->
    <div class="section">
      <div class="section-title">
        <h3>历史趋势分析</h3>
        <el-radio-group v-model="chartType" size="small">
          <el-radio-button label="day">日</el-radio-button>
          <el-radio-button label="week">周</el-radio-button>
          <el-radio-button label="month">月</el-radio-button>
        </el-radio-group>
      </div>

      <div ref="trendChart" class="trend-chart"></div>
    </div>

    <!-- 雷达图对比 -->
    <div class="section">
      <div class="section-title">
        <h3>多设备综合分析</h3>
        <el-tag>显示所有设备实时数据（最多10个）</el-tag>
      </div>

      <div class="radar-chart-container">
        <div ref="radarChart" class="radar-chart"></div>
      </div>
    </div>

    <!-- 数据明细表 -->
    <div class="section">
      <div class="section-title">
        <h3>监测数据明细</h3>
      </div>

      <el-table :data="tableData" stripe style="width: 100%" :cell-style="{ padding: '10px 0' }"
        :header-cell-style="{ padding: '12px 0', background: '#fafafa', color: '#262626' }">
        <el-table-column prop="time" label="时间" min-width="170" />

        <el-table-column prop="moisture" label="土壤湿度(%)" min-width="120" align="right">
          <template #default="{ row }">
            <span :class="getMoistureClass(row.moisture)" style="padding-right: 15px; font-weight: 500;">{{ row.moisture
              }}%</span>
          </template>
        </el-table-column>

        <el-table-column prop="temperature" label="土壤温度(°C)" min-width="120" align="right">
          <template #default="{ row }">
            <span style="padding-right: 15px;">{{ row.temperature }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="ph" label="pH值" min-width="100" align="right">
          <template #default="{ row }">
            <span :class="getPhClass(row.ph)" style="padding-right: 15px;">{{ row.ph }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="ec" label="EC值(mS/cm)" min-width="130" align="right">
          <template #default="{ row }">
            <span style="padding-right: 15px;">{{ row.ec }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="nitrogen" label="氮(mg/kg)" min-width="120" align="right">
          <template #default="{ row }">
            <span style="padding-right: 15px;">{{ row.nitrogen }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="phosphorus" label="磷(mg/kg)" min-width="120" align="right">
          <template #default="{ row }">
            <span style="padding-right: 15px;">{{ row.phosphorus }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="potassium" label="钾(mg/kg)" min-width="120" align="right">
          <template #default="{ row }">
            <span style="padding-right: 15px;">{{ row.potassium }}</span>
          </template>
        </el-table-column>

        <el-table-column label="健康状态" min-width="300" align="center" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; flex-wrap: wrap; gap: 4px; padding: 4px;">
              <el-tag v-if="row.rawData?.moistureStatus" :type="getStatusType(row.rawData.moistureStatus)" size="small">
                湿:{{ getStatusText(row.rawData.moistureStatus) }}
              </el-tag>
              <el-tag v-if="row.rawData?.temperatureStatus" :type="getStatusType(row.rawData.temperatureStatus)" size="small">
                温:{{ getStatusText(row.rawData.temperatureStatus) }}
              </el-tag>
              <el-tag v-if="row.rawData?.phStatus" :type="getStatusType(row.rawData.phStatus)" size="small">
                pH:{{ getStatusText(row.rawData.phStatus) }}
              </el-tag>
              <el-tag v-if="row.rawData?.ecStatus" :type="getStatusType(row.rawData.ecStatus)" size="small">
                EC:{{ getStatusText(row.rawData.ecStatus) }}
              </el-tag>
              <el-tag v-if="row.rawData?.nitrogenStatus" :type="getStatusType(row.rawData.nitrogenStatus)" size="small">
                氮:{{ getStatusText(row.rawData.nitrogenStatus) }}
              </el-tag>
              <el-tag v-if="row.rawData?.fertilityStatus" :type="getStatusType(row.rawData.fertilityStatus)" size="small">
                肥:{{ getStatusText(row.rawData.fertilityStatus) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total"
          :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next"
          @size-change="(s) => handlePageChange(currentPage, s)"
          @current-change="(p) => handlePageChange(p, pageSize)" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Download } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { soilApi } from '@/api'

// 响应式数据
const selectedSensor = ref('')
const dateRange = ref([])
const chartType = ref('day')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(100)
const updateTime = ref('')
const loading = ref(false)

// 设备列表（从传感器表获取）
const sensors = ref([])
const fields = ref([])

const gaugeData = reactive([
  {
    name: '土壤湿度',
    value: 0,
    unit: '%',
    percentage: 0,
    min: 30,
    max: 70,
    color: '#52c41a',
    status: 'success',
    statusText: '适宜'
  },
  {
    name: '土壤温度',
    value: 0,
    unit: '°C',
    percentage: 0,
    min: 15,
    max: 30,
    color: '#1890ff',
    status: 'success',
    statusText: '正常'
  },
  {
    name: 'pH值',
    value: 0,
    unit: '',
    percentage: 0,
    min: 6.0,
    max: 7.5,
    color: '#52c41a',
    status: 'success',
    statusText: '适宜'
  },
  {
    name: 'EC值',
    value: 0,
    unit: 'mS/cm',
    percentage: 0,
    min: 0,
    max: 3,
    color: '#1890ff',
    status: 'success',
    statusText: '正常'
  },
  {
    name: '氮含量',
    value: 0,
    unit: 'mg/kg',
    percentage: 0,
    min: 50,
    max: 150,
    color: '#faad14',
    status: 'warning',
    statusText: '偏低'
  },
  {
    name: '磷钾含量',
    value: 0,
    unit: 'mg/kg',
    percentage: 0,
    min: 80,
    max: 200,
    color: '#52c41a',
    status: 'success',
    statusText: '适宜'
  }
])

const tableData = ref([])

// 图表引用
const trendChart = ref(null)
const radarChart = ref(null)
let trendChartInstance = null
let radarChartInstance = null

// 初始化趋势图
const initTrendChart = () => {
  if (!trendChart.value) return

  trendChartInstance = echarts.init(trendChart.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: (params) => {
        let result = params[0]?.name + '<br/>'
        params.forEach(param => {
          let unit = ''
          if (param.seriesName === '土壤湿度') unit = '%'
          else if (param.seriesName === '土壤温度') unit = '°C'
          else if (param.seriesName === 'pH值') unit = ''
          else if (param.seriesName === 'EC值') unit = 'mS/cm'
          else if (param.seriesName === '氮含量') unit = 'mg/kg'
          else if (param.seriesName === '磷钾含量') unit = 'mg/kg'

          result += `${param.marker} ${param.seriesName}: ${param.value?.toFixed(2)}${unit}<br/>`
        })
        return result
      }
    },
    legend: {
      data: ['土壤湿度', '土壤温度', 'pH值', 'EC值', '氮含量', '磷钾含量'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '20%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: [],
      axisLine: { lineStyle: { color: '#d9d9d9' } },
      axisLabel: { color: '#8c8c8c' }
    },
    yAxis: [
      {
        type: 'value',
        name: '湿度(%)',
        position: 'left',
        axisLine: { show: true, lineStyle: { color: '#52c41a' } },
        axisLabel: { color: '#52c41a' },
        splitLine: { lineStyle: { color: '#f0f0f0' } }
      },
      {
        type: 'value',
        name: '温度/EC',
        position: 'right',
        offset: 40,
        axisLine: { show: true, lineStyle: { color: '#1890ff' } },
        axisLabel: { color: '#1890ff' },
        splitLine: { show: false }
      },
      {
        type: 'value',
        name: '养分(mg/kg)',
        position: 'right',
        axisLine: { show: true, lineStyle: { color: '#722ed1' } },
        axisLabel: { color: '#722ed1' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '土壤湿度',
        type: 'line',
        smooth: true,
        data: [],
        itemStyle: { color: '#52c41a' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(82, 196, 26, 0.3)' },
            { offset: 1, color: 'rgba(82, 196, 26, 0.05)' }
          ])
        }
      },
      {
        name: '土壤温度',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: [],
        itemStyle: { color: '#1890ff' }
      },
      {
        name: 'pH值',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: [],
        itemStyle: { color: '#faad14' },
        lineStyle: { type: 'dashed' }
      },
      {
        name: 'EC值',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: [],
        itemStyle: { color: '#13c2c2' },
        lineStyle: { type: 'dotted' }
      },
      {
        name: '氮含量',
        type: 'line',
        smooth: true,
        yAxisIndex: 2,
        data: [],
        itemStyle: { color: '#722ed1' }
      },
      {
        name: '磷钾含量',
        type: 'line',
        smooth: true,
        yAxisIndex: 2,
        data: [],
        itemStyle: { color: '#eb2f96' },
        lineStyle: { type: 'dashed' }
      }
    ]
  }

  trendChartInstance.setOption(option)
}

// 初始化雷达图
const initRadarChart = () => {
  if (!radarChart.value) return

  radarChartInstance = echarts.init(radarChart.value)
  updateRadarChart()
}

async function updateRadarChart() {
  if (!radarChartInstance || sensors.value.length === 0) return

  try {
    // 获取所有设备的实时数据，最多10个
    const selectedSensors = sensors.value.slice(0, 10)
    const realtimeDataList = await Promise.all(
      selectedSensors.map(s => soilApi.getRealTimeData(s.id).catch(() => null))
    )

    // 计算评分
    const radarData = selectedSensors.map((sensor, index) => {
      const data = realtimeDataList[index]
      if (!data) {
        return {
          value: [0, 0, 0, 0, 0],
          name: sensor.deviceCode,
          itemStyle: { color: getRadarColor(index) },
          areaStyle: { opacity: 0.2 },
          rawData: null
        }
      }

      // 各指标评分计算
      const moistureScore = Math.min(100, (data.moisture ?? 0)) // 湿度0-100
      const phScore = data.ph ? Math.max(0, 100 - Math.abs(data.ph - 6.5) * 20) : 0 // pH以6.5为最佳
      const tempScore = data.temperature ? Math.min(100, (data.temperature / 40) * 100) : 0 // 温度0-40
      const ecScore = data.ec ? Math.min(100, (data.ec / 3) * 100) : 0 // EC 0-3
      const fertilityScore = data.nitrogen && data.phosphorus && data.potassium
        ? Math.min(100, ((data.nitrogen + data.phosphorus + data.potassium) / 300) * 100)
        : 0 // N+P+K 总和0-300

      return {
        value: [moistureScore, phScore, fertilityScore, tempScore, ecScore],
        name: sensor.deviceCode,
        itemStyle: { color: getRadarColor(index) },
        areaStyle: { opacity: 0.2 },
        rawData: data
      }
    })

    const option = {
      tooltip: {
        formatter: (params) => {
          const dataItem = params.data
          const rawData = dataItem?.rawData
          let result = `<strong>${dataItem.name}</strong><br/>`

          result += `湿度评分: ${dataItem.value[0].toFixed(1)} (${rawData?.moisture?.toFixed(1)}%)<br/>`
          result += `pH适宜度: ${dataItem.value[1].toFixed(1)} (${rawData?.ph?.toFixed(1)})<br/>`
          result += `肥力综合: ${dataItem.value[2].toFixed(1)} (N:${rawData?.nitrogen?.toFixed(1) || 0} P:${rawData?.phosphorus?.toFixed(1) || 0} K:${rawData?.potassium?.toFixed(1) || 0})<br/>`
          result += `温度适宜: ${dataItem.value[3].toFixed(1)} (${rawData?.temperature?.toFixed(1)}°C)<br/>`
          result += `EC健康度: ${dataItem.value[4].toFixed(1)} (${rawData?.ec?.toFixed(1)} mS/cm)`

          return result
        }
      },
      legend: {
        data: selectedSensors.map(s => s.deviceCode),
        bottom: 0
      },
      radar: {
        indicator: [
          { name: '湿度评分', max: 100 },
          { name: 'pH适宜度', max: 100 },
          { name: '肥力综合', max: 100 },
          { name: '温度适宜', max: 100 },
          { name: 'EC健康度', max: 100 }
        ],
        radius: '65%',
        splitNumber: 4,
        axisName: { color: '#8c8c8c' },
        splitLine: { lineStyle: { color: '#e8e8e8' } },
        splitArea: { areaStyle: { color: ['#f8f8f8', '#fff'] } }
      },
      series: [{ type: 'radar', data: radarData }]
    }

    radarChartInstance.setOption(option, true)
  } catch (e) {
    console.error('更新雷达图失败', e)
  }
}

function getRadarColor(index) {
  const colors = ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16', '#a0d911', '#2f54ed']
  return colors[index % colors.length]
}

// 方法
async function loadSensors() {
  try {
    const sensorList = await soilApi.getSensors()
    sensors.value = sensorList || []
    if (sensors.value.length > 0 && !selectedSensor.value) {
      selectedSensor.value = sensors.value[0].id
    }
  } catch (e) {
    console.error('加载传感器失败', e)
  }
}

function updateGaugeStatus(index, value) {
  const gauge = gaugeData[index]
  if (value < gauge.min) {
    gauge.status = 'warning'
    gauge.statusText = '偏低'
    gauge.color = '#faad14'
  } else if (value > gauge.max) {
    gauge.status = 'danger'
    gauge.statusText = '过高'
    gauge.color = '#f5222d'
  } else {
    gauge.status = 'success'
    gauge.statusText = '适宜'
    gauge.color = '#52c41a'
  }
}

// 转换状态为 el-tag 可用的类型
function getTagType(status) {
  if (status === 'danger') return 'error'
  return status
}

async function loadOverview() {
  if (!selectedSensor.value) return
  loading.value = true
  try {
    const realtime = await soilApi.getRealTimeData(selectedSensor.value)
    if (realtime) {
      // 更新湿度
      gaugeData[0].value = realtime.moisture ?? 0
      gaugeData[0].percentage = Math.round(realtime.moisture ?? 0)
      updateGaugeStatus(0, realtime.moisture ?? 0)

      // 更新温度
      gaugeData[1].value = realtime.temperature ?? 0
      gaugeData[1].percentage = Math.round(((realtime.temperature ?? 0) / 40) * 100)
      updateGaugeStatus(1, realtime.temperature ?? 0)

      // 更新pH
      gaugeData[2].value = realtime.ph ?? 0
      gaugeData[2].percentage = Math.round(((realtime.ph ?? 0) / 10) * 100)
      updateGaugeStatus(2, realtime.ph ?? 0)

      // 更新EC
      gaugeData[3].value = realtime.ec ?? 0
      gaugeData[3].percentage = Math.round(((realtime.ec ?? 0) / 3) * 100)
      updateGaugeStatus(3, realtime.ec ?? 0)

      // 更新氮含量
      gaugeData[4].value = realtime.nitrogen ?? 0
      gaugeData[4].percentage = Math.round(((realtime.nitrogen ?? 0) / 150) * 100)
      updateGaugeStatus(4, realtime.nitrogen ?? 0)

      // 更新磷钾含量
      gaugeData[5].value = (realtime.phosphorus ?? 0) + (realtime.potassium ?? 0)
      gaugeData[5].percentage = Math.round((((realtime.phosphorus ?? 0) + (realtime.potassium ?? 0)) / 200) * 100)
      updateGaugeStatus(5, (realtime.phosphorus ?? 0) + (realtime.potassium ?? 0))

      updateTime.value = realtime.collectTime
        ? realtime.collectTime.replace('T', ' ').substring(0, 19)
        : new Date().toLocaleString()
    }
  } catch (e) {
    ElMessage.error('加载实时数据失败')
  } finally {
    loading.value = false
  }
}

async function loadHistoryData() {
  if (!selectedSensor.value) return
  try {
    const sensorId = selectedSensor.value
    const now = new Date()
    const end = now.toISOString()
    const start = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000).toISOString()

    const history = await soilApi.getHistoryDataPage(sensorId, start, end, currentPage.value, pageSize.value)
    if (history && history.list) {
      tableData.value = (history.list || []).map(item => ({
        time: item.collectTime ? item.collectTime.replace('T', ' ').substring(0, 19) : '',
        moisture: item.moisture ?? 0,
        temperature: item.temperature ?? 0,
        ph: item.ph ?? 0,
        ec: item.ec ?? 0,
        nitrogen: item.nitrogen ?? 0,
        phosphorus: item.phosphorus ?? 0,
        potassium: item.potassium ?? 0,
        healthStatus: item.healthStatus,
        // 保存原始数据用于详细状态显示
        rawData: item
      }))
      total.value = history.total ?? 0
    }
  } catch (e) {
    console.error('加载历史数据失败', e)
    ElMessage.error('加载历史数据失败')
  }
}

// 状态映射函数
function getStatusText(status) {
  switch (status) {
    case 'optimal': return '最佳'
    case 'good': return '良好'
    case 'poor': return '较差'
    default: return '未知'
  }
}

function getStatusType(status) {
  switch (status) {
    case 'optimal': return 'success'
    case 'good': return 'primary'
    case 'poor': return 'warning'
    default: return 'info'
  }
}

// 分页变化处理
function handlePageChange(page, size) {
  currentPage.value = page
  if (size && size !== pageSize.value) {
    pageSize.value = size
  }
  loadHistoryData()
}

async function loadTrendData() {
  if (!selectedSensor.value) return
  try {
    const sensorId = selectedSensor.value
    const days = chartType.value === 'day' ? 1 : chartType.value === 'week' ? 7 : 30
    const trend = await soilApi.getTrend(sensorId, days)

    if (trend) {
      initTrendChartWithData(trend)
    }
  } catch (e) {
    // 失败时使用默认图表
    initTrendChart()
  }
}

function initTrendChartWithData(trend) {
  if (!trendChart.value) return

  if (!trendChartInstance) {
    trendChartInstance = echarts.init(trendChart.value)
  }

  const moistureList = trend.moisture || []
  const tempList = trend.temperature || []
  const phList = trend.ph || []
  const ecList = trend.ec || []
  const nitrogenList = trend.nitrogen || []
  const npkList = trend.npk || []

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: (params) => {
        let result = params[0].name + '<br/>'
        params.forEach(param => {
          let unit = ''
          if (param.seriesName === '土壤湿度') unit = '%'
          else if (param.seriesName === '土壤温度') unit = '°C'
          else if (param.seriesName === 'pH值') unit = ''
          else if (param.seriesName === 'EC值') unit = 'mS/cm'
          else if (param.seriesName === '氮含量') unit = 'mg/kg'
          else if (param.seriesName === '磷钾含量') unit = 'mg/kg'

          result += `${param.marker} ${param.seriesName}: ${param.value.toFixed(2)}${unit}<br/>`
        })
        return result
      }
    },
    legend: { data: ['土壤湿度', '土壤温度', 'pH值', 'EC值', '氮含量', '磷钾含量'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '20%', containLabel: true },
    xAxis: {
      type: 'category',
      data: trend._timeLabels || Array.from({ length: moistureList.length }, (_, i) => `${i}:00`),
      axisLine: { lineStyle: { color: '#d9d9d9' } },
      axisLabel: { color: '#8c8c8c' }
    },
    yAxis: [
      { type: 'value', name: '湿度(%)', position: 'left', axisLine: { show: true, lineStyle: { color: '#52c41a' } }, axisLabel: { color: '#52c41a' }, splitLine: { lineStyle: { color: '#f0f0f0' } } },
      { type: 'value', name: '温度/EC', position: 'right', offset: 40, axisLine: { show: true, lineStyle: { color: '#1890ff' } }, axisLabel: { color: '#1890ff' }, splitLine: { show: false } },
      { type: 'value', name: '养分(mg/kg)', position: 'right', axisLine: { show: true, lineStyle: { color: '#722ed1' } }, axisLabel: { color: '#722ed1' }, splitLine: { show: false } }
    ],
    series: [
      { name: '土壤湿度', type: 'line', smooth: true, data: moistureList, itemStyle: { color: '#52c41a' }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(82, 196, 26, 0.3)' }, { offset: 1, color: 'rgba(82, 196, 26, 0.05)' }]) } },
      { name: '土壤温度', type: 'line', smooth: true, yAxisIndex: 1, data: tempList, itemStyle: { color: '#1890ff' } },
      { name: 'pH值', type: 'line', smooth: true, yAxisIndex: 1, data: phList, itemStyle: { color: '#faad14' }, lineStyle: { type: 'dashed' } },
      { name: 'EC值', type: 'line', smooth: true, yAxisIndex: 1, data: ecList, itemStyle: { color: '#13c2c2' }, lineStyle: { type: 'dotted' } },
      { name: '氮含量', type: 'line', smooth: true, yAxisIndex: 2, data: nitrogenList, itemStyle: { color: '#722ed1' } },
      { name: '磷钾含量', type: 'line', smooth: true, yAxisIndex: 2, data: npkList, itemStyle: { color: '#eb2f96' }, lineStyle: { type: 'dashed' } }
    ]
  }

  trendChartInstance.setOption(option)
}

const refreshData = async () => {
  await loadOverview()
  await loadHistoryData()
  ElMessage.success('数据已刷新')
}

const exportData = async () => {
  if (!selectedSensor.value) return
  try {
    const sensorId = selectedSensor.value
    const now = new Date()
    const end = now.toISOString()
    const start = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000).toISOString()

    const data = await soilApi.getHistoryData(sensorId, start, end)
    if (!data || data.length === 0) {
      ElMessage.warning('暂无数据可导出')
      return
    }

    const csv = ['时间,湿度,温度,pH值,EC值,氮,磷,钾']
    data.forEach(row => {
      csv.push([row.collectTime, row.moisture, row.temperature, row.ph, row.ec, row.nitrogen, row.phosphorus, row.potassium].join(','))
    })

    const blob = new Blob(['﻿' + csv.join('\n')], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `土壤数据_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(url)

    ElMessage.success('数据导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

const getMoistureClass = (val) => {
  if (val < 30) return 'text-danger'
  if (val > 70) return 'text-warning'
  return 'text-success'
}

const getPhClass = (val) => {
  if (val < 6.0 || val > 7.5) return 'text-warning'
  return 'text-success'
}

onMounted(async () => {
  initTrendChart()
  initRadarChart()

  await loadSensors()
  await updateRadarChart()

  window.addEventListener('resize', () => {
    trendChartInstance?.resize()
    radarChartInstance?.resize()
  })
})

watch(chartType, () => {
  loadTrendData()
})

watch(selectedSensor, () => {
  loadOverview()
  loadHistoryData()
  loadTrendData()
})
</script>

<style scoped>
.soil-monitor {
  padding: 0;
}

.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
}

.section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title h3 {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin: 0;
}

/* 仪表盘网格 */
.gauge-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 20px;
}

.gauge-item {
  text-align: center;
}

.gauge-chart {
  position: relative;
  display: inline-block;
}

.gauge-value-container {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.gauge-value {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}

.gauge-unit {
  font-size: 12px;
  font-weight: 400;
  color: #8c8c8c;
  margin-top: 2px;
}

.gauge-info {
  margin-top: 12px;
}

.gauge-name {
  font-size: 14px;
  color: #262626;
  font-weight: 500;
}

.gauge-range {
  font-size: 12px;
  color: #8c8c8c;
  margin: 4px 0;
}

/* 趋势图 */
.trend-chart {
  height: 400px;
}

/* 雷达图区域 */
.radar-chart-container {
  width: 100%;
}

.radar-chart {
  height: 400px;
}

/* 状态颜色 */
.text-success {
  color: #52c41a;
}

.text-warning {
  color: #faad14;
}

.text-danger {
  color: #f5222d;
}

/* 分页 */
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1400px) {
  .gauge-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>