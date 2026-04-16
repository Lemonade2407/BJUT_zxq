<script setup>
import { ref, computed, onMounted } from 'vue'
import { getProjectList } from '@/api/project'
import { toast } from '@/utils/toast'
import { log, error as logError } from '@/utils/logger'

// TODO: 添加搜索防抖功能（避免频繁请求）
// TODO: 添加筛选条件持久化（记住用户选择）

// 每页显示的项目数量
const PAGE_SIZE = 5
const currentPageNum = ref(1)

// 项目列表（从后端 API 获取）
const allProjects = ref([])
const isLoading = ref(false)

// 获取项目列表
const fetchProjects = async () => {
  isLoading.value = true
  try {
    const response = await getProjectList({
      pageNum: 1,
      pageSize: 100 // 获取更多项目用于前端分页
    })
    
    if (response.code === 200 && response.data) {
      allProjects.value = response.data.map(project => ({
        id: project.id,
        name: project.name,
        description: project.description || '暂无描述',
        tags: project.tags ? project.tags.map(tag => tag.name) : [],
        avatar: '👤',
        author: project.author || '未知用户',
        updatedAt: formatDate(project.updatedAt),
        likes: project.likes || project.starCount || 0,
        favorites: project.favorites || project.watchCount || 0,
        isLiked: project.isLiked || false,
        isFavorited: project.isFavorited || false
      }))
      log('项目列表加载成功，数量：', allProjects.value.length)
    } else {
      logError('获取项目列表失败：', response.message)
      toast.error(response.message || '获取项目列表失败')
    }
  } catch (error) {
    logError('获取项目列表异常：', error)
    toast.error('网络错误，请稍后重试')
  } finally {
    isLoading.value = false
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '未知时间'
  const date = new Date(dateString)
  const now = new Date()
  const diff = now - date
  
  // 小于 1 分钟
  if (diff < 60000) {
    return '刚刚'
  }
  // 小于 1 小时
  if (diff < 3600000) {
    return Math.floor(diff / 60000) + '分钟前'
  }
  // 小于 1 天
  if (diff < 86400000) {
    return Math.floor(diff / 3600000) + '小时前'
  }
  // 小于 7 天
  if (diff < 604800000) {
    return Math.floor(diff / 86400000) + '天前'
  }
  
  // 超过 7 天，显示具体日期
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 组件挂载时获取数据
onMounted(() => {
  fetchProjects()
})

// TODO: 实现真实的筛选和搜索功能
const formatNumber = (num) => {
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

// 计算总页数
const totalPages = computed(() => Math.ceil(allProjects.value.length / PAGE_SIZE))

// 计算当前页的项目列表
const projects = computed(() => {
  const start = (currentPageNum.value - 1) * PAGE_SIZE
  const end = start + PAGE_SIZE
  return allProjects.value.slice(start, end)
})

// 点赞功能
const handleLikeProject = (project) => {
  project.isLiked = !project.isLiked
  project.likes += project.isLiked ? 1 : -1
}

// 收藏功能
const handleFavoriteProject = (project) => {
  project.isFavorited = !project.isFavorited
  project.favorites += project.isFavorited ? 1 : -1
}

// 切换页码
const changePage = (page) => {
  if (page >= 1 && page <= totalPages) {
    currentPageNum.value = page
    // 滚动到顶部
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}
</script>

<template>
  <main class="app-main">
    <div class="project-square-container">
      <!-- 页面头部 -->
      <div class="page-header">
        <h1 class="page-title">项目广场</h1>
        <p class="page-description">探索优秀项目，发现创新灵感</p>
      </div>

      <!-- 项目列表 -->
      <div class="project-list" v-if="allProjects.length > 0 && !isLoading">
        <div 
          v-for="project in projects" 
          :key="project.id" 
          class="project-item"
        >
          <div class="project-info">
            <div class="project-header">
              <a href="#" class="project-name">{{ project.name }}</a>
            </div>
            
            <p class="project-description">{{ project.description }}</p>
            
            <div class="project-tags">
              <span 
                v-for="(tag, index) in project.tags" 
                :key="index"
                class="tech-tag"
              >
                {{ tag }}
              </span>
            </div>
            
            <div class="project-meta">
              <div class="author-info">
                <span class="author-avatar">{{ project.avatar }}</span>
                <span class="author-name">{{ project.author }}</span>
              </div>
              
              <div class="project-stats">
                <span class="stat-item update-time">
                  {{ project.updatedAt }}
                </span>
              </div>
            </div>
          </div>
          
          <div class="project-actions">
            <button 
              :class="['action-btn', 'like-btn', { active: project.isLiked }]"
              @click="handleLikeProject(project)"
              :title="project.isLiked ? '取消点赞' : '点赞'"
            >
              <span class="icon">{{ project.isLiked ? '❤️' : '🤍' }}</span>
              <span class="count">{{ formatNumber(project.likes) }}</span>
            </button>
            <button 
              :class="['action-btn', 'favorite-btn', { active: project.isFavorited }]"
              @click="handleFavoriteProject(project)"
              :title="project.isFavorited ? '取消收藏' : '收藏'"
            >
              <span class="icon">{{ project.isFavorited ? '⭐' : '☆' }}</span>
              <span class="count">{{ formatNumber(project.favorites) }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="isLoading" class="loading-state">
        <span class="loading-icon">⏳</span>
        <p class="loading-text">加载中...</p>
      </div>

      <!-- 空状态提示 -->
      <div v-if="allProjects.length === 0 && !isLoading" class="empty-state">
        <span class="empty-icon">📭</span>
        <p class="empty-text">暂无项目</p>
      </div>

      <!-- 分页控件（仅在有项目时显示） -->
      <div v-if="allProjects.length > 0" class="pagination-container">
        <div class="pagination">
          <button 
            class="page-btn prev"
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
            class="page-btn next"
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

.project-square-container {
  max-width: 1200px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #003366;
  margin-bottom: 8px;
}

.page-description {
  font-size: 16px;
  color: #666666;
  margin: 0;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 24px;
  box-shadow: 0 2px 4px rgba(0, 51, 102, 0.05);
}

.filter-left {
  flex: 1;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  font-weight: 500;
  color: #333333;
}

.filter-tag {
  padding: 6px 12px;
  background-color: #f5f5f5;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  color: #666666;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-tag:hover {
  background-color: #e6e6e6;
}

.filter-tag.active {
  background-color: #0059b3;
  border-color: #0059b3;
  color: #ffffff;
}

.search-box {
  margin-left: 16px;
}

.search-input {
  padding: 8px 12px;
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  color: #333333;
  font-size: 14px;
  width: 250px;
  outline: none;
  transition: all 0.2s;
}

.search-input::placeholder {
  color: #999999;
}

.search-input:focus {
  border-color: #0059b3;
  box-shadow: 0 0 0 3px rgba(0, 89, 179, 0.1);
}

/* 项目列表 */
.project-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 项目卡片 */
.project-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 16px;
  transition: all 0.2s;
  box-shadow: 0 2px 4px rgba(0, 51, 102, 0.05);
}

.project-item:hover {
  border-color: #0059b3;
  box-shadow: 0 4px 12px rgba(0, 51, 102, 0.15);
}

.project-info {
  flex: 1;
  margin-right: 16px;
}

.project-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.project-name {
  font-size: 18px;
  font-weight: 600;
  color: #0059b3;
  text-decoration: none;
  transition: color 0.2s;
}

.project-name:hover {
  color: #003366;
  text-decoration: underline;
}

.project-description {
  font-size: 14px;
  color: #666666;
  line-height: 1.6;
  margin-bottom: 12px;
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

.project-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-avatar {
  font-size: 20px;
}

.author-name {
  font-size: 13px;
  color: #666666;
}

.project-stats {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-item {
  font-size: 13px;
  color: #666666;
  cursor: pointer;
  transition: color 0.2s;
}

.stat-item:hover {
  color: #0059b3;
}

.update-time {
  color: #999999;
  font-size: 12px;
}

/* 操作按钮 */
.project-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-btn {
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  background-color: #ffffff;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 6px;
}

.like-btn {
  color: #e91e63;
  border-color: #e91e63;
}

.like-btn:hover,
.like-btn.active {
  background-color: #e91e63;
  color: #ffffff;
}

.favorite-btn {
  color: #f9ab00;
  border-color: #f9ab00;
}

.favorite-btn:hover,
.favorite-btn.active {
  background-color: #f9ab00;
  color: #ffffff;
}

.action-btn .icon {
  font-size: 16px;
}

.action-btn .count {
  font-weight: 500;
}

/* 分页控件 */
.pagination-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin-top: 32px;
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
  opacity: 0.6;
}

.page-info {
  font-size: 13px;
  color: #666666;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    gap: 12px;
  }
  
  .search-box {
    margin-left: 0;
    width: 100%;
  }
  
  .search-input {
    width: 100%;
  }
  
  .project-item {
    flex-direction: column;
  }
  
  .project-info {
    margin-right: 0;
    margin-bottom: 16px;
  }
  
  .project-actions {
    flex-direction: row;
    width: 100%;
  }
  
  .action-btn {
    flex: 1;
  }
  
  /* 分页控件响应式 */
  .pagination-container {
    flex-direction: column;
    gap: 12px;
    width: 100%;
  }
  
  .pagination-btn {
    padding: 10px;
  }
  
  .pagination-info {
    font-size: 14px;
  }
}

/* 分页控件样式 */
.pagination-container {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
}

.pagination-btn {
  padding: 8px 16px;
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  color: #0059b3;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.pagination-btn:hover:not(:disabled) {
  background-color: #f0f4ff;
  border-color: #0059b3;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-info {
  font-size: 14px;
  color: #666666;
  white-space: nowrap;
}

/* 点赞和收藏按钮样式 */
.like-btn.active,
.favorite-btn.active {
  color: #e74c3c;
  font-weight: bold;
}

.like-btn:hover,
.favorite-btn:hover {
  cursor: pointer;
  text-decoration: underline;
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
  margin-top: 24px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-text {
  font-size: 16px;
  color: #999999;
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
  margin-top: 24px;
}

.loading-icon {
  font-size: 64px;
  margin-bottom: 16px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.loading-text {
  font-size: 16px;
  color: #666666;
  margin: 0;
}
</style>
