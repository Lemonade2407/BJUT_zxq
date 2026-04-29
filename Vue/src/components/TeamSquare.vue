<script setup>
import { ref, computed } from 'vue'
import { toast } from '@/utils/toast'

// 每页显示的项目数量
const PAGE_SIZE = 6
const currentPageNum = ref(1)

// 组队项目列表（从后端 API 获取）
const allTeamProjects = ref([])

const getStatusText = (status) => {
  const statusMap = {
    recruiting: '招募中',
    full: '已满员',
    ended: '已结束'
  }
  return statusMap[status] || status
}

const getStatusClass = (status) => {
  return `status-${status}`
}

// 计算总页数
const totalPages = computed(() => Math.ceil(allTeamProjects.value.length / PAGE_SIZE))

// 计算当前页的项目列表
const teamProjects = computed(() => {
  const start = (currentPageNum.value - 1) * PAGE_SIZE
  const end = start + PAGE_SIZE
  return allTeamProjects.value.slice(start, end)
})

// 切换页码
const changePage = (page) => {
  if (page >= 1 && page <= totalPages) {
    currentPageNum.value = page
    // 滚动到顶部
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

// 查看详情
const handleViewDetail = (project) => {
  const detailText = `【${project.title}】\n\n${project.detail}\n\n当前人数：${project.members.current}/${project.members.needed}人\n状态：${getStatusText(project.status)}\n技术栈：${project.tags.join('、')}`
  toast.info(detailText, 5000) // 显示5秒
}
</script>

<template>
  <main class="app-main">
    <div class="team-square-container">
      <!-- 页面头部 -->
      <div class="page-header">
        <h1 class="page-title">组队广场</h1>
        <p class="page-description">寻找志同道合的队友，一起创造优秀项目</p>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <div class="filter-group">
          <label class="filter-label">技术栈：</label>
          <button class="filter-tag active">全部</button>
          <button class="filter-tag">Vue.js</button>
          <button class="filter-tag">Python</button>
          <button class="filter-tag">Node.js</button>
          <button class="filter-tag">Java</button>
        </div>
        <div class="filter-group">
          <label class="filter-label">状态：</label>
          <select class="filter-select">
            <option value="all">全部</option>
            <option value="recruiting">招募中</option>
            <option value="full">已满员</option>
          </select>
        </div>
      </div>

      <!-- 项目列表 -->
      <div class="project-grid">
        <div 
          v-for="project in teamProjects" 
          :key="project.id" 
          class="project-card"
        >
          <div class="project-header">
            <h3 class="project-title">{{ project.title }}</h3>
            <span 
              :class="['project-status', getStatusClass(project.status)]"
            >
              {{ getStatusText(project.status) }}
            </span>
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
          
          <div class="project-footer">
            <div class="members-info">
              <span class="members-count">
                👥 {{ project.members.current }}/{{ project.members.needed }} 人
              </span>
            </div>
            <button 
              class="detail-btn"
              @click="handleViewDetail(project)"
            >
              📋 查看详情
            </button>
          </div>
        </div>
      </div>

      <!-- 分页控件 -->
      <div class="pagination-container">
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
          共 {{ allTeamProjects.length }} 个项目，第 {{ currentPageNum }} / {{ totalPages }} 页
        </div>
      </div>

      <!-- 空状态提示 -->
      <div v-if="allTeamProjects.length === 0" class="empty-state">
        <span class="empty-icon">📭</span>
        <p class="empty-text">暂无组队项目</p>
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

.team-square-container {
  max-width: 1400px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #064e3b;
  margin-bottom: 8px;
}

.page-description {
  font-size: 16px;
  color: #666666;
  margin: 0;
}

/* 筛选栏 */
.filter-bar {
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 24px;
  box-shadow: 0 2px 4px rgba(0, 51, 102, 0.05);
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.filter-group:last-child {
  margin-bottom: 0;
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
  background-color: #10b981;
  border-color: #10b981;
  color: #ffffff;
}

.filter-select {
  padding: 6px 12px;
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  color: #333333;
  font-size: 13px;
  cursor: pointer;
  outline: none;
}

.filter-select:focus {
  border-color: #10b981;
}

/* 项目网格 */
.project-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

@media (max-width: 1200px) {
  .project-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .project-grid {
    grid-template-columns: 1fr;
  }
}

/* 项目卡片 */
.project-card {
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 16px;
  transition: all 0.2s;
  box-shadow: 0 2px 4px rgba(0, 51, 102, 0.05);
}

.project-card:hover {
  border-color: #10b981;
  box-shadow: 0 4px 12px rgba(6, 78, 59, 0.15);
  transform: translateY(-2px);
}

.project-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.project-title {
  font-size: 18px;
  font-weight: 600;
  color: #064e3b;
  margin: 0;
  flex: 1;
}

.project-status {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  margin-left: 8px;
}

.project-status.status-recruiting {
  background-color: rgba(24, 128, 56, 0.1);
  color: #188038;
}

.project-status.status-full {
  background-color: rgba(0, 0, 0, 0.1);
  color: #666666;
}

.project-status.status-ended {
  background-color: rgba(217, 48, 37, 0.1);
  color: #d93025;
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
  margin-bottom: 16px;
}

.tech-tag {
  padding: 4px 10px;
  background-color: rgba(16, 185, 129, 0.08);
  border-radius: 4px;
  font-size: 12px;
  color: #10b981;
  font-weight: 500;
}

.project-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.members-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.members-count {
  font-size: 13px;
  color: #666666;
  font-weight: 500;
}

/* 查看详情按钮 */
.detail-btn {
  padding: 8px 20px;
  background-color: #10b981;
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.detail-btn:hover {
  background-color: #059669;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
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
  background-color: #10b981;
  border-color: #10b981;
  color: #ffffff;
}

.page-btn.active {
  background-color: #10b981;
  border-color: #10b981;
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

.empty-text {
  font-size: 16px;
  color: #999999;
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .filter-group {
    flex-wrap: wrap;
  }
  
  .app-main {
    padding: 16px;
  }
}
</style>
