<template>
  <div class="drying-page">
    <div class="page-header">
      <div class="header-left">
        <h2>烘干管理</h2>
        <el-tag type="info">当前运行中: {{ runningCount }} 批次</el-tag>
      </div>
      <el-button type="primary" size="large" @click="showCreateDialog = true">
        + 新建烘干批次
      </el-button>
    </div>

    <div class="main-grid">
      <div class="left-section">
        <div class="batch-list">
          <div v-for="batch in batches" :key="batch.id" class="batch-card" :class="{ active: selectedBatch?.id === batch.id, [batch.status]: true }" @click="selectBatch(batch)">
            <div class="batch-card-header">
              <div class="batch-id">{{ batch.id }}</div>
              <el-tag :type="getStatusType(batch.status)" size="small">{{ getStatusText(batch.status) }}</el-tag>
            </div>
            <div class="batch-card-body">
              <div class="batch-info-row">
                <span class="label">品种:</span>
                <span class="value">{{ batch.grainType }}</span>
              </div>
              <div class="batch-info-row">
                <span class="label">含水率:</span>
                <span class="value">{{ batch.currentMoisture }}% → {{ batch.targetMoisture }}%</span>
              </div>
              <div class="batch-info-row" v-if="batch.status === 'running'">
                <span class="label">当前阶段:</span>
                <span class="value" style="color: #1890ff; font-weight: 600;">{{ batch.currentStage }}</span>
              </div>
              <div class="batch-progress" v-if="batch.status === 'running'">
                <el-progress :percentage="batch.progress" :stroke-width="10" :status="batch.progress >= 100 ? 'success' : ''"></el-progress>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="right-section" v-if="selectedBatch">
        <!-- 工艺曲线 -->
        <div class="card">
          <div class="card-header">
            <h3>烘干工艺曲线</h3>
            <div class="header-actions">
              <el-button v-if="selectedBatch.status === 'running'" type="danger" size="small" @click="stopBatch">停止烘干</el-button>
              <el-button v-if="selectedBatch.status === 'pending'" type="primary" size="small" @click="startBatch">开始烘干</el-button>
            </div>
          </div>
          <div class="card-body">
            <div ref="processChart" class="process-chart"></div>
          </div>
        </div>

        <!-- 细化后的烘干阶段 -->
        <div class="card">
          <div class="card-header">
            <h3>烘干工艺阶段</h3>
            <el-tag type="info" v-if="selectedBatch.dryingMethod">{{ selectedBatch.dryingMethod }}</el-tag>
          </div>
          <div class="card-body">
            <div class="stage-detailed">
              <div v-for="(stage, index) in detailedStages" :key="stage.name" 
                   class="stage-item-detailed" 
                   :class="{ completed: index < currentDetailedStage, active: index === currentDetailedStage, pending: index > currentDetailedStage }">
                <div class="stage-header">
                  <div class="stage-icon-detailed">
                    <el-icon v-if="index < currentDetailedStage"><Check /></el-icon>
                    <span v-else-if="index === currentDetailedStage" class="pulse">{{ index + 1 }}</span>
                    <span v-else>{{ index + 1 }}</span>
                  </div>
                  <div class="stage-title">
                    <div class="stage-name">{{ stage.name }}</div>
                    <div class="stage-duration">{{ stage.duration }}</div>
                  </div>
                </div>
                <div class="stage-params" v-if="stage.params">
                  <div class="param-item" v-for="param in stage.params" :key="param.label">
                    <span class="param-label">{{ param.label }}:</span>
                    <span class="param-value" :class="param.status">{{ param.value }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 质量指标监测 -->
        <div class="card" v-if="selectedBatch.status === 'running' || selectedBatch.status === 'completed'">
          <div class="card-header">
            <h3>质量指标监测</h3>
          </div>
          <div class="card-body">
            <div class="quality-metrics">
              <div class="metric-item">
                <div class="metric-label">干燥不均匀度</div>
                <div class="metric-value" :class="getQualityStatus(selectedBatch.uniformity)">
                  {{ selectedBatch.uniformity }}%
                  <el-tag size="small" :type="getQualityTag(selectedBatch.uniformity)">{{ selectedBatch.uniformity < 3 ? '合格' : '偏高' }}</el-tag>
                </div>
                <div class="metric-range">标准: ≤3%</div>
              </div>
              <div class="metric-item" v-if="selectedBatch.grainType === '水稻'">
                <div class="metric-label">爆腰率增值</div>
                <div class="metric-value" :class="getQualityStatus(selectedBatch.brokenRate)">
                  {{ selectedBatch.brokenRate }}%
                  <el-tag size="small" :type="getQualityTag(selectedBatch.brokenRate)">{{ selectedBatch.brokenRate < 5 ? '合格' : '偏高' }}</el-tag>
                </div>
                <div class="metric-range">标准: ≤5%</div>
              </div>
              <div class="metric-item">
                <div class="metric-label">当前温度</div>
                <div class="metric-value">{{ selectedBatch.currentTemp }}°C</div>
                <div class="metric-range">目标: {{ selectedBatch.targetTemp }}°C</div>
              </div>
              <div class="metric-item">
                <div class="metric-label">升温速率</div>
                <div class="metric-value" :class="getHeatingRateStatus(selectedBatch.heatingRate)">
                  {{ selectedBatch.heatingRate }}°C/h
                </div>
                <div class="metric-range">限制: ≤3°C/h</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="showCreateDialog" title="新建烘干批次" width="600px">
      <el-form :model="createForm" label-width="120px">
        <el-form-item label="粮食品种">
          <el-select v-model="createForm.grainType" style="width: 100%" @change="onGrainTypeChange">
            <el-option label="水稻" value="水稻" />
            <el-option label="小麦" value="小麦" />
            <el-option label="玉米" value="玉米" />
          </el-select>
        </el-form-item>
        <el-form-item label="烘干方式">
          <el-radio-group v-model="createForm.dryingMethod">
            <el-radio label="热泵烘干">热泵烘干</el-radio>
            <el-radio label="生物质烘干">生物质烘干</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="初始含水率">
          <el-input-number v-model="createForm.initialMoisture" :min="15" :max="35" style="width: 100%" />
          <span class="unit">%</span>
        </el-form-item>
        <el-form-item label="目标含水率">
          <el-input-number v-model="createForm.targetMoisture" :min="10" :max="15" style="width: 100%" />
          <span class="unit">%</span>
        </el-form-item>
        <el-form-item label="装载量">
          <el-input-number v-model="createForm.loadAmount" :min="1" :max="50" style="width: 100%" />
          <span class="unit">吨</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createBatch" :loading="creating">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const showCreateDialog = ref(false)
const creating = ref(false)
const selectedBatch = ref(null)
const processChart = ref(null)
let processChartInstance = null

const batches = ref([
  { 
    id: 'DH20240320001', 
    grainType: '水稻', 
    dryingMethod: '热泵烘干',
    status: 'running', 
    initialMoisture: 25.5, 
    currentMoisture: 15.2, 
    targetMoisture: 13.5, 
    progress: 65,
    currentStage: '恒速干燥阶段',
    currentTemp: 54,
    targetTemp: 55,
    heatingRate: 2.5,
    uniformity: 2.1,
    brokenRate: 3.2,
    loadAmount: 15
  },
  { 
    id: 'DH20240319002', 
    grainType: '小麦', 
    dryingMethod: '生物质烘干',
    status: 'completed', 
    initialMoisture: 22.0, 
    currentMoisture: 13.2, 
    targetMoisture: 13.5, 
    progress: 100,
    currentStage: '出仓',
    uniformity: 1.8,
    brokenRate: 2.1,
    loadAmount: 20
  },
  { 
    id: 'DH20240318003', 
    grainType: '玉米', 
    dryingMethod: '热泵烘干',
    status: 'pending', 
    initialMoisture: 28.0, 
    currentMoisture: 28.0, 
    targetMoisture: 14.0, 
    progress: 0,
    loadAmount: 18
  }
])

const createForm = reactive({ 
  grainType: '', 
  dryingMethod: '热泵烘干',
  initialMoisture: 25, 
  targetMoisture: 13.5,
  loadAmount: 15
})

// 细化的烘干工艺阶段（专家级）
const detailedStages = [
  { 
    name: '入仓准备', 
    duration: '30-60分钟',
    params: [
      { label: '操作', value: '清理杂质、分样检测', status: 'normal' },
      { label: '要求', value: '含水率检测3点取样', status: 'normal' }
    ]
  },
  { 
    name: '预热阶段', 
    duration: '15-20分钟',
    params: [
      { label: '温度', value: '≤45°C', status: 'warning' },
      { label: '升温速率', value: '≤3°C/h', status: 'normal' },
      { label: '目的', value: '设备预热、粮食升温', status: 'normal' }
    ]
  },
  { 
    name: '升温阶段', 
    duration: '1-2小时',
    params: [
      { label: '温度', value: '45→55°C', status: 'warning' },
      { label: '升温速率', value: '严格≤3°C/h', status: 'danger' },
      { label: '监测', value: '每小时检测含水率', status: 'normal' }
    ]
  },
  { 
    name: '恒速干燥', 
    duration: '4-8小时',
    params: [
      { label: '温度', value: '55±2°C', status: 'warning' },
      { label: '目标', value: '降水率1-1.5%/h', status: 'normal' },
      { label: '重点', value: '干燥不均匀度≤3%', status: 'danger' }
    ]
  },
  { 
    name: '缓苏阶段', 
    duration: '2-4小时',
    params: [
      { label: '温度', value: '停止加热', status: 'success' },
      { label: '目的', value: '水分均衡、应力释放', status: 'normal' },
      { label: '检测', value: '爆腰率监测（稻谷）', status: 'warning' }
    ]
  },
  { 
    name: '冷却阶段', 
    duration: '1-2小时',
    params: [
      { label: '温度', value: '降至环境温度+5°C', status: 'success' },
      { label: '目标', value: '准备出仓', status: 'normal' }
    ]
  },
  { 
    name: '出仓质检', 
    duration: '30-60分钟',
    params: [
      { label: '检测', value: '最终含水率、杂质率', status: 'normal' },
      { label: '记录', value: '干燥不均匀度、爆腰率', status: 'normal' }
    ]
  }
]

const currentDetailedStage = computed(() => {
  if (!selectedBatch.value) return 0
  if (selectedBatch.value.status === 'pending') return 0
  if (selectedBatch.value.status === 'completed') return 6
  // 根据进度估算阶段
  const progress = selectedBatch.value.progress
  if (progress < 5) return 1
  if (progress < 15) return 2
  if (progress < 40) return 3
  if (progress < 65) return 4
  if (progress < 80) return 5
  return 6
})

const runningCount = computed(() => batches.value.filter(b => b.status === 'running').length)

const getStatusType = (status) => ({ running: 'primary', completed: 'success', pending: 'info', error: 'danger' }[status] || 'info')
const getStatusText = (status) => ({ running: '进行中', completed: '已完成', pending: '待开始', error: '异常' }[status] || status)

const getQualityStatus = (value) => value < 3 ? 'success' : 'warning'
const getQualityTag = (value) => value < 3 ? 'success' : 'warning'
const getHeatingRateStatus = (rate) => rate <= 3 ? 'normal' : 'danger'

const onGrainTypeChange = (val) => {
  // 根据粮食品种推荐目标含水率
  const targets = { '水稻': 13.5, '小麦': 12.5, '玉米': 14.0 }
  createForm.targetMoisture = targets[val] || 13.5
}

const selectBatch = (batch) => {
  selectedBatch.value = batch
  nextTick(() => { initProcessChart() })
}

const initProcessChart = () => {
  if (!processChart.value || !selectedBatch.value) return
  if (processChartInstance) { processChartInstance.dispose() }
  processChartInstance = echarts.init(processChart.value)
  
  const hours = Array.from({ length: 24 }, (_, i) => i)
  // 根据细化的工艺曲线生成数据
  const temperatureData = hours.map(h => {
    if (h < 0.5) return 25 // 入仓
    if (h < 1) return 25 + (h - 0.5) * 40 // 预热
    if (h < 3) return 45 + (h - 1) * 5 // 升温
    if (h < 12) return 55 // 恒速干燥
    if (h < 14) return 55 - (h - 12) * 2 // 缓苏开始降温
    if (h < 16) return 51 - (h - 14) * 3 // 冷却
    return 45
  })
  
  const moistureData = hours.map(h => {
    const start = selectedBatch.value.initialMoisture
    const target = selectedBatch.value.targetMoisture
    // 非线性降水曲线
    if (h < 3) return start // 预热升温阶段少量降水
    if (h < 12) return start - (h - 3) * (start - target) / 12 // 恒速干燥
    if (h < 16) return Math.max(target, start - (start - target) * 0.95) // 缓苏平衡
    return target
  })
  
  // 添加阶段标注线
  const markLines = [
    { xAxis: 0.5, name: '预热结束' },
    { xAxis: 1, name: '开始升温' },
    { xAxis: 3, name: '恒速干燥' },
    { xAxis: 12, name: '缓苏阶段' },
    { xAxis: 14, name: '冷却阶段' }
  ]
  
  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['温度', '含水率×10'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: hours.map(h => `${h}h`), 
      axisLine: { lineStyle: { color: '#d9d9d9' } },
      axisLabel: { color: '#8c8c8c' }
    },
    yAxis: [
      { 
        type: 'value', 
        name: '温度(°C)', 
        position: 'left', 
        min: 0, 
        max: 80, 
        axisLine: { show: true, lineStyle: { color: '#ff4d4f' } },
        axisLabel: { color: '#ff4d4f' }
      },
      { 
        type: 'value', 
        name: '含水率(%)', 
        position: 'right', 
        min: 10, 
        max: 30, 
        axisLine: { show: true, lineStyle: { color: '#1890ff' } }
      }
    ],
    series: [
      { 
        name: '温度', 
        type: 'line', 
        smooth: true, 
        data: temperatureData, 
        itemStyle: { color: '#ff4d4f' },
        markLine: {
          data: markLines.map(m => ({ xAxis: m.xAxis, name: m.name })),
          label: { formatter: '{b}' },
          lineStyle: { type: 'dashed', color: '#999' }
        }
      },
      { 
        name: '含水率×10', 
        type: 'line', 
        smooth: true, 
        yAxisIndex: 1, 
        data: moistureData.map(m => m * 10), 
        itemStyle: { color: '#1890ff' }
      }
    ]
  }
  processChartInstance.setOption(option)
}

