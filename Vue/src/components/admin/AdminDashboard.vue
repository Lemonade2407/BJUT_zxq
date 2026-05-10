<script setup>
import { ref } from 'vue'
import StatsOverview from './StatsOverview.vue'
import UserManagement from './UserManagement.vue'
import ProjectManagement from './ProjectManagement.vue'
import TagManagement from './TagManagement.vue'
import CourseManagement from './CourseManagement.vue'
import CommentManagement from './CommentManagement.vue'
import TeamManagement from './TeamManagement.vue'
import { useRouter } from 'vue-router'
import { toast } from '@/utils/toast'

const router = useRouter()

// 当前激活的菜单项
const activeMenu = ref('stats')

// 菜单项配置
const menuItems = [
  { key: 'stats', label: '数据概览', icon: '📊' },
  { key: 'users', label: '用户管理', icon: '👥' },
  { key: 'projects', label: '项目管理', icon: '📁' },
  { key: 'tags', label: '标签管理', icon: '🏷️' },
  { key: 'courses', label: '课程管理', icon: '📚' },
  { key: 'comments', label: '评论管理', icon: '💬' },
  { key: 'teams', label: '组队管理', icon: '👥' }
]

// 切换菜单
const switchMenu = (key) => {
  activeMenu.value = key
}
</script>

<template>
  <div class="admin-container">
    <!-- 侧边栏 -->
    <aside class="admin-sidebar">
      <div class="sidebar-header">
        <h2 class="sidebar-title">🛡️ 管理后台</h2>
      </div>
      
      <nav class="sidebar-nav">
        <button
          v-for="item in menuItems"
          :key="item.key"
          @click="switchMenu(item.key)"
          :class="['menu-item', { active: activeMenu === item.key }]"
        >
          <span class="menu-icon">{{ item.icon }}</span>
          <span class="menu-label">{{ item.label }}</span>
        </button>
      </nav>
    </aside>

    <!-- 主内容区 -->
    <main class="admin-main">
      <KeepAlive>
        <StatsOverview v-if="activeMenu === 'stats'" />
      </KeepAlive>
      <UserManagement v-if="activeMenu === 'users'" />
      
      <!-- 项目管理 -->
      <ProjectManagement v-else-if="activeMenu === 'projects'" />
      
      <!-- 标签管理 -->
      <TagManagement v-else-if="activeMenu === 'tags'" />
      
      <!-- 课程管理 -->
      <CourseManagement v-else-if="activeMenu === 'courses'" />
      
      <!-- 评论管理 -->
      <CommentManagement v-else-if="activeMenu === 'comments'" />

      <!-- 组队管理 -->
      <TeamManagement v-else-if="activeMenu === 'teams'" />
    </main>
  </div>
</template>

<style scoped>
.admin-container {
  display: flex;
  width: 100%;
  min-height: calc(100vh - 80px);
  background-color: #f5f7fa;
  padding: 32px 0;
  gap: 32px;
}

/* 侧边栏 */
.admin-sidebar {
  width: 240px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 24px 20px;
  text-align: center;
  background: linear-gradient(135deg, #064e3b 0%, #047857 100%);
  color: #ffffff;
}

.sidebar-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  overflow-y: auto;
}

.menu-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 8px;
  border: none;
  border-radius: 8px;
  background-color: transparent;
  color: #666666;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
}

.menu-item:hover {
  background-color: #f5f5f5;
  color: #064e3b;
}

.menu-item.active {
  background-color: #ecfdf5;
  color: #064e3b;
  border-left-color: #064e3b;
  font-weight: 600;
}

.menu-icon {
  font-size: 20px;
}

.menu-label {
  flex: 1;
}

/* 主内容区 */
.admin-main {
  flex: 1;
  padding: 0 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .admin-container {
    flex-direction: column;
    padding: 20px 0;
    gap: 20px;
  }
  
  .admin-sidebar {
    width: 100%;
    border-radius: 0;
  }
  
  .admin-main {
    padding: 0 20px;
  }
}
</style>
