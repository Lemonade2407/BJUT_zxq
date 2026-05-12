<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getProjectList, filterProjects, getProjectTypes, getProjectsByTag } from '@/api/project'
import { getActiveCourses } from '@/api/course'
import { getTags } from '@/api/tag'
import { toast } from '@/utils/toast'
import { log, error as logError } from '@/utils/logger'
import { formatDate, formatNumber } from '@/utils/helpers'

const router = useRouter()


// TODO: 添加搜索防抖功能（避免频繁请求）
// TODO: 添加筛选条件持久化（记住用户选择）

// 每页显示的项目数量
const PAGE_SIZE = 12
const currentPageNum = ref(1)

// 筛选条件
const filters = ref({
  projectType: '',  // 项目类型
  courseName: '',   // 课程名称
  tagId: null       // 标签ID
})

// 排序方式
const sortBy = ref('updatedAt')  // updatedAt | viewCount | starCount

// 项目列表（从后端 API 获取）
const allProjects = ref([])
const isLoading = ref(false)
const total = ref(0)  // 总记录数

// 筛选选项
const projectTypes = ref([])
const courseList = ref([])
const tagList = ref([])

// 获取项目列表
const fetchProjects = async () => {
  isLoading.value = true
  try {
    let response
    
    // 如果有标签筛选，使用标签查询接口
    if (filters.value.tagId) {
      response = await getProjectsByTag(filters.value.tagId, {
        pageNum: currentPageNum.value,
        pageSize: PAGE_SIZE
      })
    }
    // 如果有其他筛选条件，使用 filterProjects 接口
    else if (filters.value.projectType || filters.value.courseName) {
      response = await filterProjects({
        projectType: filters.value.projectType,
        courseName: filters.value.courseName,
        pageNum: currentPageNum.value,
        pageSize: PAGE_SIZE
      })
    } else {
      // 否则使用 getProjectList 接口
      response = await getProjectList({
        pageNum: currentPageNum.value,
        pageSize: PAGE_SIZE
      })
    }
    
    if (response.code === 200 && response.data) {
      // 后端返回的是 PageResult 对象
      const { records = [], total: totalCount = 0 } = response.data
      total.value = totalCount
      
      // 按选定的方式排序（后端已排序，前端只需展示）
      allProjects.value = records.map(project => ({
        id: project.id,
        name: project.name,
        description: project.description || '暂无描述',
        tags: project.tags ? project.tags.map(tag => tag.name) : [],
        avatar: '👤',
        author: project.author || '未知用户',
        updatedAt: formatDate(project.updatedAt),
        starCount: project.starCount || 0,
        watchCount: project.watchCount || 0,
        viewCount: project.viewCount || 0,
        projectType: project.projectType || 'OTHER',
        isStarred: project.isStarred || false,
        isWatched: project.isWatched || false
      }))
      log('项目列表加载成功，数量：', allProjects.value.length, '总数：', total.value)
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

// 排序项目
const sortProjects = (projects) => {
  return [...projects].sort((a, b) => {
    switch (sortBy.value) {
      case 'viewCount':
        return (b.viewCount || 0) - (a.viewCount || 0)
      case 'starCount':
        return (b.starCount || 0) - (a.starCount || 0)
      case 'updatedAt':
      default:
        const timeA = new Date(a.updatedAt || a.createdAt).getTime()
        const timeB = new Date(b.updatedAt || b.createdAt).getTime()
        return timeB - timeA
    }
  })
}

// 加载项目类型字典
const loadProjectTypes = async () => {
  try {
    const res = await getProjectTypes()
    if (res.code === 200 && res.data) {
      projectTypes.value = res.data
    }
  } catch (error) {
    logError('加载项目类型失败:', error)
  }
}

// 加载课程列表
const loadCourseList = async () => {
  try {
    const res = await getActiveCourses()
    if (res.code === 200 && res.data) {
      courseList.value = res.data.map(course => course.courseName).sort()
    }
  } catch (error) {
    logError('加载课程列表失败:', error)
  }
}

// 加载标签列表
const loadTags = async () => {
  try {
    const res = await getTags()
    if (res.code === 200 && res.data) {
      tagList.value = res.data
    }
  } catch (error) {
    logError('加载标签列表失败:', error)
  }
}

// 重置筛选条件
const resetFilters = () => {
  filters.value.projectType = ''
  filters.value.courseName = ''
  filters.value.tagId = null
  sortBy.value = 'updatedAt'
  currentPageNum.value = 1
  fetchProjects()
}

// 监听筛选条件变化
watch([() => filters.value.projectType, () => filters.value.courseName, () => filters.value.tagId, sortBy], () => {
  currentPageNum.value = 1
  fetchProjects()  // 重新从后端获取数据
})

// 组件挂载时获取数据
onMounted(() => {
  fetchProjects()
  loadProjectTypes()
  loadCourseList()
  loadTags()
})

// 计算总页数
const totalPages = computed(() => Math.ceil(total.value / PAGE_SIZE))

// 计算当前页的项目列表（后端已分页，直接返回）
const projects = computed(() => allProjects.value)

// 点赞功能
const handleLikeProject = (project) => {
  project.isStarred = !project.isStarred
  project.starCount += project.isStarred ? 1 : -1
}

// 收藏功能
const handleFavoriteProject = (project) => {
  project.isWatched = !project.isWatched
  project.watchCount += project.isWatched ? 1 : -1
}

// 切换页码
const changePage = (page) => {
  if (page >= 1 && page <= totalPages) {
    currentPageNum.value = page
    // 滚动到顶部
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

// 打开项目详情
const handleProjectClick = (project) => {
  window.location.href = `/project/${project.id}`
}
</script>

<template>
  <main class="app-main">
    <div class="project-square-container">
      <!-- 页面头部 -->
      <div class="page-header">
        <div class="header-row">
          <div class="header-text">
            <h1 class="page-title">项目广场</h1>
            <p class="page-description">探索优秀项目，发现创新灵感</p>
          </div>
          <button class="create-btn" @click="router.push('/create-project')">
            <svg viewBox="0 0 16 16" width="16" height="16" fill="currentColor">
              <path d="M8 1.25a.75.75 0 01.75.75v5.25H14a.75.75 0 010 1.5H8.75V14a.75.75 0 01-1.5 0V8.75H2a.75.75 0 010-1.5h5.25V2A.75.75 0 018 1.25z"/>
            </svg>
            新建项目
          </button>
        </div>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <div class="filter-row">
          <div class="filter-item">
            <label class="filter-label">项目类型</label>
            <select v-model="filters.projectType" class="filter-select">
              <option value="">全部类型</option>
              <option v-for="type in projectTypes" :key="type.typeCode" :value="type.typeCode">
                {{ type.typeName }}
              </option>
            </select>
          </div>

          <div class="filter-item">
            <label class="filter-label">课程</label>
            <select v-model="filters.courseName" class="filter-select">
              <option value="">全部课程</option>
              <option v-for="course in courseList" :key="course" :value="course">
                {{ course }}
              </option>
            </select>
          </div>

          <div class="filter-item">
            <label class="filter-label">标签</label>
            <select v-model="filters.tagId" class="filter-select">
              <option :value="null">全部标签</option>
              <option v-for="tag in tagList" :key="tag.id" :value="tag.id">
                {{ tag.name }}
              </option>
            </select>
          </div>

          <div class="filter-item">
            <label class="filter-label">排序方式</label>
            <select v-model="sortBy" class="filter-select">
              <option value="updatedAt">按更新时间</option>
              <option value="viewCount">按浏览量</option>
              <option value="starCount">按点赞数</option>
            </select>
          </div>

          <div class="filter-actions">
            <button @click="resetFilters" class="reset-btn">
              🔄 重置
            </button>
          </div>
        </div>
      </div>

      <!-- 项目网格 -->
      <div v-if="projects.length > 0" class="project-grid">
        <div 
          v-for="project in projects" 
          :key="project.id" 
          class="project-card"
          @click="handleProjectClick(project)"
        >
          <div class="project-card-header">
            <span class="project-name">{{ project.name }}</span>
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
            <div class="project-stats">
              <span class="stat">
                ❤️ {{ formatNumber(project.starCount) }}
              </span>
              <span class="stat">
                ⭐ {{ formatNumber(project.watchCount) }}
              </span>
            </div>
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
        <span class="empty-icon">📥</span>
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

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #064e3b 0%, #047857 100%);
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  flex-shrink: 0;
}

.create-btn:hover {
  background: linear-gradient(135deg, #047857 0%, #10b981 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(6, 78, 59, 0.3);
}

.create-btn:active {
  transform: translateY(0);
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

.filter-row {
  display: flex;
  gap: 16px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.filter-item {
  flex: 1;
  min-width: 150px;
}

.filter-label {
  display: block;
  font-size: 13px;
  color: #666666;
  margin-bottom: 6px;
  font-weight: 500;
}

.filter-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  color: #333333;
  background-color: #ffffff;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-select:hover {
  border-color: #10b981;
}

.filter-select:focus {
  outline: none;
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.filter-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.reset-btn {
  padding: 8px 16px;
  background-color: #f5f5f5;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  color: #666666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.reset-btn:hover {
  background-color: #e8e8e8;
  border-color: #10b981;
  color: #10b981;
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
  border-color: #10b981;
  box-shadow: 0 4px 12px rgba(6, 78, 59, 0.15);
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
  color: #10b981;
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

/* 响应式设计 */
@media (max-width: 768px) {
  .page-title {
    font-size: 24px;
  }
  
  .filter-row {
    flex-direction: column;
    gap: 12px;
  }
  
  .filter-item {
    width: 100%;
    min-width: 0;
  }
  
  .filter-actions {
    width: 100%;
    justify-content: flex-end;
  }
  
  .project-grid {
    grid-template-columns: 1fr;
  }
}
</style>
