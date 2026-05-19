<template>
  <div class="pest-page">
    <!-- 第一行：筛选条件 -->
    <div class="filter-row">
      <el-select v-model="filterForm.imei" placeholder="选择设备" clearable size="default" style="width: 200px" @change="handleFilterChange">
        <el-option v-for="device in deviceList" :key="device.imei" :label="device.devName || device.imei" :value="device.imei" />
      </el-select>

      <el-date-picker
        v-model="filterForm.dateRange"
        type="datetimerange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        size="default"
        style="width: 380px"
        @change="handleFilterChange"
      />

      <el-button type="primary" @click="handleRefresh">
        <el-icon><Refresh /></el-icon> 刷新数据
      </el-button>

      <el-button @click="handleExport">
        <el-icon><Download /></el-icon> 导出数据
      </el-button>
    </div>

    <!-- 实时图像按钮 -->
    <div class="page-header">
      <h2>虫情监测预警</h2>
      <el-button type="primary" @click="showImageDialog = true">
        <el-icon><Camera /></el-icon> 查看实时图像
      </el-button>
    </div>

    <div class="main-grid">
      <div class="card alert-card">
        <div class="card-header">
          <h3>⚠️ 当前预警</h3>
        </div>
        <div class="card-body">
          <div class="alert-list">
            <div v-for="alert in alerts" :key="alert.id" class="alert-item" :class="alert.level">
              <div class="alert-icon"><Warning /></div>
              <div class="alert-content">
                <div class="alert-title">{{ alert.title }}</div>
                <div class="alert-desc">{{ alert.desc }}</div>
                <div class="alert-time">{{ alert.time }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <h3>虫情趋势</h3>
        </div>
        <div class="card-body">
          <div ref="pestChart" class="chart"></div>
        </div>
      </div>
    </div>

    <!-- 实时图像弹窗 -->
    <el-dialog v-model="showImageDialog" title="虫情实时图像" width="900px" destroy-on-close @open="loadDevices">
      <div class="image-dialog">
        <!-- 图片网格 -->
        <div v-loading="imageLoading" class="image-grid">
          <div v-if="imageList.length === 0 && !imageLoading" class="empty-tip">
            暂无虫情图像数据
          </div>
          <div v-for="item in imageList" :key="item.id" class="image-card">
            <div class="image-wrapper" @click="previewImage(item)">
              <img :src="getFullImageUrl(item.imageUrl)" :alt="item.recordTime" loading="lazy" />
              <div class="image-overlay">
                <span class="object-count">{{ item.objectCount || 0 }} 头</span>
              </div>
            </div>
            <div class="image-info">
              <div class="record-time">{{ formatTime(item.recordTime) }}</div>
              <div class="detect-result">{{ item.objectCount > 0 ? '有虫害' : '无虫害' }}</div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="imagePage"
            v-model:page-size="imageSize"
            :total="imageTotal"
            :page-sizes="[9, 18, 36]"
            layout="total, sizes, prev, pager, next"
            @size-change="loadImageData"
            @current-change="loadImageData"
          />
        </div>
      </div>
    </el-dialog>

    <!-- 图片预览 -->
    <el-dialog v-model="showPreview" title="虫情图像预览" width="80%" destroy-on-close>
      <div v-if="previewItem" class="preview-container">
        <div class="preview-image-wrap">
          <img :src="getFullImageUrl(previewItem.imageUrl)" :alt="previewItem.recordTime" />
        </div>
        <div class="preview-info">
          <p><strong>设备：</strong>{{ previewItem.devName || 'ft202604001' }}</p>
          <p><strong>IMEI：</strong>{{ previewItem.imei }}</p>
          <p><strong>记录时间：</strong>{{ formatTime(previewItem.recordTime) }}</p>
          <p><strong>检测数量：</strong>{{ previewItem.objectCount || 0 }} 头</p>
          <p><strong>检测结果：</strong>{{ previewItem.objectCount > 0 ? '有虫害' : '无虫害' }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue'
import { Camera, Warning, Refresh, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { insectApi } from '@/api'

const pestChart = ref(null)
const showImageDialog = ref(false)
const showPreview = ref(false)
const previewItem = ref(null)
const imageLoading = ref(false)
const imageList = ref([])
const imagePage = ref(1)
const imageSize = ref(9)
const imageTotal = ref(0)
const deviceList = ref([])

const filterForm = reactive({
  imei: '',
  dateRange: null
})

const alerts = ref([])

// 默认时间范围：最近7天
const now = new Date()
const defaultStart = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
const dateRange = ref([])

function getFullImageUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return `http://182.40.36.95:4098/${url}`
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ').substring(0, 19)
}

function handleFilterChange() {
  loadImageData()
  loadAlertsData()
}

function handleRefresh() {
  loadImageData()
  loadAlertsData()
  initChart()
}

function handleExport() {
  ElMessage.info('导出功能开发中')
}

async function loadDevices() {
  try {
    const res = await insectApi.getLocalDevices()
    deviceList.value = res.records || []
    if (deviceList.value.length > 0 && !filterForm.imei) {
      filterForm.imei = deviceList.value[0].imei
      loadImageData()
      loadAlertsData()
    }
  } catch (e) {
    ElMessage.error('加载设备列表失败: ' + e.message)
  }
}

async function loadAlertsData() {
  if (!filterForm.imei) return
  try {
    const res = await insectApi.getDataList({
      imei: filterForm.imei,
      page: 1,
      size: 100
    })
    const dataList = res.records || []

    // 更新图表
    initChart(dataList)

    // 根据objectCount生成预警
    const alertList = []
    dataList.forEach((item, index) => {
      if (item.objectCount > 0) {
        alertList.push({
          id: item.id || index,
          level: item.objectCount > 5 ? 'high' : 'medium',
          title: `${item.devName || filterForm.imei} 发现虫害`,
          desc: `检测到 ${item.objectCount} 头害虫，建议及时处理`,
          time: item.recordTime ? formatTime(item.recordTime) : '未知时间'
        })
      }
    })
    alerts.value = alertList
  } catch (e) {
    console.error('加载预警数据失败', e)
  }
}

async function loadImageData() {
  if (!filterForm.imei) return
  imageLoading.value = true
  try {
    const [startDate, endDate] = filterForm.dateRange || []
    const res = await insectApi.getDataList({
      imei: filterForm.imei,
      startDate,
      endDate,
      page: imagePage.value,
      size: imageSize.value
    })
    imageList.value = res.records || []
    imageTotal.value = res.total || 0
  } catch (e) {
    ElMessage.error('加载虫情数据失败: ' + e.message)
    imageList.value = []
  } finally {
    imageLoading.value = false
  }
}

function previewImage(item) {
  previewItem.value = item
  showPreview.value = true
}

function initChart(dataList) {
  if (!pestChart.value) return
  const chart = echarts.init(pestChart.value)

  // 按日期统计虫情数据
  const dateMap = {}
  dataList.forEach(item => {
    if (item.recordTime) {
      const date = item.recordTime.substring(0, 10)
      dateMap[date] = (dateMap[date] || 0) + (item.objectCount || 0)
    }
  })

  const labels = Object.keys(dateMap).sort()
  const values = labels.map(d => dateMap[d])

  if (labels.length === 0) {
    labels.push('暂无数据')
    values.push(0)
  }

  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['虫口数量'], bottom: 0 },
    xAxis: {
      type: 'category',
      data: labels
    },
    yAxis: { type: 'value', name: '虫口数量(头)' },
    series: [
      {
        name: '虫口数量',
        type: 'line',
        data: values,
        itemStyle: { color: '#f5222d' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(245, 34, 45, 0.3)' },
            { offset: 1, color: 'rgba(245, 34, 45, 0.05)' }
          ])
        }
      }
    ]
  }
  chart.setOption(option)
}

