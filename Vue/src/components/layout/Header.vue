<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { logout as logoutApi } from '@/api/auth'
import { toast } from '@/utils/toast'
import { error as logError, log } from '@/utils/logger'
import tokenManager from '@/utils/tokenManager'


const router = useRouter()
const route = useRoute()

// 用户信息
const userInfo = ref({
  username: '',
  avatar: '',
  role: ''
})

// 搜索框
const searchQuery = ref('')

// 退出登录确认对话框
const showLogoutModal = ref(false)

// 导航菜单项
const menuItems = [
  { icon: '🏠', label: '主页', path: '/home' },
  { icon: '📁', label: '项目广场', path: '/projects' },
  { icon: '👥', label: '组队广场', path: '/team' },
  { icon: '🤖', label: 'AI 助手', path: '/ai' },
]

// 计算导航项的激活状态
const isActive = (path) => {
  return route.path === path
}

// 处理退出登录
const handleLogout = () => {
  // 显示自定义确认对话框
  showLogoutModal.value = true
}

// 确认退出登录
const confirmLogout = async () => {
  try {
    await logoutApi()
  } catch (error) {
    logError('退出登录失败:', error)
  } finally {
    // 关闭对话框
    showLogoutModal.value = false
    // 使用 tokenManager 清除认证信息
    tokenManager.removeToken()
    // 跳转到登录页
    await router.push('/login')
  }
}

// 取消退出登录
const cancelLogout = () => {
  showLogoutModal.value = false
}

// 处理搜索
const handleSearch = () => {
  if (searchQuery.value.trim()) {
    // 跳转到搜索结果页，通过 query 参数传递关键词
    router.push({
      path: '/search',
      query: { keyword: searchQuery.value.trim() }
    })
    // 清空搜索框
    searchQuery.value = ''
  }
}

// 处理导航点击
const handleNavigation = (path) => {
  router.push(path)
}

// 处理头像点击
const handleAvatarClick = () => {
  router.push('/profile')
}

// 跳转到管理后台
const goToAdmin = () => {
  router.push('/admin')
}

// 组件挂载时加载用户信息
onMounted(() => {
  // 使用 tokenManager 获取用户信息
  const userInfoFromToken = tokenManager.getUserInfo()
  if (userInfoFromToken) {
    userInfo.value = userInfoFromToken
  }
})
</script>

<template>
  <header class="app-header">
    <div class="header-container">
      <div class="header-left">
        <img src="/logo.svg" alt="logo" class="app-logo" />
        <div class="site-info">
          <h1 class="site-name">ProjecTree知享圈</h1>
          <p class="site-slogan">Plant your ideas, grow your future.</p>
        </div>
        <!-- 导航菜单 -->
        <nav class="header-nav">
          <a 
            v-for="item in menuItems" 
            :key="item.label" 
            href="#" 
            @click.prevent="handleNavigation(item.path)" 
            :class="['nav-item', { active: isActive(item.path) }]"
          >
            {{ item.icon }} {{ item.label }}
            <span v-if="item.count" class="nav-count">{{ item.count }}</span>
          </a>
        </nav>
      </div>
      <div class="header-right">
        <div class="search-box">
          <svg class="search-icon" viewBox="0 0 16 16" width="16" height="16">
            <path
              fill-rule="evenodd"
              d="M11.5 7a4.499 4.499 0 11-8.998 0A4.499 4.499 0 0111.5 7zm-.82 4.74a6 6 0 111.06-1.06l3.04 3.04a.75.75 0 11-1.06 1.06l-3.04-3.04z"
            />
          </svg>
          <input
            v-model="searchQuery"
            type="text"
            placeholder="搜索项目、团队..."
            class="search-input"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="user-info">
          <!-- 管理员入口 -->
          <button 
            v-if="userInfo.role === 'ADMIN'"
            @click="goToAdmin" 
            class="admin-btn" 
            title="管理后台"
          >
            🛡️ 管理
          </button>
          <img
            :src="userInfo.avatar || '/logo.svg'"
            alt="User avatar"
            class="user-avatar"
            @click="handleAvatarClick"
            title="点击查看个人中心"
          />
          <span 
            class="username" 
            @click="handleAvatarClick"
            style="cursor: pointer;"
            title="点击查看个人中心"
          >{{ userInfo.username || '用户' }}</span>
          <button @click="handleLogout" class="logout-btn" title="退出登录">
            退出登录
          </button>
        </div>
      </div>
    </div>

    <!-- 退出登录确认对话框 -->
    <div v-if="showLogoutModal" class="modal-overlay" @click.self="cancelLogout">
      <div class="modal-content">
        <div class="modal-header">
          <h2 class="modal-title">确认退出</h2>
          <button @click="cancelLogout" class="close-btn" title="关闭">&times;</button>
        </div>
        <div class="modal-body">
          <div class="modal-icon">
            <svg viewBox="0 0 64 64" width="64" height="64">
              <circle cx="32" cy="32" r="30" fill="#d1fae5" stroke="#10b981" stroke-width="2"/>
              <path d="M32 18v18M32 42v2" stroke="#10b981" stroke-width="3" stroke-linecap="round"/>
            </svg>
          </div>
          <p class="modal-message">确定要退出登录吗？</p>
        </div>
        <div class="modal-footer">
          <button @click="cancelLogout" class="modal-btn cancel">
            取消
          </button>
          <button @click="confirmLogout" class="modal-btn confirm">
            确认退出
          </button>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  background: linear-gradient(135deg, #064e3b 0%, #065f46 50%, #047857 100%);
  padding: 12px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  border-bottom: 2px solid #10b981;
  width: 100%;
  box-shadow: 0 2px 12px rgba(6, 78, 59, 0.3);
}

