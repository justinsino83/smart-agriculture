<template>
  <div class="layout">
    <el-container class="layout-container">
      <!-- 侧边栏 -->
      <el-aside width="220px" class="sidebar">
        <div class="logo">
          <img src="/logo.svg" alt="logo" v-if="false" />
          <span class="logo-text">智慧农业平台</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          background-color="#001529"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          router
        >
          <template v-for="route in menuRoutes" :key="route.path">
            <!-- 单层菜单 -->
            <el-menu-item v-if="!route.children" :index="route.path">
              <el-icon><component :is="route.meta.icon" /></el-icon>
              <span>{{ route.meta.title }}</span>
            </el-menu-item>
            
            <!-- 多级菜单 -->
            <el-sub-menu v-else :index="route.path">
              <template #title>
                <el-icon><component :is="route.meta.icon" /></el-icon>
                <span>{{ route.meta.title }}</span>
              </template>
              <el-menu-item 
                v-for="child in route.children" 
                :key="child.path"
                :index="child.path"
              >
                {{ child.meta.title }}
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </el-aside>
      
      <!-- 主内容区 -->
      <el-container class="main-container">
        <!-- 顶部导航 -->
        <el-header class="header">
          <div class="breadcrumb">
            <el-breadcrumb>
              <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-if="currentRoute.meta?.title">{{ currentRoute.meta.title }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          
          <div class="header-right">
            <span class="weather">
              <el-icon><Sunny /></el-icon>
              24°C 晴朗
            </span>
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-avatar :size="32" icon="UserFilled" />
                <span class="username">{{ userStore.userInfo.username || '管理员' }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                  <el-dropdown-item command="settings">系统设置</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>
        
        <!-- 内容区 -->
        <el-main class="main-content">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  DataLine, Crop, HotWater, Box, Lightning, Setting,
  Sunny, ArrowDown, UserFilled
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const currentRoute = computed(() => route)
const activeMenu = computed(() => route.path)

const menuRoutes = [
  { path: '/dashboard', meta: { title: '智慧农业大屏', icon: 'DataLine' } },
  {
    path: '/planting',
    meta: { title: '智慧种植', icon: 'Crop' },
    children: [
      { path: '/planting/soil', meta: { title: '土壤监测' } },
      { path: '/planting/weather', meta: { title: '气象监测' } },
      { path: '/planting/pest', meta: { title: '虫情预警' } },
      { path: '/planting/irrigation', meta: { title: '智能灌溉' } }
    ]
  },
  { path: '/drying', meta: { title: '绿色烘干', icon: 'HotWater' } },
  { path: '/storage', meta: { title: '智慧仓储', icon: 'Box' } },
  { path: '/energy', meta: { title: '能源管理', icon: 'Lightning' } },
  {
    path: '/system',
    meta: { title: '系统管理', icon: 'Setting' },
    children: [
      { path: '/system/users', meta: { title: '用户管理' } },
      { path: '/system/devices', meta: { title: '设备管理' } },
      { path: '/system/alerts', meta: { title: '告警中心' } }
    ]
  }
]

const handleCommand = (command) => {
  switch (command) {
    case 'logout':
      userStore.logout()
      router.push('/login')
      break
    case 'profile':
      router.push('/system/users')
      break
    case 'settings':
      router.push('/system/devices')
      break
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.layout-container {
  height: 100%;
}

.sidebar {
  background-color: #001529;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #002140;
}

.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.sidebar-menu {
  border-right: none;
}

.main-container {
  background-color: #f0f2f5;
}

.header {
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.breadcrumb {
  font-size: 14px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.weather {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 14px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: #606266;
}

.main-content {
  padding: 20px;
  overflow-y: auto;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>