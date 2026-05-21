<template>
  <div class="dashboard">
    <!-- 顶部KPI指标卡 -->
    <div class="stats-row">
      <div
        v-for="(stat, index) in statsList"
        :key="stat.key"
        class="stat-card"
        :class="[stat.type, { 'skeleton': loading }]"
        :style="{ animationDelay: `${index * 0.1}s` }"
      >
        <div class="stat-icon">
          <el-icon><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">
            <span v-if="!loading">{{ stat.value }}</span>
            <el-skeleton-item v-else variant="text" style="width: 60px; height: 32px" />
            <span v-if="!loading" class="unit">{{ stat.unit }}</span>
          </div>
          <div class="stat-label">{{ stat.label }}</div>
          <div v-if="stat.trend && !loading" class="stat-trend" :class="stat.trend > 0 ? 'up' : 'down'">
            <el-icon><component :is="stat.trend > 0 ? ArrowUp : ArrowDown" /></el-icon>
            <span>{{ Math.abs(stat.trend) }}%</span>
            <span class="trend-text">较昨日</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 主体内容区 -->
    <div class="main-grid">
      <!-- 左侧：高德地图+地块分布 -->
      <div class="left-section">
        <div class="card map-card" :class="{ 'loading': mapLoading }">
          <div class="card-header">
            <h3>种植基地分布</h3>
            <div class="header-actions">
              <el-tag type="success" class="area-tag">{{ fieldStats.totalArea }}亩</el-tag>
              <el-button 
                type="primary" 
                size="small" 
                :icon="Refresh"
                circle
                @click="refreshMap"
                :loading="mapLoading"
              />
            </div>
          </div>
          <div class="filter-bar">
            <el-select v-model="filterCrop" placeholder="作物筛选" clearable size="small" style="width: 120px;">
              <el-option label="全部" value="" />
              <el-option label="水稻" value="水稻" />
              <el-option label="小麦" value="小麦" />
              <el-option label="玉米" value="玉米" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="状态筛选" clearable size="small" style="width: 120px;">
              <el-option label="全部" value="" />
              <el-option label="正常" value="normal" />
              <el-option label="干旱" value="dry" />
              <el-option label="过湿" value="wet" />
              <el-option label="需施肥" value="fertilize" />
            </el-select>
            <el-button type="primary" size="small" @click="resetFilters">重置</el-button>
          </div>
          <div class="card-body">
            <!-- 真实高德地图 -->
            <AMap :fields="filteredFields" @loaded="onMapLoaded" />
          </div>
        </div>
      </div>

      <!-- 右侧：烘干+能效+动态 -->
      <div class="right-section">
        <!-- 烘干实时监测 -->
        <div class="card drying-card">
          <div class="card-header">
            <h3>
              <el-icon class="header-icon"><HotWater /></el-icon>
              烘干实时监测
            </h3>
            <el-button type="primary" size="small" @click="$router.push('/drying')">
              查看更多
              <el-icon class="btn-icon"><ArrowRight /></el-icon>
            </el-button>
          </div>
          <div class="card-body">
            <div v-if="displayDryingBatches.length === 0" class="empty-state">
              <el-empty description="暂无进行中的烘干批次" :image-size="80">
                <el-button type="primary" size="small" @click="$router.push('/drying')">
                  创建批次
                </el-button>
              </el-empty>
            </div>
            <div
              v-for="(batch, index) in displayDryingBatches"
              :key="batch.id"
              class="batch-item"
              :class="{ 'completed': batch.status === 'completed', 'pulse': batch.status === 'running' }"
              :style="{ animationDelay: `${index * 0.15}s` }"
            >
              <div class="batch-header">
                <div class="batch-info">
                  <span class="batch-id">{{ batch.id }}</span>
                  <el-tag
                    :type="getBatchTagType(batch.status)"
                    size="small"
                    effect="light"
                  >
                    {{ batch.stage }}
                  </el-tag>
                </div>
                <span class="batch-progress-text">{{ batch.progress }}%</span>
              </div>

              <div class="batch-progress">
                <el-progress
                  :percentage="batch.progress"
                  :status="batch.status === 'completed' ? 'success' : ''"
                  :stroke-width="10"
                  :show-text="false"
                  :color="getProgressColor(batch.progress)"
                />
                <div class="progress-label">
                  <span>目标含水 {{ batch.targetMoisture }}%</span>
                  <span class="divider">|</span>
                  <span :class="{ 'highlight': batch.currentMoisture > batch.targetMoisture }">
                    当前 {{ batch.currentMoisture }}%
                  </span>
                </div>
              </div>

              <div class="batch-params">
                <span class="param-item">
                  <el-icon><HotWater /></el-icon>
                  {{ batch.temperature }}°C
                </span>
                <span class="param-item">
                  <el-icon><Timer /></el-icon>
                  {{ batch.duration }}分钟
                </span>
                <span class="param-item">
                  <el-icon><ScaleToOriginal /></el-icon>
                  {{ batch.weight }}kg
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 今日能效 -->
        <div class="card energy-card">
          <div class="card-header">
            <h3>
              <el-icon class="header-icon"><Lightning /></el-icon>
              今日能效
            </h3>
            <el-tooltip content="实时更新" placement="top">
              <el-icon class="refresh-icon" :class="{ 'rotating': energyRefreshing }"><Refresh /></el-icon>
            </el-tooltip>
          </div>
          <div class="card-body">
            <div class="energy-chart">
              <div class="ring-chart" :style="ringStyle">
                <div class="ring-inner">
                  <div class="ring-value" :class="{ 'pulse-once': energyRefreshing }">
                    {{ energyData.greenPower }}
                    <span class="percent">%</span>
                  </div>
                  <div class="ring-label">绿电占比</div>
                </div>
              </div>

              <div class="energy-legend">
                <div class="legend-item">
                  <span class="dot green"></span>
                  <div class="legend-info">
                    <div class="legend-label">光伏用电</div>
                    <div class="legend-value">
                      {{ energyData.solarUsage }}
                      <span class="unit">kWh</span>
                    </div>
                  </div>
                </div>
                <div class="legend-item">
                  <span class="dot gray"></span>
                  <div class="legend-info">
                    <div class="legend-label">电网用电</div>
                    <div class="legend-value">
                      {{ energyData.gridUsage }}
                      <span class="unit">kWh</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="carbon-info" :class="{ 'highlight': energyData.treesEquivalent > 10 }">
              <el-icon><Sunny /></el-icon>
              <span>相当于植树</span>
              <strong class="trees-count">{{ energyData.treesEquivalent }}</strong>
              <span>棵</span>
            </div>
          </div>
        </div>

        <!-- 实时动态 -->
        <div class="card activity-card">
          <div class="card-header">
            <h3>
              <el-icon class="header-icon"><Bell /></el-icon>
              实时动态
            </h3>
            <el-tag size="small" type="info">{{ activities.length }}条新消息</el-tag>
          </div>
          <div class="card-body">
            <div class="activity-list" ref="activityList">
              <div
                v-for="(item, index) in activities"
                :key="item.id"
                class="activity-item"
                :class="{ 'new': index < 3 }"
                :style="{ animationDelay: `${index * 0.1}s` }"
              >
                <div class="activity-icon" :class="item.type">
                  <el-icon>
                    <Check v-if="item.type === 'success'" />
                    <Warning v-else-if="item.type === 'warning'" />
                    <InfoFilled v-else />
                  </el-icon>
                </div>

                <div class="activity-content">
                  <div class="activity-text">{{ item.content }}</div>
                  <div class="activity-meta">
                    <span class="location">{{ item.location }}</span>
                    <span class="dot-separator">·</span>
                    <span class="time">{{ formatTime(item.time) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import AMap from '@/components/AMap.vue'
import {
  FirstAidKit, Check, WarningFilled, HotWater, Timer,
  Lightning, InfoFilled, Warning, Sunny, ArrowRight,
  Refresh, Bell, ScaleToOriginal, ArrowUp, ArrowDown
} from '@element-plus/icons-vue'

const dashboardStore = useDashboardStore()
const { stats, activities, dryingBatches, fieldDistribution, energyData, loadAllData } = dashboardStore

// 只显示最新4条烘干数据
const displayDryingBatches = computed(() => {
  return (dryingBatches || []).slice(0, 4)
})

// 筛选条件
const filterCrop = ref('')
const filterStatus = ref('')

const filteredFields = computed(() => {
    if (!fieldDistribution || !Array.isArray(fieldDistribution)) {
      return []
    }
    return fieldDistribution.filter(field => {
      if (filterCrop.value && field.crop !== filterCrop.value) return false
      if (filterStatus.value && field.status !== filterStatus.value) return false
      return true
    })
  })

const resetFilters = () => {
  filterCrop.value = ''
  filterStatus.value = ''
}

// 加载状态
const loading = ref(true)
const mapLoading = ref(true)
const energyRefreshing = ref(false)

// 定时器
let energyTimer = null
let dataTimer = null

const statsList = computed(() => [
  {
    key: 'land',
    value: stats.totalLandArea,
    unit: '亩',
    label: '总种植面积',
    icon: FirstAidKit,
    type: 'primary'
  },
  {
    key: 'device',
    value: `${stats.deviceOnline}/${stats.deviceTotal}`,
    unit: '',
    label: '设备在线/总数',
    icon: Check,
    type: stats.deviceOnline / stats.deviceTotal > 0.8 ? 'success' : 'warning'
  },
  {
    key: 'drying',
    value: stats.dryingBatches,
    unit: '批次',
    label: '烘干进行中',
    icon: HotWater,
    type: 'primary'
  },
  {
    key: 'power',
    value: stats.powerUsage,
    unit: 'kWh',
    label: '今日用电量',
    icon: Lightning,
    type: 'warning',
    trend: -12
  },
  {
    key: 'carbon',
    value: stats.carbonReduction,
    unit: '吨',
    label: '累计碳减排',
    icon: Sunny,
    type: 'success',
    trend: 8
  },
  {
    key: 'alert',
    value: stats.pendingAlerts,
    unit: '条',
    label: '待处理告警',
    icon: WarningFilled,
    type: stats.pendingAlerts > 0 ? 'danger' : 'success'
  }
])

const fieldStats = computed(() => ({
    totalArea: (filteredFields.value || []).reduce((sum, f) => sum + (f.area || 0), 0)
  }))

// 环形图样式
const ringStyle = computed(() => ({
  background: `conic-gradient(
    #52c41a 0deg calc(3.6deg * ${energyData.greenPower}),
    #d9d9d9 calc(3.6deg * ${energyData.greenPower}) 360deg
  )`
}))

// 获取批次标签类型
const getBatchTagType = (status) => {
  const map = {
    'running': 'primary',
    'completed': 'success',
    'paused': 'warning',
    'pending': 'info'
  }
  return map[status] || 'info'
}

// 获取进度条颜色
const getProgressColor = (progress) => {
  if (progress >= 80) return '#52c41a'
  if (progress >= 50) return '#1890ff'
  return '#faad14'
}

// 格式化时间
const formatTime = (timeStr) => {
  const now = new Date()
  const time = new Date(timeStr)
  const diff = Math.floor((now - time) / 1000 / 60) // 分钟
  
  if (diff < 1) return '刚刚'
  if (diff < 60) return `${diff}分钟前`
  if (diff < 1440) return `${Math.floor(diff / 60)}小时前`
  return timeStr.split(' ')[1] || timeStr
}

// 地图加载完成
const onMapLoaded = () => {
  mapLoading.value = false
}

// 刷新地图
const refreshMap = () => {
  mapLoading.value = true
  setTimeout(() => {
    mapLoading.value = false
  }, 800)
}

// 刷新能效数据
const refreshEnergy = () => {
  energyRefreshing.value = true
  setTimeout(() => {
    energyRefreshing.value = false
  }, 1000)
}

onMounted(() => {
  // 加载真实数据
  loadAllData()
  
  // 模拟加载完成
  setTimeout(() => {
    loading.value = false
  }, 600)
  
  // 每30秒刷新一次能效数据
  energyTimer = setInterval(refreshEnergy, 30000)
  
  // 每30秒刷新一次仪表盘数据
  dataTimer = setInterval(loadAllData, 30000)
})

onUnmounted(() => {
  if (energyTimer) clearInterval(energyTimer)
  if (dataTimer) clearInterval(dataTimer)
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

/* 统计卡片区 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0;
  animation: fadeInUp 0.5s ease forwards;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stat-card:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.stat-card.skeleton {
  opacity: 1;
  animation: none;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  transition: all 0.3s;
}

.stat-card:hover .stat-icon {
  transform: scale(1.1) rotate(5deg);
}

.stat-card.primary .stat-icon {
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
  color: #1890ff;
}

.stat-card.success .stat-icon {
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
  color: #52c41a;
}

.stat-card.warning .stat-icon {
  background: linear-gradient(135deg, #fffbe6 0%, #ffe58f 100%);
  color: #faad14;
}

.stat-card.danger .stat-icon {
  background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%);
  color: #f5222d;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #262626;
  line-height: 1;
  display: flex;
  align-items: baseline;
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
  margin-top: 8px;
}

.stat-trend {
  font-size: 12px;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}

.stat-trend.up {
  color: #52c41a;
}

.stat-trend.down {
  color: #f5222d;
}

.trend-text {
  color: #bfbfbf;
  font-weight: 400;
}

/* 主体网格 */
.main-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 20px;
}

.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  transition: box-shadow 0.3s;
}

.card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  color: #1890ff;
  font-size: 18px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-bar {
  padding: 12px 20px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  gap: 12px;
}

.area-tag {
  font-size: 14px;
  padding: 4px 12px;
}

.btn-icon {
  margin-left: 4px;
  transition: transform 0.3s;
}

.el-button:hover .btn-icon {
  transform: translateX(4px);
}

.card-body {
  padding: 20px;
  height: calc(100% - 60px);
}

/* 地图卡片 */
.map-card {
  height: 100%;
  min-height: 600px;
}

.map-card.loading {
  opacity: 0.7;
}

.map-card .card-body {
  padding: 0;
  overflow: hidden;
  height: calc(100% - 110px);
}

/* 右侧区域 */
.right-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 烘干卡片 */
.drying-card {
  flex: 1;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.batch-item {
  padding: 16px;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f5ff 100%);
  border-radius: 10px;
  margin-bottom: 12px;
  border: 1px solid transparent;
  transition: all 0.3s;
  opacity: 0;
  animation: fadeInUp 0.5s ease forwards;
}

.batch-item:hover {
  border-color: #1890ff;
  transform: translateX(4px);
}

.batch-item.completed {
  background: linear-gradient(135deg, #f6ffed 0%, #f0f9eb 100%);
  opacity: 0.8;
}

.batch-item.pulse {
  animation: fadeInUp 0.5s ease forwards, pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(24, 144, 255, 0.2);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(24, 144, 255, 0);
  }
}

.batch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.batch-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.batch-id {
  font-weight: 600;
  color: #262626;
  font-size: 15px;
}

.batch-progress-text {
  font-weight: 700;
  color: #1890ff;
  font-size: 16px;
}

.batch-progress {
  margin-bottom: 12px;
}

.progress-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-label .divider {
  color: #d9d9d9;
}

.progress-label .highlight {
  color: #faad14;
  font-weight: 500;
}

.batch-params {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #595959;
}

.param-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.param-item .el-icon {
  color: #1890ff;
  font-size: 14px;
}

/* 能效卡片 */
.refresh-icon {
  color: #8c8c8c;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.refresh-icon:hover {
  color: #1890ff;
}

.refresh-icon.rotating {
  animation: rotate 1s linear;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.energy-chart {
  display: flex;
  align-items: center;
  gap: 32px;
}

.ring-chart {
  width: 130px;
  height: 130px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: transform 0.3s;
}

.ring-chart:hover {
  transform: scale(1.05);
}

.ring-inner {
  width: 95px;
  height: 95px;
  background: #fff;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.05);
}

.ring-value {
  font-size: 26px;
  font-weight: 700;
  color: #52c41a;
  display: flex;
  align-items: baseline;
}

.ring-value .percent {
  font-size: 14px;
  margin-left: 2px;
}

.pulse-once {
  animation: numberPulse 0.5s ease;
}

@keyframes numberPulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}

.ring-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}

.energy-legend {
  flex: 1;
}

.energy-legend .legend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background 0.3s;
}

