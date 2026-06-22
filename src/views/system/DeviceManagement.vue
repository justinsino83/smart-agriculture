<template>
  <div class="device-page">
    <div class="page-header">
      <div class="header-left">
        <h2>设备管理</h2>
        <el-tag type="info">共 {{ deviceList.length }} 台设备</el-tag>
      </div>
      <div class="header-actions">
        <el-button type="primary" :icon="Refresh" @click="loadDevices" :loading="loading">
          刷新数据
        </el-button>
      </div>
    </div>

    <div class="main-content">
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon green"><el-icon><VideoCamera /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-label">设备总数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon blue"><el-icon><Monitor /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.cameraCount }}</div>
            <div class="stat-label">监控设备</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon yellow"><el-icon><Odometer /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.sensorCount }}</div>
            <div class="stat-label">传感/执行设备</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon red"><el-icon><Warning /></el-icon></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.offlineCount }}</div>
            <div class="stat-label">离线/异常</div>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <h3>设备列表</h3>
          <div class="header-actions">
            <el-select v-model="filterType" placeholder="按类型筛选" style="width: 160px; margin-right: 10px;" clearable>
              <el-option label="全部" value="" />
              <el-option label="监控设备" value="1" />
              <el-option label="其他设备" value="other" />
            </el-select>
            <el-input v-model="searchKeyword" placeholder="搜索设备名称/ID" style="width: 260px" clearable>
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
          </div>
        </div>

        <div class="card-body">
          <el-table :data="filteredDeviceList" stripe style="width: 100%" height="calc(100vh - 380px)" v-loading="loading">
            <el-table-column type="index" label="#" width="60" align="center" />
            <el-table-column label="设备名称" min-width="200">
              <template #default="{ row }">
                <span class="device-name">{{ row.device_name || row.name || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="device_type" label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getDeviceTypeTag(row.device_type)" size="small">
                  {{ getDeviceTypeName(row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="设备ID" min-width="180">
              <template #default="{ row }">
                <span class="device-id">{{ row.id || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row)" size="small">{{ getStatusName(row) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="viewDevice(row)">查看详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <el-dialog v-model="showVideoDialog" title="监控画面" width="900px" :close-on-click-modal="false" @close="destroyVideoPlayer">
      <div v-if="selectedDevice" class="video-container">
        <div class="video-info">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="设备名称">{{ selectedDevice.device_name || selectedDevice.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="设备类型">{{ getDeviceTypeName(selectedDevice) }}</el-descriptions-item>
            <el-descriptions-item label="设备ID" span="2">
              <span class="text-code">{{ selectedDevice.id || '-' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <div class="video-player-wrapper">
          <video ref="videoElementRef" class="video-player" muted autoplay controls playsinline></video>
          <div v-if="videoLoading" class="video-loading">
            <el-icon class="loading-icon"><Loading /></el-icon>
            <span>视频加载中...</span>
          </div>
          <div v-if="videoError" class="video-error">
            <el-icon class="error-icon"><Warning /></el-icon>
            <span>{{ videoError }}</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="showDataDialog" title="设备实时数据" width="800px" :close-on-click-modal="false">
      <div v-if="selectedDevice" class="data-container">
        <div class="data-info">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="设备名称">{{ selectedDevice.device_name || selectedDevice.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="设备类型">{{ getDeviceTypeName(selectedDevice) }}</el-descriptions-item>
            <el-descriptions-item label="设备ID" span="2">
              <span class="text-code">{{ selectedDevice.id || '-' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <div class="data-section">
          <div class="section-header">
            <h4>实时监测值</h4>
            <el-button type="primary" size="small" :icon="Refresh" @click="loadDeviceValues(selectedDevice)" :loading="valuesLoading">
              刷新数据
            </el-button>
          </div>
          <el-alert v-if="deviceValuesError" :title="deviceValuesError" type="error" show-icon style="margin-bottom: 16px;" />
          <div v-if="deviceValuesList.length > 0" class="values-grid">
            <div v-for="(item, idx) in deviceValuesList" :key="extractItemKey(item) || idx" class="value-card">
              <div class="value-label">{{ getItemName(item) }}</div>
              <div class="value-content">
                <span class="value-number">{{ formatValue(item) }}</span>
                <span v-if="getItemUnit(item)" class="value-unit">{{ getItemUnit(item) }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else-if="!valuesLoading && !deviceValuesError" description="暂无监测数据" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, shallowRef } from 'vue'
import { VideoCamera, Warning, Monitor, Search, Refresh, Odometer, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { iotDeviceApi } from '@/api'
import mpegts from 'mpegts.js'

const loading = ref(false)
const searchKeyword = ref('')
const filterType = ref('')
const deviceList = ref([])

const showVideoDialog = ref(false)
const showDataDialog = ref(false)
const selectedDevice = ref(null)

const videoElementRef = ref(null)
const videoPlayerRef = shallowRef(null)
const videoLoading = ref(false)
const videoError = ref('')
const videoStarted = ref(false)

const valuesLoading = ref(false)
const deviceValuesList = ref([])
const deviceValuesError = ref('')

const stats = computed(() => {
  const list = deviceList.value
  const total = list.length
  const cameraCount = list.filter(d => Number(d.device_type) === 1).length
  const sensorCount = list.filter(d => Number(d.device_type) !== 1).length
  const offlineCount = list.filter(d => {
    const status = d.online ?? d.status
    if (typeof status === 'number') return status === 0
    if (typeof status === 'string') {
      const s = status.toLowerCase()
      return s.includes('off') || s.includes('离线') || s.includes('false')
    }
    return false
  }).length
  return { total, cameraCount, sensorCount, offlineCount }
})

const filteredDeviceList = computed(() => {
  let list = deviceList.value
  if (filterType.value) {
    if (filterType.value === '1') {
      list = list.filter(d => Number(d.device_type) === 1)
    } else if (filterType.value === 'other') {
      list = list.filter(d => Number(d.device_type) !== 1)
    }
  }
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(d => {
      const name = (d.device_name || d.name || '').toLowerCase()
      const id = (String(d.id || '')).toLowerCase()
      return name.includes(kw) || id.includes(kw)
    })
  }
  return list
})

const getDeviceTypeName = (row) => {
  if (row.device_type_name) {
    return row.device_type_name
  }
  const t = Number(row.device_type)
  if (t === 1) return '监控摄像头'
  if (t === 2) return '温湿度传感器'
  if (t === 3) return '执行器'
  if (t === 4) return '水位传感器'
  if (t === 5) return '水浸传感器'
  if (t === 6) return '阀门控制'
  if (t === 7) return '压力传感器'
  if (t === 8) return '电流电压传感器'
  return '其他设备'
}

const getDeviceTypeTag = (type) => {
  const t = Number(type)
  if (t === 1) return 'success'
  if (t === 2 || t === 4 || t === 5 || t === 7) return 'warning'
  if (t === 3 || t === 6) return 'info'
  return ''
}

const getStatusType = (row) => {
  const status = row.online ?? row.status
  if (typeof status === 'number') return status === 1 ? 'success' : 'danger'
  if (typeof status === 'boolean') return status ? 'success' : 'danger'
  if (typeof status === 'string') {
    const s = status.toLowerCase()
    if (s.includes('on') || s.includes('在线') || s.includes('true')) return 'success'
    if (s.includes('off') || s.includes('离线') || s.includes('false')) return 'danger'
  }
  return 'info'
}

const getStatusName = (row) => {
  const status = row.online ?? row.status
  if (typeof status === 'number') return status === 1 ? '在线' : '离线'
  if (typeof status === 'boolean') return status ? '在线' : '离线'
  if (typeof status === 'string') {
    const s = status.toLowerCase()
    if (s.includes('on') || s.includes('在线') || s.includes('true')) return '在线'
    if (s.includes('off') || s.includes('离线') || s.includes('false')) return '离线'
  }
  return '未知'
}

const FIELD_LABEL_MAP = {
  t: '温度',
  h: '湿度',
  status: '状态',
  v: '执行器工作电压',
  t2: '温度2',
  t1: '温度1',
  s: '阀门状态',
  protectTorque: '执行器保护扭矩(推力)',
  pressure2: '压力2',
  pressure1: '压力1',
  pos: '阀门开度',
  minTorque: '执行器最小扭矩',
  minI: '执行器最小电流',
  maxTorque: '执行器最大扭矩',
  maxI: '执行器最大电流',
  i: '执行器保护电流',
  waterLevel: '水位值',
  hasWater: '水浸状态'
}

const FIELD_UNIT_MAP = {
  t: '°C',
  t1: '°C',
  t2: '°C',
  h: '%',
  pos: '%',
  v: 'V',
  minI: 'A',
  maxI: 'A',
  i: 'A'
}

const hasChinese = (value) => typeof value === 'string' && /[\u4e00-\u9fa5]/.test(value)

const extractItemKey = (item) => {
  const directKey = item.item_key || item.itemKey || item.key || item.code
  if (directKey) return String(directKey)

  const fallback = [item.name, item.item_name, item.label].find(
    (value) => typeof value === 'string' && /^[A-Za-z][A-Za-z0-9_]*$/.test(value)
  )
  return fallback || ''
}

const getItemName = (item) => {
  const itemKey = extractItemKey(item)
  if (itemKey && FIELD_LABEL_MAP[itemKey]) {
    return FIELD_LABEL_MAP[itemKey]
  }

  const chineseName = [item.name, item.item_name, item.label].find(hasChinese)
  if (chineseName) return chineseName

  return itemKey || '未知'
}

const getRawValue = (item) => (
  item.value ??
  item.item_value ??
  item.val ??
  item.itemValue ??
  item.currentValue ??
  item.data ??
  '-'
)

const formatValue = (item) => {
  const itemKey = extractItemKey(item)
  const val = getRawValue(item)
  if (val === null || val === undefined || val === '') return '-'

  if (itemKey === 'status') {
    if (val === 1 || val === '1' || val === true || val === 'true') return '正常'
    if (val === 0 || val === '0' || val === false || val === 'false') return '异常'
  }

  if (itemKey === 's') {
    if (val === 1 || val === '1' || val === true || val === 'true') return '开启'
    if (val === 0 || val === '0' || val === false || val === 'false') return '关闭'
  }

  if (itemKey === 'hasWater') {
    if (val === 1 || val === '1' || val === true || val === 'true') return '有水'
    if (val === 0 || val === '0' || val === false || val === 'false') return '无水'
  }

  return val
}

const getItemUnit = (item) => {
  const itemKey = extractItemKey(item)
  return item.unit || item.item_unit || FIELD_UNIT_MAP[itemKey] || ''
}

const loadDevices = async () => {
  loading.value = true
  try {
    const result = await iotDeviceApi.listAllDevices()
    console.log('IOT Devices Response:', result)

    let devices = []
    if (Array.isArray(result)) {
      devices = result
    } else if (result && Array.isArray(result.data)) {
      devices = result.data
    } else if (result && result.data && Array.isArray(result.data.devices)) {
      devices = result.data.devices
    } else if (result && result.data && Array.isArray(result.data.list)) {
      devices = result.data.list
    } else if (result && Array.isArray(result.devices)) {
      devices = result.devices
    } else if (result && Array.isArray(result.list)) {
      devices = result.list
    } else if (result && Array.isArray(result.result)) {
      devices = result.result
    }

    deviceList.value = devices
    ElMessage.success(`加载成功，共 ${devices.length} 台设备`)
  } catch (error) {
    console.error('加载设备列表失败:', error)
    ElMessage.error('加载设备列表失败，请检查网络连接')
    deviceList.value = []
  } finally {
    loading.value = false
  }
}

const viewDevice = (row) => {
  selectedDevice.value = row
  const deviceType = Number(row.device_type)
  if (deviceType === 1) {
    showDataDialog.value = false
    showVideoDialog.value = true
    nextTick(() => { initVideoPlayer(row) })
  } else {
    showVideoDialog.value = false
    showDataDialog.value = true
    loadDeviceValues(row)
  }
}

const initVideoPlayer = (device) => {
  const flvUrl = device.https_flv_url
  if (!flvUrl) {
    videoError.value = '设备没有推流地址'
    videoLoading.value = false
    return
  }

  destroyVideoPlayer()
  videoStarted.value = false
  videoError.value = ''
  videoLoading.value = true

  nextTick(() => {
    const videoEl = videoElementRef.value
    if (!videoEl) {
      videoLoading.value = false
      return
    }
    
    try {
      if (mpegts && mpegts.isSupported()) {
        console.log('Starting video player with URL:', flvUrl)
        const markVideoReady = () => {
          videoStarted.value = true
          videoLoading.value = false
          videoError.value = ''
        }

        videoEl.onloadeddata = markVideoReady
        videoEl.onplaying = markVideoReady

        const player = mpegts.createPlayer({
          type: 'flv',
          url: flvUrl,
          isLive: true,
          hasAudio: false,
          hasVideo: true,
          cors: true
        }, {
          enableWorker: false,
          enableStashBuffer: false,
          stashInitialSize: 128,
          autoCleanupSourceBuffer: true,
          lazyLoad: false,
          lazyLoadMaxDuration: 0,
          lazyLoadRecoverDuration: 0,
          liveSync: true,
          liveBufferLatencyChasing: true,
          liveBufferLatencyMaxLatency: 1.5,
          liveBufferLatencyMinRemain: 0.5
        })
        
        player.attachMediaElement(videoEl)
        player.load()
        
        player.on(mpegts.Events.LOADING_COMPLETE, () => {
          console.log('Video loading complete')
          if (!videoStarted.value) {
            videoLoading.value = false
          }
        })

        player.play().then(() => {
          console.log('Video playing successfully')
          markVideoReady()
        }).catch((err) => {
          console.warn('mpegts.js play failed:', err)
          if (!videoStarted.value) {
            videoLoading.value = false
            videoError.value = '视频播放失败，请检查推流地址'
          }
        })

        player.on(mpegts.Events.ERROR, (errorType, errorDetail) => {
          console.error('mpegts.js error:', errorType, errorDetail)
          if (!videoStarted.value) {
            videoError.value = `视频播放错误: ${errorType}`
            videoLoading.value = false
          }
        })

        videoPlayerRef.value = player
      } else {
        console.warn('mpegts.js not available')
        videoError.value = '浏览器不支持FLV格式播放'
        videoLoading.value = false
      }
    } catch (err) {
      console.error('视频播放器初始化失败:', err)
      videoError.value = '视频播放器初始化失败'
      videoLoading.value = false
    }
  })
}

const destroyVideoPlayer = () => {
  if (videoPlayerRef.value) {
    try {
      videoPlayerRef.value.pause()
      videoPlayerRef.value.unload()
      videoPlayerRef.value.detachMediaElement()
      videoPlayerRef.value.destroy()
    } catch (e) {
      console.warn('destroy player warning:', e)
    }
    videoPlayerRef.value = null
  }
  if (videoElementRef.value) {
    videoElementRef.value.pause()
    videoElementRef.value.onloadeddata = null
    videoElementRef.value.onplaying = null
    videoElementRef.value.removeAttribute('src')
    videoElementRef.value.load()
  }
  videoStarted.value = false
  videoLoading.value = false
  videoError.value = ''
}

const loadDeviceValues = async (device) => {
  const deviceId = device.id
  if (!deviceId) {
    deviceValuesError.value = '设备ID为空，无法查询数据'
    return
  }
  valuesLoading.value = true
  deviceValuesError.value = ''
  deviceValuesList.value = []
  try {
    const result = await iotDeviceApi.getDeviceValues(deviceId)
    console.log('Device Values Response:', result)

    let items = []
    if (Array.isArray(result)) {
      items = result
    } else if (result && Array.isArray(result.data)) {
      items = result.data
    } else if (result && result.data && Array.isArray(result.data.values)) {
      items = result.data.values
    } else if (result && result.data && Array.isArray(result.data.list)) {
      items = result.data.list
    } else if (result && result.data && Array.isArray(result.data.items)) {
      items = result.data.items
    } else if (result && Array.isArray(result.values)) {
      items = result.values
    } else if (result && Array.isArray(result.list)) {
      items = result.list
    } else if (result && Array.isArray(result.items)) {
      items = result.items
    } else if (result && result.data && typeof result.data === 'object' && !Array.isArray(result.data)) {
      const entries = Object.entries(result.data)
      items = entries.map(([key, val]) => ({ item_key: key, name: key, value: val }))
    }

    if (!Array.isArray(items) && items && typeof items === 'object') {
      const entries = Object.entries(items)
      items = entries.map(([key, val]) => ({ item_key: key, name: key, value: val }))
    }

    deviceValuesList.value = items || []
    if ((items || []).length === 0 && !valuesLoading.value) {
      deviceValuesError.value = '未获取到监测数据'
    }
  } catch (error) {
    console.error('加载设备数据失败:', error)
    deviceValuesError.value = '加载设备数据失败，请检查网络连接'
    deviceValuesList.value = []
  } finally {
    valuesLoading.value = false
  }
}

onMounted(() => {
  loadDevices()
})

onBeforeUnmount(() => {
  destroyVideoPlayer()
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
.header-left { display: flex; align-items: center; gap: 16px; }
.header-left h2 { margin: 0; font-size: 20px; color: #262626; }
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
  flex-shrink: 0;
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  transition: transform 0.2s, box-shadow 0.2s;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;
}
.stat-icon.blue { background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%); }
.stat-icon.green { background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%); }
.stat-icon.yellow { background: linear-gradient(135deg, #faad14 0%, #ffc53d 100%); }
.stat-icon.red { background: linear-gradient(135deg, #f5222d 0%, #ff4d4f 100%); }
.stat-content { flex: 1; }
.stat-value { font-size: 32px; font-weight: 700; color: #262626; line-height: 1.2; }
.stat-label { font-size: 14px; color: #8c8c8c; margin-top: 6px; }
.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex: 1;
}
.card-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}
.card-header h3 { margin: 0; font-size: 18px; font-weight: 600; color: #262626; }
.card-body { padding: 24px; flex: 1; overflow: hidden; }
.device-name { font-weight: 500; color: #262626; }
.device-id { font-family: monospace; color: #595959; font-size: 12px; }
.text-code { font-family: monospace; font-size: 12px; }

.video-container { display: flex; flex-direction: column; gap: 20px; }
.video-info { margin-bottom: 8px; }
.video-player-wrapper {
  position: relative;
  width: 100%;
  background: #1a1a1a;
  border-radius: 12px;
  overflow: hidden;
  aspect-ratio: 16/9;
}
.video-player { width: 100%; height: 100%; display: block; background: #1a1a1a; }
.video-loading, .video-error {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 16px; color: #fff; font-size: 16px;
  background: rgba(0, 0, 0, 0.8);
}
.video-error { color: #ff7875; }
.loading-icon, .error-icon { font-size: 40px; }
.loading-icon { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.data-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 4px 2px;
}
.data-info {
  margin-bottom: 4px;
  padding: 14px;
  border-radius: 14px;
  background: linear-gradient(135deg, #f7fbff 0%, #eef6ff 100%);
}
.data-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 18px;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: inset 0 0 0 1px #edf2f7;
}
.section-header {
  display: flex; justify-content: space-between; align-items: center;
  padding-bottom: 16px; border-bottom: 2px solid #f0f0f0;
}
.section-header h4 { margin: 0; font-size: 18px; color: #262626; font-weight: 600; }
.values-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 18px;
  margin-top: 8px;
}
.value-card {
  position: relative;
  background: linear-gradient(180deg, #ffffff 0%, #f7fbff 100%);
  border: 1px solid #e6eef8;
  border-radius: 16px;
  padding: 22px 20px;
  transition: all 0.3s;
  box-shadow: 0 8px 24px rgba(15, 35, 95, 0.06);
}
.value-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  border-radius: 16px 16px 0 0;
  background: linear-gradient(90deg, #409eff 0%, #67c23a 100%);
}
.value-card:hover {
  background: linear-gradient(180deg, #ffffff 0%, #edf6ff 100%);
  box-shadow: 0 12px 28px rgba(24,144,255,0.14);
  border-color: #1890ff;
  transform: translateY(-2px);
}
.value-label { 
  font-size: 14px;
  color: #5b6b7b;
  margin-bottom: 14px;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.value-content { 
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 4px;
}
.value-number { 
  font-size: 30px;
  font-weight: 700;
  color: #262626;
  line-height: 1;
}
.value-unit {
  font-size: 14px;
  color: #8c8c8c;
  font-weight: 600;
  padding-bottom: 3px;
}
@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}
</style>
