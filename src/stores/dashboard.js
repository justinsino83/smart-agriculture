import { defineStore } from 'pinia'
import { ref } from 'vue'
import { devicePushApi, dryingApi, energyApi } from '@/api'

export const useDashboardStore = defineStore('dashboard', () => {
  // 加载状态
  const loading = ref(true)
  const error = ref(null)

  // 默认演示数据 - 当API失败时使用
  const defaultStats = {
    totalLandArea: 5000,
    deviceOnline: 48,
    deviceTotal: 50,
    dryingBatches: 0,
    powerUsage: 0,
    carbonReduction: 32.5,
    avgSoilPH: 6.8,
    avgSoilEC: 1.2,
    avgTemperature: 25,
    avgHumidity: 60,
    npkLevel: '适宜',
    growthStage: '拔节期',
    pendingAlerts: 0
  }

  const defaultEnergyData = {
    greenPower: 85,
    solarUsage: 1062,
    gridUsage: 188,
    treesEquivalent: 156
  }

  const defaultDryingBatches = [
    {
      id: 'DH20240320001',
      status: 'running',
      progress: 65,
      targetMoisture: 13.5,
      currentMoisture: 15.2,
      temperature: 55,
      duration: 120,
      weight: 2500,
      stage: '烘干中'
    }
  ]

  const defaultFields = [
    {
      id: 1,
      name: '一号田块',
      area: 500,
      lng: 118.835,
      lat: 33.025,
      soilType: '壤土',
      crop: '水稻',
      status: 'normal',
      moisture: 45,
      growthStage: '拔节期',
      ph: 6.8,
      ec: 1.1,
      moistureLevel: '适宜',
      fertilizeNeeded: false
    },
    {
      id: 2,
      name: '二号田块',
      area: 800,
      lng: 118.842,
      lat: 33.022,
      soilType: '壤土',
      crop: '小麦',
      status: 'dry',
      moisture: 38,
      growthStage: '抽穗期',
      ph: 6.5,
      ec: 0.9,
      moistureLevel: '轻旱',
      fertilizeNeeded: true
    },
    {
      id: 3,
      name: '三号田块',
      area: 600,
      lng: 118.838,
      lat: 33.018,
      soilType: '壤土',
      crop: '玉米',
      status: 'normal',
      moisture: 52,
      growthStage: '大喇叭口期',
      ph: 7.0,
      ec: 1.3,
      moistureLevel: '适宜',
      fertilizeNeeded: false
    }
  ]

  const defaultActivities = [
    { id: 1, content: '一号田块灌溉完成，用水量2.5立方米', location: '一号田块', time: '5分钟前', type: 'success' },
    { id: 2, content: '二号田块土壤pH值偏低告警', location: '二号田块', time: '15分钟前', type: 'warning' },
    { id: 3, content: '热泵烘干机启动新批次', location: '烘干车间', time: '30分钟前', type: 'info' }
  ]

  // 统计数据
  const stats = ref({ ...defaultStats })

  // 实时动态
  const activities = ref([...defaultActivities])

  // 烘干批次
  const dryingBatches = ref([...defaultDryingBatches])

  // 地块分布数据
  const fieldDistribution = ref([...defaultFields])

  // 能效数据
  const energyData = ref({ ...defaultEnergyData })

  const updateStats = (newStats) => {
    stats.value = { ...stats.value, ...newStats }
  }

  // 格式化时间
  const formatTime = (timeStr) => {
    if (!timeStr) return '刚刚'
    try {
      const time = new Date(timeStr)
      const now = new Date()
      const diff = Math.floor((now - time) / 1000 / 60)
      
      if (diff < 1) return '刚刚'
      if (diff < 60) return `${diff}分钟前`
      if (diff < 1440) return `${Math.floor(diff / 60)}小时前`
      return `${Math.floor(diff / 1440)}天前`
    } catch (e) {
      return '刚刚'
    }
  }

  // 从API加载仪表盘综合数据
  const loadDashboardData = async () => {
    try {
      const data = await devicePushApi.getDashboardOverview()
      if (data) {
        // 更新设备统计
        const statusDist = data.statusDistribution || {}
        stats.value.deviceOnline = statusDist.online || defaultStats.deviceOnline
        stats.value.deviceTotal = statusDist.total || defaultStats.deviceTotal
        stats.value.totalLandArea = data.totalLandArea || defaultStats.totalLandArea
        
        // 更新环境数据
        const env = data.environment || {}
        stats.value.avgTemperature = env.avgTemperature || defaultStats.avgTemperature
        stats.value.avgHumidity = env.avgHumidity || defaultStats.avgHumidity
        stats.value.avgSoilPH = env.avgSoilPH || defaultStats.avgSoilPH
        stats.value.avgSoilEC = env.avgSoilEC || defaultStats.avgSoilEC
        
        // 更新告警统计
        const alerts = data.alerts || []
        stats.value.pendingAlerts = alerts.length
        
        // 更新实时动态
        if (alerts.length > 0) {
          activities.value = alerts.slice(0, 10).map((alert, index) => ({
            id: Date.now() + index,
            content: alert.message || '',
            location: alert.clientId || '设备',
            time: formatTime(alert.time),
            type: alert.level || 'info'
          }))
        } else {
          activities.value = [...defaultActivities]
        }
        
        // 更新地块数据
        if (data.fields && data.fields.length > 0) {
          fieldDistribution.value = data.fields
        } else {
          fieldDistribution.value = [...defaultFields]
        }
      }
      error.value = null
    } catch (err) {
      console.error('加载仪表盘数据失败:', err)
      error.value = '仪表盘数据加载失败，使用演示数据'
      // 保持默认数据
      activities.value = [...defaultActivities]
      fieldDistribution.value = [...defaultFields]
    }
  }

  // 加载烘干数据
  const loadDryingData = async () => {
    try {
      const batches = await dryingApi.getBatches()
      if (batches && batches.length > 0) {
        dryingBatches.value = batches
        stats.value.dryingBatches = batches.filter(b => b.status === 'running').length
      } else {
        dryingBatches.value = [...defaultDryingBatches]
        stats.value.dryingBatches = defaultDryingBatches.filter(b => b.status === 'running').length
      }
    } catch (err) {
      console.error('加载烘干数据失败:', err)
      dryingBatches.value = [...defaultDryingBatches]
      stats.value.dryingBatches = defaultDryingBatches.filter(b => b.status === 'running').length
    }
  }

  // 加载能耗数据
  const loadEnergyData = async () => {
    try {
      const energyStats = await energyApi.getTodayStats()
      if (energyStats) {
        stats.value.powerUsage = energyStats.todayPower || defaultStats.powerUsage
        stats.value.carbonReduction = energyStats.carbonSaved || defaultStats.carbonReduction
        energyData.value = {
          greenPower: energyStats.avgEfficiency || defaultEnergyData.greenPower,
          solarUsage: energyStats.solarUsage || defaultEnergyData.solarUsage,
          gridUsage: energyStats.gridUsage || defaultEnergyData.gridUsage,
          treesEquivalent: energyStats.carbonSaved || defaultEnergyData.treesEquivalent
        }
      }
    } catch (err) {
      console.error('加载能耗数据失败:', err)
      energyData.value = { ...defaultEnergyData }
    }
  }

  // 加载所有仪表盘数据
  const loadAllData = async () => {
    loading.value = true
    try {
      await Promise.all([
        loadDashboardData(),
        loadDryingData(),
        loadEnergyData()
      ])
    } finally {
      loading.value = false
    }
  }

  return {
    stats,
    activities,
    dryingBatches,
    fieldDistribution,
    energyData,
    loading,
    error,
    updateStats,
    loadDashboardData,
    loadDryingData,
    loadEnergyData,
    loadAllData
  }
})
