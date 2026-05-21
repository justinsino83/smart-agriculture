import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 辅助函数：从存储中读取用户信息
const getUserInfoFromStorage = () => {
  const info = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo')
  return info ? JSON.parse(info) : {}
}

// 辅助函数：从存储中读取token
const getTokenFromStorage = () => {
  return localStorage.getItem('token') || sessionStorage.getItem('token') || ''
}

export const useUserStore = defineStore('user', () => {
  const token = ref(getTokenFromStorage())
  const userInfo = ref(getUserInfoFromStorage())

  const isLoggedIn = computed(() => !!token.value)

  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
    sessionStorage.setItem('token', newToken)
  }

  const setUserInfo = (info) => {
    userInfo.value = info
    const infoStr = JSON.stringify(info)
    localStorage.setItem('userInfo', infoStr)
    sessionStorage.setItem('userInfo', infoStr)
  }

  const logout = () => {
    token.value = ''
    userInfo.value = {}
    // 完整清除所有存储中的认证信息
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
    // 清除可能遗留的旧标识
    localStorage.removeItem('isLoggedIn')
    sessionStorage.removeItem('isLoggedIn')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    setToken,
    setUserInfo,
    logout
  }
})