<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyProjects } from '@/api/project'
import { toast } from '@/utils/toast'
import { log, error as logError, warn } from '@/utils/logger'
import tokenManager from '@/utils/tokenManager'

const router = useRouter()

// 当前用户信息
const userInfo = ref({
  id: 1,
  username: 'Admin',
  avatar: '/bjut-logo.svg'
})

// 仓库标签页（固定为"我创建的"）
const activeTab = ref('created')

// 每页显示的项目数量
const PAGE_SIZE = 6
const currentPageNum = ref(1)

// 项目列表（只存储当前用户创建的项目）
const allProjects = ref([])
const isLoading = ref(false)

// 计算总页数
const totalPages = computed(() => Math.ceil(allProjects.value.length / PAGE_SIZE))

// 计算当前页的项目列表
const projects = computed(() => {
  const start = (currentPageNum.value - 1) * PAGE_SIZE
  const end = start + PAGE_SIZE
  return allProjects.value.slice(start, end)
})

// 切换页码
const changePage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPageNum.value = page
    window.scrollTo({ top: 0, behavior: 'smooth' })
    // 重新加载当前页的数据
    loadUserProjects()
  }
}

// 格式化数字
const formatNumber = (num) => {
  if (num === undefined || num === null) {
    return '0'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

// 打开项目详情
const handleProjectClick = (projectId) => {
  router.push(`/project/${projectId}`)
}

// 加载用户创建的项目
const loadUserProjects = async () => {
  isLoading.value = true
  try {
    // 从 tokenManager 获取用户信息
    const userInfoFromToken = tokenManager.getUserInfo()
    if (userInfoFromToken) {
      userInfo.value = userInfoFromToken
      log('从 tokenManager 获取用户信息成功:', userInfo.value.username)
    } else {
      warn('未找到用户信息，可能未登录')
      toast.warning('请先登录')
      return
    }

    log('加载用户项目，用户 ID:', userInfo.value.id)

    // 调用真实 API 获取用户创建的项目
    const res = await getMyProjects({
      pageNum: currentPageNum.value,
      pageSize: PAGE_SIZE
    })
    
    if (res.code === 200 && res.data) {
      allProjects.value = res.data
      log('加载完成，项目数量:', allProjects.value.length)
    } else {
      warn('API 返回数据异常:', res)
      allProjects.value = []
    }
  } catch (error) {
    logError('加载项目失败:', error)
    allProjects.value = []
    toast.error(error.message || '加载项目失败，请稍后重试')
  } finally {
    isLoading.value = false
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadUserProjects()
})
</script>

<template>
  <main class="app-main">
    <div class="user-repository-container">
      <!-- 页面头部 -->
      <div class="page-header">
        <div class="user-info-header">
          <img :src="userInfo.avatar" alt="User avatar" class="user-avatar" />
          <div class="user-details">
            <h1 class="page-title">{{ userInfo.username }} 的仓库</h1>
            <p class="page-description">管理你的项目和代码仓库</p>
          </div>
          <button class="create-project-btn" @click="$router.push('/create-project')">
            ➕ 新增项目
          </button>
        </div>
      </div>

      <!-- 项目列表 -->
      <div v-if="isLoading" class="loading-state">
        <span class="loading-spinner">⟳</span>
        <p>加载中...</p>
      </div>

      <div v-else-if="projects.length > 0" class="project-grid">
        <div 
          v-for="project in projects" 
          :key="project.id" 
          class="project-card"
          @click="handleProjectClick(project.id)"
        >
          <div class="project-card-header">
            <span class="project-name">{{ project.name }}</span>
          </div>
          <p class="project-description">{{ project.description }}</p>
          <div class="project-tags">
            <span v-for="tag in project.tags" :key="tag.id" class="tech-tag">
              {{ tag.name }}
            </span>
          </div>
          <div class="project-footer">
            <div class="project-stats">
              <span class="stat">
                ❤️ {{ formatNumber(project.likes) }}
              </span>
              <span class="stat">
                ⭐ {{ formatNumber(project.favorites) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <span class="empty-icon">📭</span>
        <p class="empty-text">暂无项目</p>
        <!-- TODO: 添加创建项目的路由 -->
        <button class="create-project-btn" @click="$router.push('/create-project')">
          创建新项目
        </button>
      </div>

      <!-- 分页控件 -->
      <div v-if="allProjects.length > 0" class="pagination-container">
        <div class="pagination">
          <button 
            class="page-btn"
            @click="changePage(currentPageNum - 1)"
            :disabled="currentPageNum === 1"
          >
            ‹ 上一页
          </button>
          
          <button 
            v-for="page in totalPages" 
            :key="page"
            :class="['page-btn', { active: page === currentPageNum }]"
            @click="changePage(page)"
          >
            {{ page }}
          </button>
          
          <button 
            class="page-btn"
            @click="changePage(currentPageNum + 1)"
            :disabled="currentPageNum === totalPages"
          >
            下一页 ›
          </button>
        </div>
        
        <div class="page-info">
          共 {{ allProjects.length }} 个项目，第 {{ currentPageNum }} / {{ totalPages }} 页
        </div>
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

.user-repository-container {
  max-width: 1200px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  margin-bottom: 24px;
}

.user-info-header {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.user-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.user-details {
  flex: 1;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #003366;
  margin: 0 0 8px 0;
}

.page-description {
  font-size: 16px;
  color: #666666;
  margin: 0;
}

/* 标签页 */
.tabs-container {
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 24px;
  box-shadow: 0 2px 4px rgba(0, 51, 102, 0.05);
}

.tabs {
  display: flex;
  gap: 8px;
}

.tab-btn {
  padding: 8px 16px;
  background-color: transparent;
  border: none;
  border-radius: 4px;
  color: #666666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab-btn:hover {
  background-color: #f5f5f5;
}

.tab-btn.active {
  background-color: #0059b3;
  color: #ffffff;
}

.tab-count {
  font-size: 12px;
  opacity: 0.8;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
}

.loading-spinner {
  font-size: 48px;
  color: #0059b3;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 项目网格 */
.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

/* 项目卡片 */
.project-card {
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 4px rgba(0, 51, 102, 0.05);
}

.project-card:hover {
  border-color: #0059b3;
  box-shadow: 0 4px 12px rgba(0, 51, 102, 0.15);
  transform: translateY(-2px);
}

.project-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.project-icon {
  font-size: 24px;
}

.project-name {
  font-size: 16px;
  font-weight: 600;
  color: #0059b3;
}

.project-description {
  font-size: 14px;
  color: #666666;
  line-height: 1.6;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.project-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.tech-tag {
  padding: 4px 10px;
  background-color: rgba(0, 89, 179, 0.08);
  border-radius: 4px;
  font-size: 12px;
  color: #0059b3;
  font-weight: 500;
}

.project-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.project-stats {
  display: flex;
  gap: 12px;
}

.stat {
  font-size: 13px;
  color: #666666;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  margin-bottom: 24px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-text {
  font-size: 16px;
  color: #999999;
  margin-bottom: 24px;
}

.create-project-btn {
  padding: 10px 24px;
  background-color: #0059b3;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.create-project-btn:hover {
  background-color: #004080;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 89, 179, 0.3);
}

/* 分页控件 */
.pagination-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding-top: 24px;
  border-top: 1px solid #e0e0e0;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-btn {
  min-width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  color: #333333;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  background-color: #0059b3;
  border-color: #0059b3;
  color: #ffffff;
}

.page-btn.active {
  background-color: #0059b3;
  border-color: #0059b3;
  color: #ffffff;
  font-weight: 600;
}

.page-btn:disabled {
  background-color: #f5f5f5;
  color: #cccccc;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: #666666;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-info-header {
    flex-direction: column;
    text-align: center;
  }

  .tabs {
    flex-wrap: wrap;
  }

  .project-grid {
    grid-template-columns: 1fr;
  }
}
</style>
