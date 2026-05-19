<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-left">
        <div class="brand">
          <el-icon class="logo-icon"><Sunny /></el-icon>
          <h1>智慧农业管理平台</h1>
          <p>江苏维明农业科技有限公司</p>
        </div>
        <div class="features">
          <div class="feature-item">
            <el-icon><Monitor /></el-icon>
            <span>实时监测</span>
          </div>
          <div class="feature-item">
            <el-icon><Cpu /></el-icon>
            <span>智能控制</span>
          </div>
          <div class="feature-item">
            <el-icon><TrendCharts /></el-icon>
            <span>数据分析</span>
          </div>
        </div>
      </div>
      
      <div class="login-right">
        <div class="login-form-wrapper">
          <h2>欢迎登录</h2>
          <p class="subtitle">请使用您的账号密码登录系统</p>
          
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            class="login-form"
            @keyup.enter="handleLogin"
          >
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                :prefix-icon="User"
                size="large"
              />
            </el-form-item>
            
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                size="large"
                show-password
              />
            </el-form-item>
            
            <el-form-item prop="captcha">
              <div class="captcha-row">
                <el-input
                  v-model="loginForm.captcha"
                  placeholder="请输入验证码"
                  :prefix-icon="Key"
                  size="large"
                  class="captcha-input"
                />
                <div class="captcha-img" @click="refreshCaptcha">
                  {{ captchaCode }}
                </div>
              </div>
            </el-form-item>
            
            <div class="login-options">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              <el-link type="primary" :underline="false">忘记密码？</el-link>
            </div>
            
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                class="login-btn"
                :loading="loading"
                @click="handleLogin"
              >
                登 录
              </el-button>
            </el-form-item>
          </el-form>
          
          <div class="login-tips">
            <el-alert
              title="演示账号"
              type="info"
              :closable="false"
              description="用户名：admin | 密码：123456"
            />
          </div>
        </div>
      </div>
    </div>
    
    <div class="login-footer">
      <p>© 2026 江苏维明农业科技有限公司 版权所有</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Sunny, Monitor, Cpu, TrendCharts, User, Lock, Key } from '@element-plus/icons-vue'
import { userApi } from '@/api/modules'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loginFormRef = ref(null)
const loading = ref(false)
const rememberMe = ref(false)
const captchaCode = ref('')

const loginForm = reactive({
  username: '',
  password: '',
  captcha: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 4, message: '验证码为4位字符', trigger: 'blur' }
  ]
}

// 生成验证码
const generateCaptcha = () => {
  const chars = 'ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz2345678'
  let code = ''
  for (let i = 0; i < 4; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  captchaCode.value = code
}

const refreshCaptcha = () => {
  generateCaptcha()
  loginForm.captcha = ''
}

const handleLogin = async () => {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true

  try {
    const res = await userApi.login({
      username: loginForm.username,
      password: loginForm.password
    })

    // 保存登录状态
    const userInfoData = {
      username: loginForm.username,
      nickname: res.nickname || res.username || loginForm.username,
      avatar: res.avatar || '',
      roles: res.roles || ['admin'],
      loginTime: new Date().toISOString()
    }

    userStore.setToken(res.token)
    userStore.setUserInfo(userInfoData)

    if (rememberMe.value) {
      localStorage.setItem('userInfo', JSON.stringify(userInfoData))
      localStorage.setItem('isLoggedIn', 'true')
    } else {
      sessionStorage.setItem('userInfo', JSON.stringify(userInfoData))
      sessionStorage.setItem('isLoggedIn', 'true')
    }

    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error) {
    ElMessage.error(error.message || '登录失败，请稍后重试')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  generateCaptcha()
  
  // 检查是否已登录
  const isLoggedIn = localStorage.getItem('isLoggedIn') || sessionStorage.getItem('isLoggedIn')
  if (isLoggedIn) {
    router.push('/dashboard')
  }
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  flex-direction: column;
}

.login-container {
  flex: 1;
  display: flex;
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
  width: 100%;
}

.login-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: #fff;
  padding-right: 60px;
}

.brand {
  margin-bottom: 60px;
}

.logo-icon {
  font-size: 64px;
  color: #52c41a;
  margin-bottom: 20px;
}

.brand h1 {
  font-size: 42px;
  font-weight: 600;
  margin: 0 0 16px 0;
}

.brand p {
  font-size: 20px;
  opacity: 0.9;
  margin: 0;
}

.features {
  display: flex;
  gap: 40px;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.feature-item .el-icon {
  font-size: 36px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 12px;
}

.feature-item span {
  font-size: 16px;
}

.login-right {
  width: 440px;
  display: flex;
  align-items: center;
}

.login-form-wrapper {
  width: 100%;
  background: #fff;
  border-radius: 16px;
  padding: 48px 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-form-wrapper h2 {
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #262626;
}

.subtitle {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0 0 32px 0;
}

.login-form {
  margin-bottom: 24px;
}

.captcha-row {
  display: flex;
  gap: 12px;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  width: 120px;
  height: 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 4px;
  cursor: pointer;
  user-select: none;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.login-btn {
  width: 100%;
  font-size: 16px;
}

.login-tips {
  margin-top: 24px;
}

.login-footer {
  text-align: center;
  padding: 24px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

@media (max-width: 992px) {
  .login-left {
    display: none;
  }
  
  .login-right {
    width: 100%;
    max-width: 440px;
    margin: 0 auto;
  }
}
</style>