.energy-legend .legend-item:hover {
  background: #f6ffed;
}

.energy-legend .legend-item:last-child {
  margin-bottom: 0;
}

.energy-legend .dot {
  width: 14px;
  height: 14px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.dot.green {
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
}

.dot.gray {
  background: linear-gradient(135deg, #d9d9d9 0%, #bfbfbf 100%);
}

.legend-info .legend-label {
  font-size: 13px;
  color: #8c8c8c;
}

.legend-info .legend-value {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
  margin-top: 4px;
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.legend-value .unit {
  font-size: 12px;
  color: #8c8c8c;
  font-weight: 400;
}

.carbon-info {
  margin-top: 20px;
  padding: 14px;
  background: linear-gradient(135deg, #f6ffed 0%, #f0f9eb 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #52c41a;
  font-size: 14px;
  border: 1px solid #d9f7be;
  transition: all 0.3s;
}

.carbon-info:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.15);
}

.carbon-info.highlight {
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
  border-color: #52c41a;
}

.trees-count {
  font-size: 24px;
  color: #389e0d;
  font-weight: 700;
  margin: 0 4px;
}

/* 动态卡片 */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: 300px;
  overflow-y: auto;
  padding-right: 8px;
}

.activity-list::-webkit-scrollbar {
  width: 4px;
}

.activity-list::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

.activity-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 10px;
  transition: all 0.3s;
  opacity: 0;
  animation: fadeInUp 0.4s ease forwards;
}