.header-container {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.app-logo {
  height: 32px;
  width: auto;
  cursor: pointer;
  transition: opacity 0.2s;
}

.app-logo:hover {
  opacity: 0.9;
}

.site-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.site-name {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 0.5px;
}

.site-slogan {
  margin: 0;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
  font-style: italic;
  letter-spacing: 0.3px;
}

.header-nav {
  display: flex;
  gap: 4px;
}

.nav-item {
  color: #ffffff;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: 6px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-item:hover {
  background-color: rgba(16, 185, 129, 0.2);
}

.nav-icon {
  display: inline-block;
}

.nav-count {
  background-color: rgba(245, 158, 11, 0.9);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  color: #ffffff;
  margin-left: 4px;
  font-weight: 600;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 8px;
  fill: #a7f3d0;
  pointer-events: none;
}

.search-input {
  background-color: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(16, 185, 129, 0.4);
  border-radius: 6px;
  padding: 6px 12px 6px 32px;
  color: #ffffff;
  font-size: 14px;
  width: 300px;
  outline: none;
  transition: all 0.2s;
}

.search-input::placeholder {
  color: #a7f3d0;
}

.search-input:focus {
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.25);
}

.header-icon-link svg {
  fill: #ffffff;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid #10b981;
  cursor: pointer;
  transition: all 0.2s;
}

.user-avatar:hover {
  border-color: #f59e0b;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.3);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.username {
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px;
  color: #ffffff;
  background-color: rgba(245, 158, 11, 0.2);
  border: 1px solid rgba(245, 158, 11, 0.4);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn svg {
  fill: #fbbf24;
}

.logout-btn:hover {
  background-color: rgba(245, 158, 11, 0.35);
  border-color: #f59e0b;
}

/* 管理员按钮 */
.admin-btn {
  padding: 6px 12px;
  color: #ffffff;
  background-color: rgba(239, 68, 68, 0.2);
  border: 1px solid rgba(239, 68, 68, 0.4);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  font-weight: 500;
}

.admin-btn:hover {
  background-color: rgba(239, 68, 68, 0.35);
  border-color: #ef4444;
}

/* 退出登录确认对话框 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-content {
  background: #ffffff;
  border-radius: 16px;
  width: 90%;
  max-width: 420px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.25);
  animation: slideUp 0.3s ease-out;
  overflow: hidden;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
}

.modal-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #065f46;
}

.close-btn {
  background: none;
  border: none;
  font-size: 32px;
  color: #065f46;
  cursor: pointer;
  line-height: 1;
  padding: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: rgba(6, 95, 70, 0.1);
  transform: rotate(90deg);
}

.modal-body {
  padding: 32px 24px;
  text-align: center;
}

.modal-icon {
  margin-bottom: 16px;
  animation: bounce 0.6s ease-in-out;
}

@keyframes bounce {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

.modal-message {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 500;
  color: #1f2937;
}

.modal-hint {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
}

.modal-footer {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  background: #f9fafb;
  border-top: 1px solid #f0f0f0;
}

.modal-btn {
  flex: 1;
  padding: 12px 20px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.modal-btn.cancel {
  background: #ffffff;
  color: #6b7280;
  border: 2px solid #e5e7eb;
}

.modal-btn.cancel:hover {
  background: #f9fafb;
  border-color: #d1d5db;
  color: #374151;
}

.modal-btn.confirm {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

.modal-btn.confirm:hover {
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
  transform: translateY(-1px);
}

.modal-btn.confirm:active {
  transform: translateY(0);
}

@media (max-width: 768px) {
  .header-nav {
    display: none;
  }

  .search-input {
    width: 200px;
  }
}
</style>
