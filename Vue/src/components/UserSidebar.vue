<script setup>
import { ref, onMounted } from 'vue'
import tokenManager from '@/utils/tokenManager'

// 用户信息
const userInfo = ref({
  username: '',
  avatar: '',
  role: ''
})

// 获取角色显示文本
const getRoleText = (role) => {
  const roleMap = {
    'USER': '普通用户',
    'TEACHER': '教师',
    'ADMIN': '管理员'
  }
  return roleMap[role] || '未知'
}

// 组件挂载时加载用户信息
onMounted(() => {
  const userInfoFromToken = tokenManager.getUserInfo()
  console.log('UserSidebar - 从 localStorage 获取的用户信息:', userInfoFromToken)
  if (userInfoFromToken) {
    userInfo.value = userInfoFromToken
    console.log('UserSidebar - 当前用户角色:', userInfo.value.role)
    console.log('UserSidebar - 是否为教师:', userInfo.value.role === 'TEACHER')
  } else {
    console.warn('UserSidebar - 未找到用户信息，请先登录')
  }
})
</script>

<template>
  <aside class="profile-sidebar">
    <div class="sidebar-header">
      <img
        :src="userInfo.avatar || '/logo.svg'"
        alt="头像"
        class="sidebar-avatar"
      />
      <h3 class="sidebar-username">{{ userInfo.username }}</h3>
      <p class="sidebar-role">{{ getRoleText(userInfo.role) }}</p>
    </div>
    
    <nav class="sidebar-nav">
      <router-link to="/profile" class="nav-item" active-class="active">
        <span class="nav-icon">👤</span>
        <span class="nav-label">个人信息</span>
      </router-link>
      <router-link to="/repository" class="nav-item" active-class="active">
        <span class="nav-icon">📁</span>
        <span class="nav-label">我的仓库</span>
      </router-link>
      <router-link to="/favorites" class="nav-item" active-class="active">
        <span class="nav-icon">⭐</span>
        <span class="nav-label">我的收藏</span>
      </router-link>
      <router-link 
        v-if="userInfo.role === 'TEACHER'" 
        to="/class-management" 
        class="nav-item" 
        active-class="active"
      >
        <span class="nav-icon">📚</span>
        <span class="nav-label">教学管理</span>
      </router-link>
    </nav>
  </aside>
</template>

<style scoped>
.profile-sidebar {
  width: 240px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 24px 20px;
  text-align: center;
  background: linear-gradient(135deg, #064e3b 0%, #047857 100%);
  color: #ffffff;
}

.sidebar-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 3px solid #ffffff;
  margin-bottom: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.sidebar-username {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px 0;
}

.sidebar-role {
  font-size: 13px;
  opacity: 0.9;
  margin: 0;
}

.sidebar-nav {
  padding: 12px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  color: #333333;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  border-left: 3px solid transparent;
}

.nav-item:hover {
  background-color: #f5f5f5;
  color: #064e3b;
}

.nav-item.active {
  background-color: #ecfdf5;
  color: #064e3b;
  border-left-color: #064e3b;
  font-weight: 600;
}

.nav-icon {
  font-size: 18px;
  width: 24px;
  text-align: center;
}

.nav-label {
  flex: 1;
}

@media (max-width: 768px) {
  .profile-sidebar {
    width: 100%;
  }

  .sidebar-header {
    padding: 20px;
  }

  .sidebar-avatar {
    width: 60px;
    height: 60px;
  }
}
</style>
