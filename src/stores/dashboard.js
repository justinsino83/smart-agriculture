import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useDashboardStore = defineStore('dashboard', () => {
  // 统计数据
  const stats = ref({
    totalLandArea: 5000,
    deviceOnline: 42,
    deviceTotal: 50,
    dryingBatches: 2,
    powerUsage: 1250,
    carbonReduction: 32.5,
    pendingAlerts: 3,
    // 新增农艺指标
    avgSoilPH: 6.8,
    avgSoilEC: 1.2,
    npkLevel: '适宜',
    growthStage: '拔节期'
  })

  // 实时动态
  const activities = ref([
    { id: 1, content: '1号田灌溉完成，用水量2.5立方米', location: '试验田1号', time: '5分钟前', type: 'success' },
    { id: 2, content: '2号田土壤pH值偏低告警', location: '试验田2号', time: '15分钟前', type: 'warning' },
    { id: 3, content: '热泵烘干机启动新批次', location: '维明烘干中心', time: '30分钟前', type: 'info' },
    { id: 4, content: '3号田自动灌溉启动', location: '试验田3号', time: '45分钟前', type: 'success' }
  ])

  // 烘干批次
  const dryingBatches = ref([
    { 
      id: 'DH20240320001', 
      status: 'running', 
      progress: 65, 
      targetMoisture: 13.5, 
      currentMoisture: 15.2,
      temperature: 55,
      stage: '烘干中'
    },
    { 
      id: 'DH20240319002', 
      status: 'completed', 
      progress: 100, 
      targetMoisture: 13.5, 
      currentMoisture: 13.2,
      temperature: 55,
      stage: '已完成'
    }
  ])

  // 地块分布数据 - 增加经纬度信息用于高德地图
  const fieldDistribution = ref([
    { 
      name: '1号田', 
      moisture: 45, 
      status: 'normal', 
      area: 1200, 
      crop: '水稻', 
      growthStage: '拔节期', 
      ph: 6.8, 
      ec: 1.1, 
      moistureLevel: '适宜', 
      fertilizeNeeded: false,
      lat: 33.025,
      lng: 118.835
    },
    { 
      name: '2号田', 
      moisture: 38, 
      status: 'dry', 
      area: 1000, 
      crop: '小麦', 
      growthStage: '抽穗期', 
      ph: 6.5, 
      ec: 0.9, 
      moistureLevel: '轻旱', 
      fertilizeNeeded: true,
      lat: 33.022,
      lng: 118.842
    },
    { 
      name: '3号田', 
      moisture: 52, 
      status: 'wet', 
      area: 1100, 
      crop: '玉米', 
      growthStage: '大喇叭口期', 
      ph: 7.0, 
      ec: 1.3, 
      moistureLevel: '过湿', 
      fertilizeNeeded: false,
      lat: 33.018,
      lng: 118.838
    },
    { 
      name: '4号田', 
      moisture: 28, 
      status: 'dry', 
      area: 900, 
      crop: '水稻', 
      growthStage: '分蘖期', 
      ph: 5.8, 
      ec: 1.0, 
      moistureLevel: '中旱', 
      fertilizeNeeded: true,
      lat: 33.015,
      lng: 118.845
    },
    { 
      name: '5号田', 
      moisture: 48, 
      status: 'normal', 
      area: 800, 
      crop: '小麦', 
      growthStage: '灌浆期', 
      ph: 6.9, 
      ec: 1.2, 
      moistureLevel: '适宜', 
      fertilizeNeeded: false,
      lat: 33.028,
      lng: 118.848
    }
  ])

  // 能效数据
  const energyData = ref({
    greenPower: 85,
    solarUsage: 1062,
    gridUsage: 188,
    treesEquivalent: 156
  })

  const updateStats = (newStats) => {
    stats.value = { ...stats.value, ...newStats }
  }

  return {
    stats,
    activities,
    dryingBatches,
    fieldDistribution,
    energyData,
    updateStats
  }
})
