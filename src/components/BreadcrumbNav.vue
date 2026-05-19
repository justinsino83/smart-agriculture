<template>
  <el-breadcrumb separator="/">
    <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index"
    >
      <router-link v-if="item.path && index < breadcrumbs.length - 1" :to="item.path"
    >
        {{ item.title }}
      </router-link>
      <span v-else>{{ item.title }}</span>
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const breadcrumbs = computed(() => {
  const matched = route.matched
  const list = []
  
  matched.forEach(r => {
    if (r.meta?.title) {
      list.push({
        title: r.meta.title,
        path: r.path
      })
    }
  })
  
  return list
})
</script>

<style scoped>
:deep(.el-breadcrumb__inner) {
  font-size: 14px;
}

:deep(.el-breadcrumb__inner.is-link) {
  color: #1890ff;
}

:deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: #262626;
  font-weight: 500;
}
</style>