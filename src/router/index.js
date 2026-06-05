import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/components/Layout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '智慧大屏', icon: 'Monitor' }
      },
      {
        path: '/planting',
        name: 'Planting',
        redirect: '/planting/soil',
        meta: { title: '智慧种植', icon: 'Odometer' },
        children: [
          {
            path: '/planting/soil',
            name: 'SoilMonitor',
            component: () => import('@/views/planting/SoilMonitor.vue'),
            meta: { title: '土壤监测', icon: 'TrendCharts' }
          },
          {
            path: '/planting/irrigation',
            name: 'Irrigation',
            component: () => import('@/views/planting/Irrigation.vue'),
            meta: { title: '智能灌溉', icon: 'Coffee' }
          },
          {
            path: '/planting/weather',
            name: 'Weather',
            component: () => import('@/views/planting/Weather.vue'),
            meta: { title: '气象监测', icon: 'Sunny' }
          },
          {
            path: '/planting/pest',
            name: 'Pest',
            component: () => import('@/views/planting/Pest.vue'),
            meta: { title: '虫情监测', icon: 'Warning' }
          }
        ]
      },
      {
        path: '/drying',
        name: 'Drying',
        component: () => import('@/views/drying/index.vue'),
        meta: { title: '绿色烘干', icon: 'Sunrise' }
      },
      {
        path: '/storage',
        name: 'Storage',
        component: () => import('@/views/storage/index.vue'),
        meta: { title: '智慧仓储', icon: 'Box' }
      },
      {
        path: '/energy',
        name: 'Energy',
        component: () => import('@/views/energy/index.vue'),
        meta: { title: '能耗管理', icon: 'Lightning' }
      },
      {
        path: '/system',
        name: 'System',
        redirect: '/system/devices',
        meta: { title: '系统管理', icon: 'Tools' },
        children: [
          {
            path: '/system/devices',
            name: 'Devices',
            component: () => import('@/views/system/DeviceManagement.vue'),
            meta: { title: '设备管理', icon: 'Monitor' }
          },
          {
            path: '/system/users',
            name: 'Users',
            component: () => import('@/views/system/UserManagement.vue'),
            meta: { title: '用户管理', icon: 'User' }
          },
          {
            path: '/system/llm',
            name: 'LlmModel',
            component: () => import('@/views/llm/index.vue'),
            meta: { title: '模型管理', icon: 'Cpu' }
          }
        ]
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/404.vue'),
    meta: { title: '页面未找到', public: true }
  }
]

const router = createRouter({
  history: createWebHistory('/agridigital/'),
  routes
})

// 路由守卫 - 登录验证
router.beforeEach((to, from, next) => {
  // 公开路由直接放行
  if (to.meta.public) {
    return next()
  }
  
  // 检查是否已登录 - 同时检查 localStorage 和 sessionStorage
  const hasToken = !!(localStorage.getItem('token') || sessionStorage.getItem('token'))
  
  if (!hasToken) {
    return next('/login')
  }
  next()
})

// 设置页面标题
router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 智慧农业平台` : '智慧农业平台'
})

export default router