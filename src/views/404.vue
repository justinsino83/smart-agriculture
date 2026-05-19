
<template>
  <div class="not-found">
    <div class="content">
      <div class="error-code">404</div>
      <div class="error-title">页面未找到</div>
      <div class="error-desc">抱歉，您访问的页面不存在或已被移除</div>
      <div class="actions">
        <button class="btn-primary" @click="goHome">返回首页</button>
        <button class="btn-secondary" @click="goBack">返回上页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.content {
  text-align: center;
  padding: 40px;
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  color: #2c5f2d;
  line-height: 1;
  margin-bottom: 20px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
}

.error-title {
  font-size: 28px;
  color: #333;
  margin-bottom: 12px;
}

.error-desc {
  font-size: 16px;
  color: #666;
  margin-bottom: 32px;
}

.actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.btn-primary,
.btn-secondary {
  padding: 12px 32px;
  border-radius: 8px;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
}

.btn-primary {
  background: #2c5f2d;
  color: #fff;
}

.btn-primary:hover {
  background: #1e4a1f;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(44, 95, 45, 0.3);
}

.btn-secondary {
  background: #fff;
  color: #2c5f2d;
  border: 1px solid #2c5f2d;
}

.btn-secondary:hover {
  background: #f0f7f0;
  transform: translateY(-1px);
}
</style>
