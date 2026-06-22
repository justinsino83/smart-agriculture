import axios from 'axios'
import router from '@/router'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8280',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // Prevent cache for GET requests
    if (config.method === 'get') {
      config.params = {
        ...config.params,
        _t: Date.now()
      }
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 统一清除登录状态并跳转到登录页
const clearAuthAndRedirect = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('userInfo')
  setTimeout(() => {
    router.push('/login')
  }, 100)
}

// Response interceptor
api.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && typeof res.code === 'number') {
      // 处理业务层的未登录状态（后端NotLoginException返回HTTP 200但code为非200）
      const msg = (res.message || '').toLowerCase()
      // 精确匹配"未登录"相关关键字，避免误匹配"登录成功"等正常消息
      const isNotLoggedIn = (
        res.code === 401 ||
        msg.includes('notlogin') ||
        msg.includes('token无效') ||
        msg.includes('token 无效') ||
        msg.includes('未登录') ||
        msg === 'token' ||
        msg.includes('token已过期') ||
        msg.includes('token 已过期') ||
        msg.includes('please login') ||
        msg.includes('session expired') ||
        (msg.includes('token') && (msg.includes('invalid') || msg.includes('无效') || msg.includes('过期')))
      )
      if (isNotLoggedIn) {
        clearAuthAndRedirect()
        return Promise.reject(new Error(res.message || 'Session expired, please login again'))
      }
      if (res.code === 200) {
        return res.data !== undefined ? res.data : res
      }
      return Promise.reject(new Error(res.message || 'Request failed'))
    }
    return res
  },
  (error) => {
    let message = '网络异常，请稍后重试'
    if (error.response) {
      const status = error.response.status
      const data = error.response.data
      switch (status) {
        case 400:
          message = data?.message || '参数错误'
          break
        case 401:
          message = '登录状态已过期，请重新登录'
          clearAuthAndRedirect()
          break
        case 403:
          message = '没有权限访问'
          break
        case 404:
          message = '请求资源不存在'
          break
        case 500:
          message = data?.message || '服务器错误'
          break
        default:
          message = `请求失败 [${status}]`
      }
    } else if (error.request) {
      message = '服务器未响应，请检查网络'
    } else {
      message = error.message
    }
    const err = new Error(message)
    err.status = error.response?.status
    err.original = error
    return Promise.reject(err)
  }
)

// Insect APIs
export const insectApi = {
  getDevices: () => api.get('/api/insect/devices'),
  getLocalDevices: (page = 1, size = 20) => api.get(`/api/insect/devices/local?page=${page}&size=${size}`),
  getDataList: (params) => api.get('/api/insect/data/list', { params }),
  getStatistics: (imei, startDate, endDate) => api.get('/api/insect/statistics', { params: { imei, startDate, endDate } }),
  getLatest: (imei, hours = 24) => api.get('/api/insect/latest', { params: { imei, hours } }),
  sync: () => api.post('/api/insect/sync'),
  getImageUrl: (imageUrl) => {
    if (!imageUrl) return ''
    if (imageUrl.startsWith('http')) return imageUrl
    return `http://182.40.36.95:4098/${imageUrl}`
  }
}

// Weather APIs
export const weatherApi = {
  getCurrentWeather: (clientId) => api.get('/api/weather/current', { params: { clientId } }),
  get24HourTrend: (clientId, startTime, endTime) => api.get('/api/weather/trend', { params: { clientId, startTime, endTime } }),
  get24HourHumidityTrend: (clientId, startTime, endTime) => api.get('/api/weather/humidity-trend', { params: { clientId, startTime, endTime } }),
  get24HourWindDirectionTrend: (clientId, startTime, endTime) => api.get('/api/weather/wind-direction-trend', { params: { clientId, startTime, endTime } }),
  getForecast: () => api.get('/api/weather/forecast'),
  getAll: (clientId, startTime, endTime) => api.get('/api/weather/all', { params: { clientId, startTime, endTime } })
}

// Soil APIs
export const soilApi = {
    getSensors: () => api.get('/api/soil/sensors'),
    getRealTimeData: (clientId) => api.get(`/api/soil/realtime/${clientId}`),
    getHistoryData: (clientId, start, end) => api.get(`/api/soil/history/${clientId}`, { params: { start, end } }),
    getHistoryDataPage: (clientId, start, end, page, size) => api.get(`/api/soil/history/${clientId}/page`, { params: { start, end, page, size } }),
    getOverview: () => api.get('/api/soil/overview'),
    getTrend: (clientId, days = 7) => api.get(`/api/soil/trend/${clientId}`, { params: { days } }),
    getStatistics: () => api.get('/api/soil/statistics'),
    getAlerts: () => api.get('/api/soil/alerts'),
    getRecommendations: () => api.get('/api/soil/recommendations')
}

