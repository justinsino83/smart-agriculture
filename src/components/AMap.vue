<template>
  <div class="amap-container">
    <div id="amap" ref="mapRef" class="map"></div>
    
    <!-- 地块信息弹窗 -->
    <div v-if="selectedField" class="field-popup" :style="popupStyle">
      <div class="popup-header">
        <h4>{{ selectedField.name }}</h4>
        <el-icon class="close-btn" @click="selectedField = null"><Close /></el-icon>
      </div>
      <div class="popup-content">
        <p><label>作物：</label>{{ selectedField.crop }}</p>
        <p><label>面积：</label>{{ selectedField.area }} 亩</p>
        <p><label>生长期：</label>{{ selectedField.growthStage }}</p>
        <p>
          <label>土壤湿度：</label>
          <span :class="selectedField.moistureLevel">{{ selectedField.moisture }}% {{ selectedField.moistureLevel }}</span>
        </p>
        <p v-if="selectedField.fertilizeNeeded">
          <el-tag type="warning" size="small">需施肥</el-tag>
        </p>
      </div>
    </div>
    
    <!-- 地图图例 -->
    <div class="map-legend">
      <div class="legend-item"><span class="dot normal"></span>适宜</div>
      <div class="legend-item"><span class="dot dry"></span>干旱</div>
      <div class="legend-item"><span class="dot wet"></span>过湿</div>
      <div class="legend-item"><span class="dot fertilize"></span>需施肥</div>
    </div>
    
    <!-- 地图控制按钮 -->
    <div class="map-controls">
      <el-button circle size="small" @click="zoomIn"><el-icon><Plus /></el-icon></el-button>
      <el-button circle size="small" @click="zoomOut"><el-icon><Minus /></el-icon></el-button>
      <el-button circle size="small" @click="resetView"><el-icon><RefreshRight /></el-icon></el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Close, Plus, Minus, RefreshRight } from '@element-plus/icons-vue'
import AMapLoader from '@amap/amap-jsapi-loader'

const props = defineProps({
  fields: {
    type: Array,
    default: () => []
  }
})

const mapRef = ref(null)
let map = null
let markers = []
const selectedField = ref(null)
const popupStyle = ref({})

// 高德地图Key和安全密钥
const MAP_KEY = '60553215d1d96bc041e6aacd8daa0e2e'
const SECURITY_CONFIG = 'a9246383dfc0da5cf16808b4b0c32236'

// 初始化地图
const initMap = async () => {
  try {
    // 设置安全密钥
    window._AMapSecurityConfig = {
      securityJsCode: SECURITY_CONFIG
    }
    
    const AMap = await AMapLoader.load({
      key: MAP_KEY,
      version: '2.0',
      plugins: ['AMap.Marker', 'AMap.InfoWindow', 'AMap.Scale', 'AMap.ToolBar']
    })
    
    // 创建地图实例 - 以江苏淮安地区为中心（示例坐标）
    map = new AMap.Map('amap', {
      zoom: 13,
      center: [118.8396, 33.0196], // 淮安坐标
      viewMode: '2D',
      mapStyle: 'amap://styles/whitesmoke' // 浅色风格
    })
    
    // 添加比例尺和工具条
    map.addControl(new AMap.Scale())
    map.addControl(new AMap.ToolBar({
      position: 'RB'
    }))
    
    // 添加地块标记
    addFieldMarkers(AMap)
    
  } catch (error) {
    console.error('地图加载失败:', error)
  }
}

// 添加地块标记
const addFieldMarkers = (AMap) => {
  // 清除已有标记
  markers.forEach(marker => marker.setMap(null))
  markers = []
  
  props.fields.forEach((field, index) => {
    // 根据状态确定颜色
    const colors = {
      normal: '#52c41a',
      dry: '#faad14', 
      wet: '#1890ff',
      fertilize: '#fa8c16'
    }
    const color = colors[field.status] || colors.normal
    
    // 创建圆形标记表示地块
    const circle = new AMap.Circle({
      center: [field.lng || 118.8396 + index * 0.01, field.lat || 33.0196 + index * 0.005],
      radius: Math.sqrt(field.area) * 20, // 根据面积计算半径
      fillColor: color,
      fillOpacity: 0.6,
      strokeColor: color,
      strokeWeight: 2,
      extData: field // 存储地块数据
    })
    
    circle.setMap(map)
    
    // 点击事件
    circle.on('click', (e) => {
      selectedField.value = field
      // 设置弹窗位置
      const pixel = map.lngLatToContainer(e.lnglat)
      popupStyle.value = {
        left: pixel.x + 'px',
        top: pixel.y + 'px'
      }
    })
    
    markers.push(circle)
    
    // 添加文字标签
    const text = new AMap.Text({
      text: field.name,
      anchor: 'center',
      draggable: false,
      cursor: 'pointer',
      style: {
        'font-size': '12px',
        'font-weight': 'bold',
        color: '#fff',
        'text-shadow': '0 1px 2px rgba(0,0,0,0.5)'
      },
      position: [field.lng || 118.8396 + index * 0.01, field.lat || 33.0196 + index * 0.005]
    })
    
    text.setMap(map)
    markers.push(text)
  })
}

// 地图控制
const zoomIn = () => map && map.zoomIn()
const zoomOut = () => map && map.zoomOut()
const resetView = () => {
  if (map) {
    map.setZoomAndCenter(13, [118.8396, 33.0196])
  }
}

onMounted(() => {
  initMap()
})

onUnmounted(() => {
  if (map) {
    map.destroy()
  }
})
</script>

<style scoped>
.amap-container {
  position: relative;
  width: 100%;
  height: 100%;
}

.map {
  width: 100%;
  height: 100%;
  border-radius: 8px;
}

/* 地块信息弹窗 */
.field-popup {
  position: absolute;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  padding: 16px;
  min-width: 200px;
  z-index: 100;
  transform: translate(-50%, -120%);
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.popup-header h4 {
  margin: 0;
  font-size: 16px;
  color: #262626;
}

.close-btn {
  cursor: pointer;
  color: #8c8c8c;
}

.close-btn:hover {
  color: #262626;
}

.popup-content p {
  margin: 8px 0;
  font-size: 14px;
  color: #595959;
}

.popup-content label {
  color: #8c8c8c;
  margin-right: 8px;
}

.popup-content .适宜 {
  color: #52c41a;
  font-weight: 500;
}

.popup-content .轻旱,
.popup-content .中旱 {
  color: #faad14;
  font-weight: 500;
}

.popup-content .过湿 {
  color: #1890ff;
  font-weight: 500;
}

/* 图例 */
.map-legend {
  position: absolute;
  bottom: 16px;
  right: 16px;
  background: rgba(255, 255, 255, 0.95);
  padding: 12px 16px;
  border-radius: 8px;
  display: flex;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #595959;
}

.legend-item .dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.dot.normal {
  background: #52c41a;
}

.dot.dry {
  background: #faad14;
}

.dot.wet {
  background: #1890ff;
}

.dot.fertilize {
  background: #fa8c16;
}

/* 地图控制按钮 */
.map-controls {
  position: absolute;
  top: 16px;
  right: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
