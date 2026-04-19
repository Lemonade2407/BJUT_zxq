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
  avatar: ''
})

// 搜索框
const searchQuery = ref('')

// 导航菜单项
const menuItems = [
  { icon: '🏠', label: '主页', path: '/home' },
  { icon: '📁', label: '我的仓库', path: '/repository'},
  { icon: '📦', label: '项目广场', path: '/projects' },
  { icon: '⭐', label: '收藏', path: '/favorites' },
  // { icon: '👥', label: '组队广场', path: '/team' },  // 暂时注释，后续开发
  { icon: '⚙️', label: '设置', path: '/settings' },
]

// 计算导航项的激活状态
const isActive = (path) => {
  return route.path === path
}

// 处理退出登录
const handleLogout = async () => {
  // TODO: 添加退出确认对话框美化（使用自定义 Modal）
  if (confirm('确定要退出登录吗？')) {
    try {
      await logoutApi()
    } catch (error) {
      logError('退出登录失败:', error)
    } finally {
      // 使用 tokenManager 清除认证信息
      tokenManager.removeToken()
      // 跳转到登录页
      await router.push('/login')
    }
  }
}

// 处理搜索
const handleSearch = () => {
  if (searchQuery.value.trim()) {
    log('搜索:', searchQuery.value)
    // TODO: 实现真实的搜索功能，跳转到搜索结果页
    // TODO: 添加搜索历史记录
    // TODO: 添加热门搜索推荐
    toast.info(`搜索功能开发中... 搜索内容: ${searchQuery.value}`)
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
        <img src="/bjut-logo.svg" alt="北京工业大学校徽" class="app-logo" />
        <!-- 导航菜单 -->
        <nav class="header-nav">
          <a 
            v-for="item in menuItems" 
            :key="item.label" 
            href="#" 
            @click.prevent="handleNavigation(item.path)" 
            :class="['nav-item', { active: isActive(item.path) }]"
          >
            <span class="nav-emoji">{{ item.icon }}</span>
            {{ item.label }}
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
          <img
            :src="userInfo.avatar || '/bjut-logo.svg'"
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

    <!-- 退出确认对话框 -->
    <div v-if="showLogoutModal" class="modal-overlay" @click.self="cancelLogout">
      <div class="modal-content">
        <div class="modal-header">
          <h3>确认退出</h3>
          <button @click="cancelLogout" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <div class="modal-icon">👋</div>
          <p class="modal-message">确定要退出登录吗？</p>
          <p class="modal-hint">退出后需要重新登录才能访问</p>
        </div>
        <div class="modal-footer">
          <button @click="cancelLogout" class="modal-btn cancel-btn">取消</button>
          <button @click="confirmLogout" class="modal-btn confirm-btn">确认退出</button>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  background: linear-gradient(135deg, #003366 0%, #004080 100%);
  padding: 12px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  border-bottom: 2px solid #0059b3;
  width: 100%;
  box-shadow: 0 2px 8px rgba(0, 51, 102, 0.2);
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
  background-color: rgba(255, 255, 255, 0.15);
}

.nav-emoji {
  font-size: 16px;
  display: inline-block;
}

.nav-count {
  background-color: rgba(255, 255, 255, 0.3);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  color: #ffffff;
  margin-left: 4px;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 8px;
  fill: #a8c0e0;
  pointer-events: none;
}

.search-input {
  background-color: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 6px;
  padding: 6px 12px 6px 32px;
  color: #ffffff;
  font-size: 14px;
  width: 300px;
  outline: none;
  transition: all 0.2s;
}

.search-input::placeholder {
  color: #a8c0e0;
}

.search-input:focus {
  border-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.2);
}

.header-icon-link svg {
  fill: #ffffff;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid #ffffff;
  cursor: pointer;
  transition: border-color 0.2s;
}

.user-avatar:hover {
  border-color: #a8c0e0;
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
  background-color: rgba(255, 255, 255, 0.15);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn svg {
  fill: #ffffff;
}

.logout-btn:hover {
  background-color: rgba(255, 255, 255, 0.3);
}

/* 退出确认对话框 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 51, 102, 0.3);
  max-width: 400px;
  width: 90%;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e8e8e8;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #003366;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #999999;
  cursor: pointer;
  line-height: 1;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.close-btn:hover {
  background-color: #f5f5f5;
  color: #333333;
}

.modal-body {
  padding: 32px 24px;
  text-align: center;
}

.modal-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.modal-message {
  font-size: 16px;
  color: #333333;
  margin: 0 0 8px 0;
  font-weight: 500;
}

.modal-hint {
  font-size: 14px;
  color: #999999;
  margin: 0;
}

.modal-footer {
  display: flex;
  gap: 12px;
  padding: 16px 24px 24px;
  justify-content: flex-end;
}

.modal-btn {
  padding: 10px 24px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 100px;
}

.cancel-btn {
  background-color: #f5f5f5;
  color: #666666;
}

.cancel-btn:hover {
  background-color: #e8e8e8;
  color: #333333;
}

.confirm-btn {
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
  color: #ffffff;
  box-shadow: 0 2px 4px rgba(255, 77, 79, 0.2);
}

.confirm-btn:hover {
  background: linear-gradient(135deg, #ff7875 0%, #ffa39e 100%);
  box-shadow: 0 4px 8px rgba(255, 77, 79, 0.3);
  transform: translateY(-1px);
}

.confirm-btn:active {
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
