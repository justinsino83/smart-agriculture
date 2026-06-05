import api from './index'

export const dashboardApi = {
  getStats: () => api.get('/api/dashboard/statistics'),
  getFields: () => api.get('/api/dashboard/fields'),
  getDryingBatches: () => api.get('/api/dashboard/drying'),
  getAlerts: () => api.get('/api/dashboard/alerts'),
  getActivities: () => api.get('/api/dashboard/activities'),
  getEnergy: () => api.get('/api/dashboard/energy'),
  getOverview: () => api.get('/api/dashboard/overview')
}

export const soilApi = {
  getFields: () => api.get('/api/soil/fields'),
  getRealtime: (params) => api.get('/api/soil/realtime', { params }),
  getHistory: (params) => api.get('/api/soil/history', { params }),
  getStatistics: (params) => api.get('/api/soil/statistics', { params })
}

export const irrigationApi = {
  getDevices: () => api.get('/api/irrigation/devices'),
  controlDevice: (data) => api.post('/api/irrigation/control', data),
  getSchedule: () => api.get('/api/irrigation/schedule'),
  saveSchedule: (data) => api.post('/api/irrigation/schedule', data),
  getRecords: (params) => api.get('/api/irrigation/records', { params })
}

export const dryingApi = {
  getBatches: () => api.get('/api/drying/batches'),
  getBatch: (id) => api.get(`/api/drying/batch/${id}`),
  createBatch: (data) => api.post('/api/drying/batches', data),
  controlDevice: (data) => api.post('/api/drying/control', data),
  getComparison: (params) => api.get('/api/drying/comparison', { params })
}

export const storageApi = {
  getList: (params) => api.get('/api/storage/list', { params }),
  getStatistics: () => api.get('/api/storage/statistics'),
  inStorage: (data) => api.post('/api/storage/in', data),
  outStorage: (data) => api.post('/api/storage/out', data)
}

export const energyApi = {
  getList: (params) => api.get('/api/energy/list', { params }),
  getStatistics: () => api.get('/api/energy/statistics'),
  getTrend: (params) => api.get('/api/energy/trend', { params })
}

export const deviceApi = {
  push: (data) => api.post('/api/device/push', data),
  getHistory: (params) => api.get('/api/device/history', { params }),
  getLatest: (clientId) => api.get(`/api/device/latest?clientId=${clientId}`)
}

export const insectApi = {
  sync: () => api.post('/api/insect/sync'),
  getDevices: () => api.get('/api/insect/devices'),
  getData: (params) => api.get('/api/insect/data/list', { params }),
  getStatistics: (params) => api.get('/api/insect/statistics', { params }),
  getLatest: (imei, hours) => api.get('/api/insect/latest', { params: { imei, hours } })
}

export const userApi = {
  login: (data) => api.post('/api/auth/login', data),
  logout: () => api.post('/api/auth/logout'),
  getInfo: () => api.get('/api/auth/info')
}

export const llmApi = {
  // 前端下拉框使用的接口
  getModels: () => api.get('/api/llm/models'),

  // 管理页面接口
  listModels: (params) => api.get('/api/llm/admin/models', { params }),
  getModelDetail: (id) => api.get(`/api/llm/admin/models/${id}`),
  addModel: (data) => api.post('/api/llm/admin/models', data),
  updateModel: (id, data) => api.put(`/api/llm/admin/models/${id}`, data),
  deleteModel: (id) => api.delete(`/api/llm/admin/models/${id}`),
  toggleAvailable: (id) => api.put(`/api/llm/admin/models/${id}/available`)
}