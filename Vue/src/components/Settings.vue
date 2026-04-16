<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import UserProfile from './UserProfile.vue'
import ProjectAnalytics from './ProjectAnalytics.vue'

const route = useRoute()
const router = useRouter()

// 当前激活的菜单项
const activeMenu = computed(() => {
  return route.query.tab || 'profile'
})

// 菜单项配置
// TODO: 添加更多设置选项（通知设置、隐私设置、账号安全等）
const menuItems = [
  { key: 'profile', label: '个人中心', icon: '👤' },
  { key: 'analytics', label: '项目数据分析', icon: '📊' }
]

// 切换菜单
const switchMenu = (key) => {
  router.push({ path: '/settings', query: { tab: key } })
}
</script>

<template>
  <div class="settings-container">
    <div class="settings-layout">
      <!-- 侧边栏 -->
      <aside class="settings-sidebar">
        <div class="sidebar-header">
          <h2>设置</h2>
        </div>
        <nav class="sidebar-nav">
          <a
            v-for="item in menuItems"
            :key="item.key"
            href="#"
            @click.prevent="switchMenu(item.key)"
            :class="['nav-item', { active: activeMenu === item.key }]"
          >
            <span class="nav-icon">{{ item.icon }}</span>
            <span class="nav-label">{{ item.label }}</span>
          </a>
        </nav>
      </aside>

      <!-- 主内容区 -->
      <main class="settings-content">
        <UserProfile v-if="activeMenu === 'profile'" />
        <ProjectAnalytics v-if="activeMenu === 'analytics'" />
      </main>
    </div>
  </div>
</template>

<style scoped>
.settings-container {
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
  width: 100%;
}

.settings-layout {
  display: flex;
  width: 100%;
  min-height: calc(100vh - 60px);
}

/* 侧边栏样式 */
.settings-sidebar {
  width: 240px;
  background: linear-gradient(135deg, #003366 100%, #004080 0%);
  border-right: none;
  padding: 24px 0;
  top: 0;
  overflow-y: auto;
  box-shadow: 2px 0 8px rgba(0, 51, 102, 0.1);
}

.sidebar-header {
  padding: 0 20px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.sidebar-header h2 {
  margin: 0;
  font-size: 20px;
  color: #ffffff;
  font-weight: 600;
}

.sidebar-nav {
  padding: 12px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  color: rgba(255, 255, 255, 0.85);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s;
  border-left: 3px solid transparent;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #ffffff;
}

.nav-item.active {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
  border-left-color: #ffffff;
  font-weight: 500;
}

.nav-icon {
  font-size: 18px;
  display: inline-block;
}

.nav-label {
  flex: 1;
}

/* 主内容区 */
.settings-content {
  flex: 1;
  padding: 0;
  overflow-y: auto;
  background: #ffffff;
}

@media (max-width: 768px) {
  .settings-layout {
    flex-direction: column;
  }

  .settings-sidebar {
    width: 100%;
    height: auto;
    position: static;
    border-right: none;
    border-bottom: none;
  }

  .sidebar-nav {
    display: flex;
    overflow-x: auto;
    padding: 8px;
  }

  .nav-item {
    white-space: nowrap;
    border-left: none;
    border-bottom: 3px solid transparent;
  }

  .nav-item.active {
    border-left: none;
    border-bottom-color: #ffffff;
  }

  .settings-content {
    padding: 0;
  }
}
</style>
