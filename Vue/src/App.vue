<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import Header from './components/Header.vue'
import Footer from './components/Footer.vue'
import tokenManager from '@/utils/tokenManager'
import notificationWS from '@/utils/websocket'
import { log, error as logError } from '@/utils/logger'

const route = useRoute()

// 判断是否显示主布局（有 Header）
const showMainLayout = computed(() => {
  return route.path !== '/login' && route.path !== '/register'
})

// 检查登录状态（用于 Header 显示用户信息）
const isLoggedIn = computed(() => {
  return tokenManager.isLoggedIn()
})

// 初始化全局 WebSocket 连接
const initGlobalWebSocket = () => {
  if (!tokenManager.isLoggedIn()) {
    log('未登录，跳过 WebSocket 连接')
    return
  }
  
  // 连接到 WebSocket
  notificationWS.connect()
  log('全局 WebSocket 已初始化')
}

// 组件挂载时初始化 WebSocket
onMounted(() => {
  initGlobalWebSocket()
})

// 组件卸载时断开 WebSocket（应用关闭时）
onUnmounted(() => {
  notificationWS.disconnect()
  log('全局 WebSocket 已断开')
})
</script>

<template>
  <div class="app-dashboard">
    <!-- 登录/注册页面 - 不显示 Header 和 Sidebar -->
    <router-view v-if="!showMainLayout" />
    
    <!-- 主应用界面 - 显示完整布局 -->
    <template v-else>
      <Header />
      <div class="dashboard-container">
        <router-view />
      </div>
      <Footer />
    </template>
  </div>
</template>

<style scoped>
.app-dashboard {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif;
  color: #333333;
  background-color: #f5f7fa;
  min-height: 100vh;
  width: 100%;
  display: flex;
  flex-direction: column;
}

.dashboard-container {
  display: flex;
  width: 100%;
  margin: 0;
  padding-top: 60px;
  flex: 1;
  overflow: hidden;
}
</style>