.activity-item:hover {
  background: #f6ffed;
  transform: translateX(4px);
}

.activity-item.new {
  background: linear-gradient(90deg, #e6f7ff 0%, transparent 100%);
  border-left: 3px solid #1890ff;
}

.activity-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 18px;
  transition: transform 0.3s;
}

.activity-item:hover .activity-icon {
  transform: scale(1.1);
}

.activity-icon.success {
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
  color: #52c41a;
}

.activity-icon.warning {
  background: linear-gradient(135deg, #fffbe6 0%, #ffe58f 100%);
  color: #faad14;
}

.activity-icon.info {
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
  color: #1890ff;
}

.activity-content {
  flex: 1;
  min-width: 0;
}

.activity-text {
  font-size: 14px;
  color: #262626;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.activity-meta {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.activity-meta .location {
  color: #1890ff;
  font-weight: 500;
}

.activity-meta .dot-separator {
  color: #d9d9d9;
}

.activity-meta .time {
  color: #bfbfbf;
}

/* 响应式 */
@media (max-width: 1400px) {
  .stats-row {
    grid-template-columns: repeat(3, 1fr);
  }

  .main-grid {
    grid-template-columns: 1fr;
  }
  
  .map-card {
    min-height: 400px;
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .stat-card {
    padding: 16px;
  }
  
  .stat-value {
    font-size: 22px;
  }
  
  .energy-chart {
    flex-direction: column;
    gap: 20px;
  }
}
</style>
