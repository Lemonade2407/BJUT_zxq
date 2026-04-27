<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTrendingProjects } from '@/api/project'
import { getNotifications, markAsRead as markNotificationRead, batchDeleteNotifications } from '@/api/notification'
import { toast } from '@/utils/toast'
import { error as logError, log } from '@/utils/logger'
import tokenManager from '@/utils/tokenManager'
import notificationWS from '@/utils/websocket'

const router = useRouter()

// 今日热门项目数据
const trendingRepos = ref([])
const isLoadingTrending = ref(false)

// 消息通知数据
const notifications = ref([])
const isLoadingNotifications = ref(false)

const displayedCount = ref(6) // 初始显示 6 条
const showAll = ref(false)
const isLoading = ref(false)
const notificationListRef = ref(null)

const getNotificationColor = (type) => {
  const colors = {
    1: '#f9ab00',      // 点赞
    2: '#0059b3',      // 评论
    3: '#e91e63',      // 关注
    4: '#5f6368'       // 系统通知
  }
  return colors[type] || '#5f6368'
}

const getNotificationIcon = (type) => {
  const icons = {
    1: '❤️',   // 点赞
    2: '💬',   // 评论
    3: '👁️',   // 关注
    4: '🔔'    // 系统通知
  }
  return icons[type] || '📢'
}

// 格式化时间
const formatTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  const now = new Date()
  const diff = now - date
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return date.toLocaleDateString('zh-CN')
}

// 加载热门项目
const loadTrendingProjects = async () => {
  isLoadingTrending.value = true
  try {
    const res = await getTrendingProjects({ limit: 10 })
    if (res.code === 200 && res.data) {
      // 转换数据格式以适应前端展示
      trendingRepos.value = res.data.map(project => ({
        id: project.id,
        name: project.name,
        description: project.description || '暂无描述',
        stars: project.starCount || 0,
        trend: project.viewCount > 100 ? 'hot' : project.viewCount > 50 ? 'rising' : 'stable',
        viewCount: project.viewCount || 0
      }))
    }
  } catch (error) {
    logError('加载热门项目失败:', error)
    // 只在非 401 错误时显示提示（401 会自动跳转登录）
    if (!error.message.includes('未授权') && !error.message.includes('Token')) {
      toast.error(error.message || '加载热门项目失败')
    }
  } finally {
    isLoadingTrending.value = false
  }
}

// 加载通知列表
const loadNotifications = async () => {
  // 检查是否已登录
  if (!tokenManager.isLoggedIn()) {
    // 未登录时不加载通知，静默返回
    return
  }
  
  isLoadingNotifications.value = true
  try {
    const res = await getNotifications({ pageNum: 1, pageSize: 50 })
    if (res.code === 200 && res.data) {
      // 转换数据格式
      notifications.value = res.data.map(notification => ({
        id: notification.id,
        type: notification.type,
        icon: getNotificationIcon(notification.type),
        title: getNotificationTitle(notification.type),
        content: notification.content,
        time: formatTime(notification.createdAt),
        unread: notification.isRead === 0,
        sender: notification.sender,
        projectId: notification.projectId
      }))
      
      // 重置显示数量
      displayedCount.value = 6
      showAll.value = false
    }
  } catch (error) {
    // Token 过期或无效时，request.js 会自动跳转到登录页，这里不需要额外处理
    // 只在非认证错误时才记录日志和显示提示
    if (!error.message.includes('未授权') && 
        !error.message.includes('Token') && 
        !error.message.includes('expired')) {
      logError('加载通知失败:', error)
      toast.error(error.message || '加载通知失败')
    }
  } finally {
    isLoadingNotifications.value = false
  }
}

// 获取通知标题
const getNotificationTitle = (type) => {
  const titles = {
    1: '点赞通知',
    2: '评论通知',
    3: '关注通知',
    4: '系统通知'
  }
  return titles[type] || '通知'
}

const loadMoreMessages = async () => {
  if (isLoadingNotifications.value || showAll.value) return
  
  isLoadingNotifications.value = true
  
  try {
    displayedCount.value += 6
    if (displayedCount.value >= notifications.value.length) {
      showAll.value = true
    }
  } catch (error) {
    logError('加载更多消息失败:', error)
  } finally {
    isLoadingNotifications.value = false
  }
}

const handleScroll = (event) => {
  const target = event.target
  const scrollTop = target.scrollTop
  const scrollHeight = target.scrollHeight
  const clientHeight = target.clientHeight
  
  // 当滚动到距离底部 50px 时加载更多
  if (scrollHeight - scrollTop - clientHeight < 50) {
    if (!showAll.value && !isLoadingNotifications.value) {
      loadMoreMessages()
    }
  }
}

