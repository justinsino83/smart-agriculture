<template>
  <div class="user-page">
    <div class="page-header">
      <div class="header-left">
        <h2>用户权限管理</h2>
        <el-tag type="info">共 {{ userList.length }} 位用户</el-tag>
      </div>
      <div class="header-actions">
        <el-button type="primary">
          <el-icon>
            <Plus />
          </el-icon> 添加用户
        </el-button>
      </div>
    </div>

    <div class="main-content">
      <!-- 用户统计 -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon blue">
            <el-icon>
              <User />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ totalUsers }}</div>
            <div class="stat-label">总用户数</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon green">
            <el-icon>
              <CircleCheck />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ activeUsers }}</div>
            <div class="stat-label">在线用户</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon yellow">
            <el-icon>
              <UserFilled />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ adminCount }}</div>
            <div class="stat-label">管理员</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon purple">
            <el-icon>
              <Setting />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ roleTypes.length }}</div>
            <div class="stat-label">角色类型</div>
          </div>
        </div>
      </div>

      <!-- 用户列表 -->
      <div class="card">
        <div class="card-header">
          <h3>用户列表</h3>
          <div class="header-actions">
            <el-input v-model="searchKeyword" placeholder="搜索用户名/姓名" style="width: 220px" clearable>
              <template #prefix><el-icon>
                  <Search />
                </el-icon></template>
            </el-input>
          </div>
        </div>

        <div class="card-body">
          <el-table :data="filteredUserList" stripe style="width: 100%" height="calc(100vh - 320px)"
            :cell-style="{ padding: '10px 0' }"
            :header-cell-style="{ padding: '12px 0', background: '#fafafa', color: '#262626' }">
            <el-table-column type="index" label="#" width="60" align="center" />

            <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
            <el-table-column prop="realName" label="姓名" min-width="100" show-overflow-tooltip />
            <el-table-column prop="role" label="角色" min-width="110">
              <template #default="{ row }">
                <el-tag :type="getRoleType(row.role)" size="small">{{ row.role }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="department" label="部门" min-width="120" show-overflow-tooltip />
            <el-table-column prop="phone" label="手机号" min-width="130" />

            <el-table-column prop="status" label="状态" min-width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === '启用' ? 'success' : 'info'" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastLogin" label="最后登录" min-width="160" />

            <el-table-column label="操作" width="140" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small">编辑</el-button>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Plus, Search, User, UserFilled, Setting, CircleCheck } from '@element-plus/icons-vue'

const searchKeyword = ref('')

const defaultUserList = [
  { username: 'admin', realName: '系统管理员', role: '超级管理员', department: '技术部', phone: '138****8888', status: '启用', lastLogin: '2024-03-20 10:15' },
  { username: 'operator1', realName: '张三', role: '操作员', department: '生产部', phone: '139****6666', status: '启用', lastLogin: '2024-03-20 09:30' },
  { username: 'viewer1', realName: '李四', role: '查看员', department: '质检部', phone: '137****5555', status: '启用', lastLogin: '2024-03-19 16:20' }
]

const userList = ref([...defaultUserList])

// 计算属性
const totalUsers = computed(() => userList.value.length)
const activeUsers = computed(() => userList.value.filter(u => u.status === '启用').length)
const adminCount = computed(() => userList.value.filter(u => u.role === '超级管理员' || u.role === '管理员').length)
const roleTypes = computed(() => [...new Set(userList.value.map(u => u.role))])

const filteredUserList = computed(() => {
  if (!searchKeyword.value) return userList.value
  const keyword = searchKeyword.value.toLowerCase()
  return userList.value.filter(item =>
    item.username.toLowerCase().includes(keyword) ||
    item.realName.toLowerCase().includes(keyword)
  )
})

const getRoleType = (role) => {
  const map = { '超级管理员': 'danger', '管理员': 'warning', '操作员': 'primary', '查看员': 'info' }
  return map[role] || 'info'
}
</script>

<style scoped>
.user-page {
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left h2 {
  margin: 0;
  font-size: 20px;
  color: #262626;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.stat-icon.blue {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
}

.stat-icon.green {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
}

.stat-icon.yellow {
  background: linear-gradient(135deg, #faad14 0%, #ffc53d 100%);
}

.stat-icon.purple {
  background: linear-gradient(135deg, #722ed1 0%, #9254de 100%);
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #262626;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #8c8c8c;
  margin-top: 4px;
}

.card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.card-body {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
