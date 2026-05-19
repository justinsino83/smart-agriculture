import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
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

// Response interceptor
api.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && typeof res.code === 'number') {
      if (res.code === 200) {
        return res.data !== undefined ? res.data : res
      }
      return Promise.reject(new Error(res.message || 'Request failed'))
    }
    return res
  },
  (error) => {
    let message = 'Network error, please try again'
    if (error.response) {
      const status = error.response.status
      const data = error.response.data
      switch (status) {
        case 400:
          message = data?.message || 'Invalid parameters'
          break
        case 401:
          message = 'Session expired, please login again'
          localStorage.removeItem('token')
          setTimeout(() => {
            window.location.href = '/#/login'
          }, 500)
          break
        case 403:
          message = 'No permission'
          break
        case 404:
          message = 'Resource not found'
          break
        case 500:
          message = data?.message || 'Server error'
          break
        default:
          message = `Request failed [${status}]`
      }
    } else if (error.request) {
      message = 'Server not responding'
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
  getCurrentWeather: (deviceCode) => api.get('/api/weather/current', { params: { deviceCode } }),
  get24HourTrend: (deviceCode, startTime, endTime) => api.get('/api/weather/trend', { params: { deviceCode, startTime, endTime } }),
  get24HourHumidityTrend: (deviceCode, startTime, endTime) => api.get('/api/weather/humidity-trend', { params: { deviceCode, startTime, endTime } }),
  get24HourWindDirectionTrend: (deviceCode, startTime, endTime) => api.get('/api/weather/wind-direction-trend', { params: { deviceCode, startTime, endTime } }),
  getForecast: () => api.get('/api/weather/forecast'),
  getAll: (deviceCode, startTime, endTime) => api.get('/api/weather/all', { params: { deviceCode, startTime, endTime } })
}

// Soil APIs
export const soilApi = {
  getSensors: () => api.get('/api/soil/sensors'),
  getRealTimeData: (sensorId) => api.get(`/api/soil/realtime/${sensorId}`),
  getHistoryData: (sensorId, start, end) => api.get(`/api/soil/history/${sensorId}`, { params: { start, end } }),
  getOverview: () => api.get('/api/soil/overview'),
  getTrend: (sensorId, days = 7) => api.get(`/api/soil/trend/${sensorId}`, { params: { days } }),
  getStatistics: () => api.get('/api/soil/statistics'),
  getAlerts: () => api.get('/api/soil/alerts'),
  getRecommendations: () => api.get('/api/soil/recommendations')
}

// Irrigation APIs
export const irrigationApi = {
  getDevices: () => api.get('/api/irrigation/devices'),
  getDeviceDetail: (deviceId) => api.get(`/api/irrigation/device/${deviceId}`),
  controlDevice: (deviceId, on) => api.post(`/api/irrigation/device/${deviceId}/control`, null, { params: { on } }),
  getTasks: () => api.get('/api/irrigation/tasks'),
  createTask: (task) => api.post('/api/irrigation/task', task),
  getStatistics: (period = 'day') => api.get('/api/irrigation/statistics', { params: { period } }),
  getTrend: (days = 7) => api.get('/api/irrigation/trend', { params: { days } })
}

export default api