// Irrigation APIs
export const irrigationApi = {
  getDevices: () => api.get('/api/irrigation/devices'),
  getDevicesPage: (page = 1, size = 10) => api.get('/api/irrigation/devices/page', { params: { page, size } }),
  getDeviceDetail: (deviceId) => api.get(`/api/irrigation/device/${deviceId}`),
  controlDevice: (deviceId, on) => api.post(`/api/irrigation/device/${deviceId}/control`, null, { params: { on } }),
  getTasks: () => api.get('/api/irrigation/tasks'),
  getTasksPage: (page = 1, size = 10, status = null) => api.get('/api/irrigation/tasks/page', { params: { page, size, status } }),
  createTask: (task) => api.post('/api/irrigation/task', task),
  getStatistics: (period = 'day') => api.get('/api/irrigation/statistics', { params: { period } }),
  getTrend: (days = 7) => api.get('/api/irrigation/trend', { params: { days } })
}

// Device Push APIs
export const devicePushApi = {
  pushData: (data) => api.post('/api/device/push', data),
  getDashboardOverview: () => api.get('/api/device/dashboard/overview'),
  getActiveDevices: () => api.get('/api/device/devices/active'),
  getTrend: (clientId, hours = 24) => api.get('/api/device/trend', { params: { clientId, hours } }),
  getHistory: (params) => api.get('/api/device/history', { params }),
  getLatest: (clientId) => api.get('/api/device/latest', { params: { clientId } }),
  getStatistics: (clientId) => api.get('/api/device/statistics', { params: { clientId } })
}

// Drying APIs
export const dryingApi = {
  getBatches: () => api.get('/api/drying/batches'),
  getBatchDetail: (batchId) => api.get(`/api/drying/batch/${batchId}`),
  createBatch: (data) => api.post('/api/drying/batch', data),
  startBatch: (batchId) => api.post(`/api/drying/batch/${batchId}/start`),
  stopBatch: (batchId) => api.post(`/api/drying/batch/${batchId}/stop`),
  getProcessData: (batchId) => api.get(`/api/drying/batch/${batchId}/process`),
  getStatistics: () => api.get('/api/drying/statistics')
}

// Storage APIs
export const storageApi = {
  getOverview: () => api.get('/api/storage/overview'),
  getStockList: (params) => api.get('/api/storage/stock', { params }),
  getStockDetail: (stockId) => api.get(`/api/storage/stock/${stockId}`),
  stockIn: (data) => api.post('/api/storage/stock-in', data),
  stockOut: (data) => api.post('/api/storage/stock-out', data),
  deleteStock: (stockId) => api.delete(`/api/storage/stock/${stockId}`),
  getAlerts: () => api.get('/api/storage/alerts'),
  getTrace: (stockId) => api.get(`/api/storage/stock/${stockId}/trace`)
}

// Energy APIs
export const energyApi = {
  getTodayStats: () => api.get('/api/energy/today'),
  getList: (params) => api.get('/api/energy/list', { params }),
  getTrend: (period) => api.get('/api/energy/trend', { params: { period } }),
  getDeviceUsage: () => api.get('/api/energy/device-usage'),
  getStatistics: () => api.get('/api/energy/statistics')
}

// System/Device APIs
export const systemApi = {
  getDevices: () => api.get('/api/system/devices'),
  getDeviceDetail: (deviceId) => api.get(`/api/system/device/${deviceId}`),
  updateDevice: (deviceId, data) => api.put(`/api/system/device/${deviceId}`, data),
  getLogs: (params) => api.get('/api/system/logs', { params })
}

// Facility APIs
export const facilityApi = {
  getAll: (type) => api.get('/api/facility/all', { params: { type } }),
  getList: (page, size, type) => api.get('/api/facility/list', { params: { page, size, type } }),
  getDetail: (id) => api.get(`/api/facility/${id}`),
  create: (data) => api.post('/api/facility', data),
  update: (id, data) => api.put(`/api/facility/${id}`, data),
  delete: (id) => api.delete(`/api/facility/${id}`)
}

// ========== IOT 设备接口（使用 Vite 代理） ==========
const IOT_TOKEN = 'BCACAFFC658149958B6401A3F0179281'

const iotApi = axios.create({
  baseURL: '',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

iotApi.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    console.error('IOT API Error:', error)
    return Promise.reject(error)
  }
)

// IOT 设备接口
export const iotDeviceApi = {
  // 获取所有设备列表
  listAllDevices: () => iotApi.post('/api/iot/manage/api/listAllDevices', {
    token: IOT_TOKEN
  }),
  // 获取指定设备的实时值
  getDeviceValues: (deviceId) => iotApi.post('/api/iot/manage/api/deviceValues', {
    deviceId: deviceId,
    token: IOT_TOKEN
  })
}

export default api