const clearAllMessages = async () => {
  if (confirm('确定要清除所有消息吗？')) {
    try {
      // 获取所有通知的 ID
      const notificationIds = notifications.value.map(n => n.id)
      // 如果有通知，调用后端 API 批量删除
      if (notificationIds.length > 0) {
        await batchDeleteNotifications(notificationIds)
      }
      // 清空前端数据
      notifications.value = []
      displayedCount.value = 0
      showAll.value = true
      toast.success('已清除所有消息')
    } catch (error) {
      logError('清除消息失败:', error)
      toast.error(error.message || '清除消息失败')
    }
  }
}

const hasMoreMessages = computed(() => {
  return displayedCount.value < notifications.value.length
})

const displayedNotifications = computed(() => {
  return notifications.value.slice(0, displayedCount.value)
})

const unreadCount = computed(() => {
  return notifications.value.filter(n => n.unread).length
})

const markAsRead = async (id) => {
  const notification = notifications.value.find(n => n.id === id)
  if (notification && notification.unread) {
    try {
      await markNotificationRead(id)
      notification.unread = false
    } catch (error) {
      logError('标记为已读失败:', error)
      toast.error(error.message || '操作失败')
    }
  }
}

// 点击整个消息项时标记为已读
const handleNotificationClick = (id) => {
  markAsRead(id)
}

// 点击热门项目跳转
const handleProjectClick = (projectId) => {
  router.push(`/project/${projectId}`)
}

// 处理 WebSocket 接收到的新通知
const handleNewNotification = (data) => {
  log('收到实时通知:', data)
  
  // 构建通知对象
  const newNotification = {
    id: data.id,
    type: data.notificationType,
    icon: getNotificationIcon(data.notificationType),
    title: getNotificationTitle(data.notificationType),
    content: data.content,
    time: formatTime(data.createdAt),
    unread: data.isRead === 0,
    sender: data.sender,
    projectId: data.projectId
  }
  
  // 将新通知添加到列表顶部
  notifications.value.unshift(newNotification)
  
  // 如果当前显示全部，则增加显示数量
  if (showAll.value) {
    displayedCount.value++
  }
  
  // 通过 WebSocket 发送确认（标记为已读）
  if (newNotification.unread) {
    notificationWS.ackNotification(data.id)
    log('已发送通知确认:', data.id)
  }
  
  // 显示提示
  toast.info(`收到新通知：${newNotification.content}`)
  
  log('通知已添加到列表，当前通知数量:', notifications.value.length)
}

// 注册 WebSocket 消息监听器
const registerWebSocketListener = () => {
  notificationWS.on('notification', handleNewNotification)
  log('已注册 WebSocket 通知监听器')
}

// 组件挂载时加载数据并注册监听器
onMounted(() => {
  loadTrendingProjects()
  loadNotifications()
  registerWebSocketListener()
})

</script>

<template>
  <main class="app-main">
    <div class="main-content">
      <!-- Left Column: 今日热门项目 -->
      <div class="left-column">
        <section class="dashboard-section">
          <div class="section-header">
            <h2 class="section-title">今日热门项目</h2>
          </div>
          
          <div class="trending-list">
            <div v-if="isLoadingTrending" class="loading-state">
              <span class="loading-spinner">⟳</span>
              <span>加载中...</span>
            </div>
            <div v-else-if="trendingRepos.length === 0" class="empty-state">
              <span class="empty-icon">📭</span>
              <p class="empty-text">暂无热门项目</p>
            </div>
            <div v-for="repo in trendingRepos" :key="repo.id" class="trending-item" @click="handleProjectClick(repo.id)">
              <div class="trending-header">
                <a href="#" class="trending-name" @click.prevent>{{ repo.name }}</a>
                <span class="trending-stars">⭐ {{ repo.stars }}</span>
              </div>
              <p class="trending-description">{{ repo.description }}</p>
              <div class="trending-meta">
                <span class="trending-badge" :class="repo.trend">
                  <span v-if="repo.trend === 'hot'">🔥 热门</span>
                  <span v-else-if="repo.trend === 'rising'">📈 上升</span>
                  <span v-else>➡️ 稳定</span>
                </span>
              </div>
            </div>
          </div>
        </section>
      </div>
      
      <!-- Right Column: 消息通知栏 -->
      <div class="right-column">
        <section class="dashboard-section">
          <div class="section-header">
            <h2 class="section-title">消息通知</h2>
            <button v-if="displayedNotifications.length > 0" @click="clearAllMessages" class="clear-btn" title="清除所有消息">
              清除所有
            </button>
          </div>
          
          <div class="notification-list-container">
            <div 
              ref="notificationListRef"
              class="notification-list"
              @scroll="handleScroll"
            >
              <div v-if="displayedNotifications.length === 0" class="empty-state">
                <span class="empty-icon">📭</span>
                <p class="empty-text">暂无消息</p>
              </div>
              <div v-for="notice in displayedNotifications" :key="notice.id" 
                   class="notification-item" 
                   :class="{ unread: notice.unread }"
                   @click="handleNotificationClick(notice.id)">
                <div class="notification-icon" :style="{ backgroundColor: getNotificationColor(notice.type) }">
                  {{ notice.icon }}
                </div>
                <div class="notification-content">
                  <div class="notification-header">
                    <span class="notification-title">{{ notice.title }}</span>
                    <span class="notification-time">{{ notice.time }}</span>
                  </div>
                  <p class="notification-text">{{ notice.content }}</p>
                </div>
                <div 
                  v-if="notice.unread" 
                  class="unread-dot"
                  @click.stop="markAsRead(notice.id)"
                  title="标记为已读"
                ></div>
              </div>
              
              <div v-if="isLoading" class="loading-indicator">
                <span class="loading-spinner">⟳</span>
                <span>加载中...</span>
              </div>
              
              <div v-if="showAll && displayedNotifications.length > 0" class="no-more-hint">
                <span>没有更多消息了</span>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  </main>