const createBatch = async () => {
  creating.value = true
  await new Promise(r => setTimeout(r, 1000))
  const newBatch = { 
    id: `DH${Date.now()}`, 
    grainType: createForm.grainType,
    dryingMethod: createForm.dryingMethod,
    status: 'pending', 
    initialMoisture: createForm.initialMoisture, 
    currentMoisture: createForm.initialMoisture, 
    targetMoisture: createForm.targetMoisture,
    loadAmount: createForm.loadAmount,
    progress: 0,
    uniformity: 0,
    brokenRate: 0,
    heatingRate: 0
  }
  batches.value.unshift(newBatch)
  ElMessage.success('批次创建成功')
  showCreateDialog.value = false
  creating.value = false
}

const startBatch = () => { 
  ElMessageBox.confirm('确认开始烘干？请确保粮食已入仓完成。', '提示', { 
    confirmButtonText: '确认', 
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => { 
    selectedBatch.value.status = 'running'
    selectedBatch.value.currentStage = '预热阶段'
    ElMessage.success('烘干已开始')
  }) 
}

const stopBatch = () => { 
  ElMessageBox.confirm('确认停止烘干？这会导致当前批次异常终止。', '警告', { 
    confirmButtonText: '确认停止', 
    cancelButtonText: '取消',
    type: 'error'
  }).then(() => { 
    selectedBatch.value.status = 'pending'
    ElMessage.success('烘干已停止')
  }) 
}

onMounted(() => { 
  if (batches.value.length > 0) { selectBatch(batches.value[0]) } 
  window.addEventListener('resize', () => { processChartInstance?.resize() }) 
})
</script>

<style scoped>
.drying-page { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.header-left { display: flex; align-items: center; gap: 16px; }
.header-left h2 { margin: 0; }
.main-grid { display: grid; grid-template-columns: 380px 1fr; gap: 20px; }
.batch-list { display: flex; flex-direction: column; gap: 12px; max-height: calc(100vh - 200px); overflow-y: auto; }
.batch-card { background: #fff; border-radius: 8px; padding: 16px; cursor: pointer; border: 2px solid transparent; transition: all 0.3s; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.batch-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.batch-card.active { border-color: #1890ff; }
.batch-card.running { background: #e6f7ff; }
.batch-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.batch-id { font-weight: 600; color: #262626; font-size: 14px; }
.batch-info-row { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 13px; }
.batch-info-row .label { color: #8c8c8c; }
.batch-info-row .value { color: #262626; font-weight: 500; }
.batch-progress { margin-top: 12px; }
.right-section { display: flex; flex-direction: column; gap: 16px; }
.card { background: #fff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.card-header { padding: 16px 20px; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; }
.card-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: #262626; }
.card-body { padding: 20px; }
.process-chart { height: 320px; }

/* 细化阶段样式 */
.stage-detailed { display: flex; flex-direction: column; gap: 12px; }
.stage-item-detailed { 
  display: flex; 
  flex-direction: column; 
  padding: 16px; 
  border-radius: 8px; 
  border-left: 4px solid #d9d9d9;
  background: #f5f5f5;
  transition: all 0.3s;
}
.stage-item-detailed.completed { border-left-color: #52c41a; background: #f6ffed; }
.stage-item-detailed.active { border-left-color: #1890ff; background: #e6f7ff; }
.stage-item-detailed.pending { opacity: 0.6; }

.stage-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.stage-icon-detailed { 
  width: 32px; 
  height: 32px; 
  border-radius: 50%; 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  background: #d9d9d9; 
  color: #fff; 
  font-size: 14px; 
  font-weight: 600;
}
.stage-item-detailed.completed .stage-icon-detailed { background: #52c41a; }
.stage-item-detailed.active .stage-icon-detailed { background: #1890ff; animation: pulse 1.5s infinite; }

.stage-title { flex: 1; }
.stage-name { font-weight: 600; color: #262626; font-size: 15px; }
.stage-duration { font-size: 12px; color: #8c8c8c; margin-top: 2px; }

.stage-params { display: flex; flex-wrap: wrap; gap: 12px; padding-left: 44px; }
.param-item { background: #fff; padding: 6px 12px; border-radius: 4px; font-size: 13px; }
.param-label { color: #8c8c8c; }
.param-value { font-weight: 500; margin-left: 4px; }
.param-value.normal { color: #52c41a; }
.param-value.warning { color: #faad14; }
.param-value.danger { color: #f5222d; }

/* 质量指标样式 */
.quality-metrics { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
.metric-item { background: #f5f5f5; padding: 16px; border-radius: 8px; }
.metric-label { font-size: 13px; color: #8c8c8c; margin-bottom: 8px; }
.metric-value { font-size: 24px; font-weight: 600; color: #262626; display: flex; align-items: center; gap: 8px; }
.metric-value.success { color: #52c41a; }
.metric-value.warning { color: #faad14; }
.metric-value.danger { color: #f5222d; }
.metric-value.normal { color: #1890ff; }
.metric-range { font-size: 12px; color: #bfbfbf; margin-top: 4px; }

@keyframes pulse { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.1); } }
.unit { margin-left: 8px; color: #8c8c8c; }
@media (max-width: 1200px) { 
  .main-grid { grid-template-columns: 1fr; } 
  .quality-metrics { grid-template-columns: 1fr; }
}
</style>