onMounted(() => {
  loadDevices()
  loadImageData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

function handleResize() {
  // 重新渲染图表
}
</script>

<style scoped>
.pest-page { padding: 0; }

.filter-row {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h2 { margin: 0; }
.main-grid {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 20px;
}
.card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.alert-card {
  background: #fff1f0;
  border: 1px solid #ffccc7;
}
.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}
.card-header h3 { margin: 0; font-size: 16px; font-weight: 600; }
.card-body { padding: 20px; }
.alert-list { display: flex; flex-direction: column; gap: 12px; }
.alert-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  border-left: 4px solid #faad14;
}
.alert-item.high { border-left-color: #f5222d; }
.alert-icon { font-size: 24px; color: #faad14; }
.alert-item.high .alert-icon { color: #f5222d; }
.alert-title { font-weight: 600; color: #262626; }
.alert-desc { font-size: 13px; color: #595959; margin-top: 4px; }
.alert-time { font-size: 12px; color: #bfbfbf; margin-top: 8px; }
.chart { height: 400px; }

/* 弹窗样式 */
.image-dialog { display: flex; flex-direction: column; gap: 16px; }
.date-range-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.total-tip { color: #666; font-size: 13px; margin-left: 8px; }
.image-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  max-height: 500px;
  overflow-y: auto;
  padding: 4px;
}
.empty-tip { grid-column: 1/-1; text-align: center; color: #999; padding: 40px; }
.image-card {
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  background: #fafafa;
}
.image-wrapper {
  position: relative;
  height: 160px;
  overflow: hidden;
  cursor: pointer;
}
.image-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}
.image-wrapper:hover img { transform: scale(1.05); }
.image-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0,0,0,0.6));
  padding: 6px 8px;
  display: flex;
  justify-content: flex-end;
}
.object-count {
  background: rgba(0,0,0,0.5);
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}
.image-info {
  padding: 8px;
  text-align: center;
}
.record-time { font-size: 12px; color: #333; }
.detect-result { font-size: 11px; color: #1890ff; margin-top: 2px; }
.pagination-bar { display: flex; justify-content: center; padding-top: 8px; }

/* 预览样式 */
.preview-container { display: flex; gap: 20px; }
.preview-image-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
  max-height: 70vh;
}
.preview-image-wrap img { max-width: 100%; max-height: 70vh; object-fit: contain; }
.preview-info {
  width: 220px;
  flex-shrink: 0;
  padding: 16px;
  background: #f5f5f5;
  border-radius: 8px;
  height: fit-content;
}
.preview-info p { margin: 8px 0; font-size: 14px; color: #333; }
</style>