</template>

<style scoped>
.app-main {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  min-height: calc(100vh - 200px);
  width: 100%;
  background-color: #f5f7fa;
}

.main-content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
  min-height: 100%;
  max-width: 100%;
}

.left-column,
.right-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.dashboard-section {
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 16px;
  box-shadow: 0 2px 4px rgba(0, 51, 102, 0.05);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  border-bottom: 2px solid #003366;
  padding-bottom: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #003366;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.clear-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px 12px;
  background-color: transparent;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  color: #999999;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.clear-btn:hover {
  background-color: #f5f5f5;
  color: #d93025;
  border-color: #d93025;
}


/* Repository List */

/* Trending Repositories */
.trending-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.trending-item {
  padding: 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  transition: all 0.2s;
  cursor: pointer;
}

.trending-item:hover {
  border-color: #0059b3;
  background-color: #f0f4ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 89, 179, 0.1);
}

.trending-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.trending-name {
  color: #0059b3;
  text-decoration: none;
  font-size: 14px;
  font-weight: 600;
}

.trending-name:hover {
  text-decoration: underline;
}

.trending-stars {
  font-size: 12px;
  color: #666666;
}

.trending-description {
  font-size: 14px;
  color: #666666;
  margin: 0 0 8px 0;
  line-height: 1.5;
}

.trending-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.trending-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background-color: rgba(0, 51, 102, 0.08);
  color: #003366;
}

.trending-badge.hot {
  background-color: rgba(217, 48, 37, 0.1);
  color: #d93025;
}

.trending-badge.rising {
  background-color: rgba(24, 128, 56, 0.1);
  color: #188038;
}


/* Notifications */
.notification-list-container {
  max-height: 500px;
  overflow-y: auto;
  position: relative;
  scroll-behavior: smooth;
}

.notification-list-container::-webkit-scrollbar {
  width: 6px;
}

.notification-list-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.notification-list-container::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.notification-list-container::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 100%;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  padding: 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  transition: all 0.2s;
  position: relative;
}

.notification-item:hover {
  border-color: #0059b3;
  background-color: #f0f4ff;
}

.notification-item.unread {
  border-color: #0059b3;
  background-color: rgba(0, 89, 179, 0.02);
}

.notification-item.unread:hover {
  border-color: #0059b3;
  background-color: rgba(0, 89, 179, 0.08);
}

.notification-icon {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 40px;
  height: 40px;
  min-width: 40px;
  border-radius: 50%;
  color: white;
  font-size: 20px;
}

.notification-content {
  flex: 1;
  margin-left: 12px;
  overflow: hidden;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.notification-title {
  font-size: 14px;
  font-weight: 600;
  color: #003366;
}

.notification-time {
  font-size: 12px;
  color: #999999;
  white-space: nowrap;
  margin-left: 8px;
}

.notification-text {
  font-size: 13px;
  color: #333333;
  line-height: 1.5;
  margin: 0;
  word-wrap: break-word;
}

.notification-count {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
}

.notification-count span {
  font-size: 12px;
  color: #003366;
  font-weight: 600;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background-color: #d93025;
  border-radius: 50%;
  position: absolute;
  top: 12px;
  right: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.unread-dot:hover {
  transform: scale(1.3);
  background-color: #c5221f;
}

.notification-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  background-color: #d93025;
  color: white;
  font-weight: 600;
}

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-text {
  font-size: 14px;
  color: #999999;
  margin: 0;
}

/* Loading State */
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 20px;
  color: #999999;
  font-size: 14px;
}

.loading-spinner {
  display: inline-block;
  animation: spin 1s linear infinite;
  font-size: 20px;
  color: #0059b3;
}



/* Loading Indicator */
.loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  color: #999999;
  font-size: 13px;
}

.loading-spinner {
  display: inline-block;
  animation: spin 1s linear infinite;
  font-size: 16px;
  color: #0059b3;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* No More Hint */
.no-more-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  color: #999999;
  font-size: 12px;
  text-align: center;
}

@media (max-width: 1200px) {
  .main-content {
    grid-template-columns: 2fr 1fr;
  }
}

@media (max-width: 768px) {
  .main-content {
    grid-template-columns: 1fr;
  }
  
  .app-main {
    padding: 16px;
  }
}
</style>