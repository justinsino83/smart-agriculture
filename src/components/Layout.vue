<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon><Monitor /></el-icon>
        <span>智慧农业平台</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        background-color="#001529"
        text-color="#a6adb4"
        active-text-color="#fff"
        router
        collapse-transition
      >
        <template v-for="route in menuRoutes" :key="route.path">
          <!-- 无子菜单 -->
          <el-menu-item v-if="!route.children" :index="route.path">
            <el-icon v-if="route.meta?.icon && iconMap[route.meta.icon]">
              <component :is="iconMap[route.meta.icon]" />
            </el-icon>
            <span>{{ route.meta?.title }}</span>
          </el-menu-item>

          <!-- 有子菜单 -->
          <el-sub-menu v-else :index="route.path">
            <template #title>
              <el-icon v-if="route.meta?.icon && iconMap[route.meta.icon]">
                <component :is="iconMap[route.meta.icon]" />
              </el-icon>
              <span>{{ route.meta?.title }}</span>
            </template>

            <el-menu-item 
              v-for="child in route.children" 
              :key="child.path" 
              :index="child.path"
            >
              <el-icon v-if="child.meta?.icon && iconMap[child.meta.icon]">
                <component :is="iconMap[child.meta.icon]" />
              </el-icon>
              <span>{{ child.meta?.title }}</span>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>

      <div class="sidebar-footer">
        <div class="user-info">
          <el-avatar :size="32" :icon="User" />
          <span>管理员</span>
        </div>
      </div>
    </el-aside>

    <!-- 主内容区 -->
    <el-container class="main-container">
      <!-- 顶部导航 -->
      <el-header class="header">
        <breadcrumb-nav />

        <div class="header-right">
          <!-- <el-badge :value="3" class="badge">
            <el-icon><Warning /></el-icon>
          </el-badge> -->

          <el-dropdown @command="handleCommand">
            <span class="user-dropdown">
              <el-avatar :size="28" :icon="User" />
              <span>{{ userInfo.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>

            <template #dropdown>
              <el-dropdown-menu>
                <!-- <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="settings">系统设置</el-dropdown-item> -->
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
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Monitor, Odometer, TrendCharts, Coffee, Sunny, Warning,
  Sunrise, Box, Lightning, Tools, User, ArrowDown, Cpu
} from '@element-plus/icons-vue'
import BreadcrumbNav from './BreadcrumbNav.vue'
import router from '@/router'
import { useUserStore } from '@/stores/user'

// 图标映射表
const iconMap = {
  Monitor,
  Odometer,
  TrendCharts,
  Coffee,
  Sunny,
  Warning,
  Sunrise,
  Box,
  Lightning,
  Tools,
  User,
  ArrowDown,
  Cpu
}

const route = useRoute()
const vueRouter = useRouter()
const userStore = useUserStore()
const menuRoutes = computed(() => router.getRoutes().find(r => r.path === '/')?.children || [])
const activeMenu = computed(() => route.path)

// 获取用户信息
const userInfo = computed(() => {
  return userStore.userInfo && Object.keys(userStore.userInfo).length > 0 
    ? userStore.userInfo 
    : { nickname: '管理员' }
})

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // 使用 store 统一清除登录状态
    userStore.logout()
    
    ElMessage.success('已退出登录')
    vueRouter.push('/login')
  }).catch(() => {})
}

// 处理下拉菜单命令
const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      ElMessage.info('个人中心功能开发中')
      break
    case 'settings':
      ElMessage.info('系统设置功能开发中')
      break
    case 'logout':
      handleLogout()
      break
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background: #001529;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.logo .el-icon {
  font-size: 28px;
  color: #52c41a;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 50px;
  line-height: 50px;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: #1890ff !important;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255,255,255,0.1);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #a6adb4;
}

.main-container {
  background: #f0f2f5;
}

.header {
  height: 64px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  z-index: 100;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.badge :deep(.el-badge__content) {
  top: 8px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.3s;
}

.user-dropdown:hover {
  background: #f5f5f5;
}

.main-content {
  padding: 20px;
  overflow-y: auto;
}

/* 页面切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
