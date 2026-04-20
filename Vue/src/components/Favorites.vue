<template>
  <div class="favorites-page">
    <div class="container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h1>⭐ 我的收藏</h1>
        <p class="subtitle">我关注的项目列表</p>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="projects.length === 0" class="empty-state">
        <div class="empty-icon">⭐</div>
        <h3>还没有收藏任何项目</h3>
        <p>去项目广场发现有趣的项目吧！</p>
        <router-link to="/projects" class="btn-primary">浏览项目广场</router-link>
      </div>

      <!-- 项目列表 -->
      <div v-else>
        <div class="project-grid">
          <div 
            v-for="project in projects" 
            :key="project.id"
            class="project-card"
            @click="goToProject(project.id)"
          >
            <div class="project-card-header">
              <span class="project-name">{{ project.name }}</span>
            </div>
            <p class="project-description">{{ project.description || '暂无描述' }}</p>
            <div class="project-tags">
              <span v-for="tag in (project.tags || [])" :key="tag.id" class="tech-tag">
                {{ tag.name }}
              </span>
            </div>
            <div class="project-footer">
              <div class="project-stats">
                <span class="stat">
                  ❤️ {{ formatNumber(project.starCount || 0) }}
                </span>
                <span class="stat">
                  ⭐ {{ formatNumber(project.watchCount || 0) }}
                </span>
              </div>
            </div>
          </div>
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
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyWatchedProjects } from '@/api/project'
import { error as logError } from '@/utils/logger'

const router = useRouter()
const loading = ref(false)

// 每页显示的项目数量
const PAGE_SIZE = 6
const currentPageNum = ref(1)

// 所有收藏的项目
const allProjects = ref([])

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
  }
}

// 加载收藏的项目
const loadFavorites = async () => {
  loading.value = true
  try {
    const res = await getMyWatchedProjects()
    if (res.code === 200) {
      // 后端已按更新时间降序排列，直接使用
      allProjects.value = res.data || []
      currentPageNum.value = 1 // 重置到第一页
    }
  } catch (error) {
    logError('加载收藏列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 跳转到项目详情
const goToProject = (projectId) => {
  router.push(`/project/${projectId}`)
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  
  const days = Math.floor(diff / 86400000)
  if (days < 1) return '今天'
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  if (days < 365) return `${Math.floor(days / 30)}个月前`
  return `${Math.floor(days / 365)}年前`
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

onMounted(() => {
  loadFavorites()
})
</script>

<style scoped>
.favorites-page {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  min-height: calc(100vh - 200px);
  width: 100%;
  background-color: #f5f7fa;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 28px;
  font-weight: 600;
  color: #003366;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 16px;
  color: #666666;
  margin: 0;
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

.spinner {
  font-size: 48px;
  color: #0059b3;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
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
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state h3 {
  font-size: 16px;
  color: #999999;
  margin-bottom: 24px;
}

.empty-state p {
  font-size: 14px;
  color: #999999;
  margin-bottom: 24px;
}

.btn-primary {
  padding: 10px 24px;
  background-color: #0059b3;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
  white-space: nowrap;
}

.btn-primary:hover {
  background-color: #004080;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 89, 179, 0.3);
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

.page-btn:disabled {
  background-color: #f5f5f5;
  color: #cccccc;
  cursor: not-allowed;
}

.page-btn.active {
  background-color: #0059b3;
  border-color: #0059b3;
  color: #ffffff;
}

.page-info {
  font-size: 13px;
  color: #666666;
}

@media (max-width: 768px) {
  .page-header h1 {
    font-size: 24px;
  }
  
  .project-grid {
    grid-template-columns: 1fr;
  }
}
</style>
