<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProjectList, batchDownloadProjects } from '@/api/project'
import { getActiveCourses } from '@/api/course'
import { toast } from '@/utils/toast'
import { log, error as logError } from '@/utils/logger'
import tokenManager from '@/utils/tokenManager'
import UserSidebar from '@/components/UserSidebar.vue'

const router = useRouter()

// 用户信息
const userInfo = ref({
  id: null,
  username: '',
  role: ''
})

// 筛选条件
const filters = ref({
  className: '',
  courseName: ''
})

// 课程列表（从课程字典获取）
const courseList = ref([])

// 所有项目
const allProjects = ref([])

// 是否已执行查询
const hasSearched = ref(false)

// 加载状态
const loading = ref(false)

// 每页显示数量
const PAGE_SIZE = 12
const currentPageNum = ref(1)

// 计算总页数
const totalPages = computed(() => Math.ceil(filteredProjects.value.length / PAGE_SIZE))

// 计算当前页的项目
const paginatedProjects = computed(() => {
  const start = (currentPageNum.value - 1) * PAGE_SIZE
  const end = start + PAGE_SIZE
  return filteredProjects.value.slice(start, end)
})

// 根据筛选条件过滤项目
const filteredProjects = computed(() => {
  // 如果未执行查询，返回空数组
  if (!hasSearched.value) {
    return []
  }

  let result = allProjects.value

  // 按班级筛选
  if (filters.value.className) {
    result = result.filter(project => 
      project.ownerClassName && 
      project.ownerClassName.includes(filters.value.className)
    )
  }

  // 按课程筛选
  if (filters.value.courseName) {
    result = result.filter(project => 
      project.projectType === 'COURSE' && 
      project.courseName === filters.value.courseName
    )
  }

  return result
})

// 格式化数字
const formatNumber = (num) => {
  if (num === undefined || num === null) return '0'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num.toString()
}

// 获取项目类型文本
const getProjectTypeText = (type) => {
  const typeMap = {
    'COURSE': '课程设计',
    'THESIS': '毕业设计',
    'COMPETITION': '竞赛作品',
    'PERSONAL': '个人项目',
    'OTHER': '其他'
  }
  return typeMap[type] || '未知'
}

// 切换页码
const changePage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPageNum.value = page
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

// 跳转到项目详情
const handleProjectClick = (projectId) => {
  router.push(`/project/${projectId}`)
}

// 重置筛选
const resetFilters = () => {
  filters.value = {
    className: '',
    courseName: ''
  }
  currentPageNum.value = 1
  hasSearched.value = false
}

// 执行查询
const handleSearch = () => {
  hasSearched.value = true
  currentPageNum.value = 1
}

// 批量下载学生项目
const handleBatchDownload = async () => {
  if (!hasSearched.value || filteredProjects.value.length === 0) {
    toast.warning('请先查询并筛选出要下载的项目')
    return
  }
  
  // 检查是否有班级和课程信息
  if (!filters.value.className && !filters.value.courseName) {
    toast.warning('请至少选择班级或课程之一')
    return
  }
  
  try {
    log('开始批量下载，项目数量:', filteredProjects.value.length)
    toast.info(`正在打包 ${filteredProjects.value.length} 个项目...`)
    
    // 提取所有项目 ID
    const projectIds = filteredProjects.value.map(p => p.id)
    
    // 调用批量下载 API
    await batchDownloadProjects({
      projectIds: projectIds,
      className: filters.value.className || '未知班级',
      courseName: filters.value.courseName || '未知课程'
    })
    
    toast.success('批量下载成功！')
  } catch (error) {
    logError('批量下载失败:', error)
    toast.error(error.message || '批量下载失败，请稍后重试')
  }
}

// 加载学生项目
const loadStudentProjects = async () => {
  loading.value = true
  try {
    // 获取当前用户信息
    const userInfoFromToken = tokenManager.getUserInfo()
    if (userInfoFromToken) {
      userInfo.value = userInfoFromToken
    }

    // 检查是否为教师
    if (userInfo.value.role !== 'TEACHER') {
      toast.warning('只有教师可以访问此页面')
      router.push('/profile')
      return
    }

    log('加载学生项目列表...')

    // 获取所有公开的课程设计项目
    const res = await getProjectList({
      pageNum: 1,
      pageSize: 1000, // 获取尽可能多的数据用于筛选
      projectType: 'COURSE'
    })

    if (res.code === 200 && res.data) {
      allProjects.value = res.data
      log(`加载完成，项目数量: ${allProjects.value.length}`)
    } else {
      logError('API 返回数据异常:', res)
      allProjects.value = []
    }
  } catch (error) {
    logError('加载学生项目失败:', error)
    toast.error(error.message || '加载项目失败，请稍后重试')
    allProjects.value = []
  } finally {
    loading.value = false
  }
}

// 加载课程列表
const loadCourseList = async () => {
  try {
    log('加载课程列表...')
    const res = await getActiveCourses()
    
    if (res.code === 200 && res.data) {
      // 提取课程名称列表
      courseList.value = res.data.map(course => course.courseName).sort()
      log(`加载完成，课程数量: ${courseList.value.length}`)
    } else {
      logError('课程 API 返回数据异常:', res)
      courseList.value = []
    }
  } catch (error) {
    logError('加载课程列表失败:', error)
    courseList.value = []
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadStudentProjects()
  loadCourseList()
})
</script>

<template>
  <div class="class-management-page">
    <div class="class-management-layout">
      <!-- 侧边栏 -->
      <UserSidebar />

      <!-- 主内容区 -->
      <main class="class-management-main">
        <div class="container">
          <!-- 页面标题 -->
          <div class="page-header">
            <h1>📚 教学班级管理</h1>
            <p class="subtitle">查看和管理学生的课程设计项目</p>
          </div>

          <!-- 筛选栏 -->
          <div class="filter-bar">
            <div class="filter-row">
              <div class="filter-item">
                <label class="filter-label">班级</label>
                <input
                  v-model="filters.className"
                  type="text"
                  placeholder="输入班级名称"
                  class="filter-input"
                  @input="currentPageNum = 1"
                />
              </div>

              <div class="filter-item">
                <label class="filter-label">课程</label>
                <select
                  v-model="filters.courseName"
                  class="filter-select"
                  @change="currentPageNum = 1"
                >
                  <option value="">全部课程</option>
                  <option v-for="course in courseList" :key="course" :value="course">
                    {{ course }}
                  </option>
                </select>
              </div>

              <div class="filter-actions">
                <button @click="handleSearch" class="search-btn">
                  🔍 查询
                </button>
                <button @click="resetFilters" class="reset-btn">
                  🔄 重置
                </button>
                <button 
                  v-if="hasSearched && filteredProjects.length > 0"
                  @click="handleBatchDownload" 
                  class="batch-download-btn"
                >
                  📦 批量下载 ({{ filteredProjects.length }})
                </button>
              </div>
            </div>

            <div class="filter-summary">
              <span v-if="!hasSearched">请输入筛选条件后点击“查询”按钮</span>
              <span v-else>共找到 <strong>{{ filteredProjects.length }}</strong> 个项目</span>
            </div>
          </div>

          <!-- 初始状态 -->
          <div v-if="!hasSearched" class="initial-state">
            <div class="empty-icon">🔍</div>
            <h3>开始查询学生项目</h3>
            <p>输入班级名称或选择课程，然后点击“查询”按钮</p>
          </div>

          <!-- 加载状态 -->
          <div v-else-if="loading" class="loading-state">
            <div class="spinner"></div>
            <p>加载中...</p>
          </div>

          <!-- 空状态 -->
          <div v-else-if="filteredProjects.length === 0" class="empty-state">
            <div class="empty-icon">📭</div>
            <h3>未找到相关项目</h3>
            <p>尝试调整筛选条件或等待学生提交项目</p>
          </div>

          <!-- 项目列表 -->
          <div v-else class="project-grid">
            <div
              v-for="project in paginatedProjects"
              :key="project.id"
              class="project-card"
              @click="handleProjectClick(project.id)"
            >
              <div class="project-card-header">
                <span class="project-name">{{ project.name }}</span>
                <span class="project-type-badge">{{ getProjectTypeText(project.projectType) }}</span>
              </div>
              
              <p class="project-description">{{ project.description || '暂无描述' }}</p>
              
              <div class="project-info">
                <div class="info-item">
                  <span class="info-label">👤 作者:</span>
                  <span class="info-value">{{ project.ownerUsername || '未知' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">🏫 班级:</span>
                  <span class="info-value">{{ project.ownerClassName || '未设置' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">📖 课程:</span>
                  <span class="info-value">{{ project.courseName || '未设置' }}</span>
                </div>
              </div>

              <div class="project-tags">
                <span v-for="tag in (project.tags || [])" :key="tag.id" class="tech-tag">
                  {{ tag.name }}
                </span>
              </div>

              <div class="project-footer">
                <div class="project-stats">
                  <span class="stat">❤️ {{ formatNumber(project.starCount || 0) }}</span>
                  <span class="stat">⭐ {{ formatNumber(project.watchCount || 0) }}</span>
                  <span class="stat">👁️ {{ formatNumber(project.viewCount || 0) }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 分页控件 -->
          <div v-if="filteredProjects.length > 0" class="pagination-container">
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
              共 {{ filteredProjects.length }} 个项目，第 {{ currentPageNum }} / {{ totalPages }} 页
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.class-management-page {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  min-height: calc(100vh - 200px);
  width: 100%;
  background-color: #f5f7fa;
}

/* 布局容器 */
.class-management-layout {
  display: flex;
  gap: 24px;
  max-width: 1600px;
  margin: 0 auto;
  min-height: calc(100vh - 120px);
}

/* 主内容区 */
.class-management-main {
  flex: 1;
  min-width: 0;
}

.container {
  width: 100%;
}

/* 页面头部 */
.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 28px;
  font-weight: 700;
  color: #064e3b;
  margin: 0 0 8px 0;
}

.subtitle {
  font-size: 16px;
  color: #666;
  margin: 0;
}

/* 筛选栏 */
.filter-bar {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.filter-row {
  display: flex;
  gap: 16px;
  align-items: flex-end;
  margin-bottom: 16px;
}

.filter-item {
  flex: 1;
}

.filter-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.filter-input,
.filter-select {
  width: 100%;
  padding: 10px 14px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
  background-color: #fafafa;
}

.filter-input:focus,
.filter-select:focus {
  border-color: #064e3b;
  outline: none;
  background-color: #ffffff;
  box-shadow: 0 0 0 4px rgba(6, 78, 59, 0.1);
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.search-btn {
  padding: 10px 24px;
  background: linear-gradient(135deg, #064e3b 0%, #047857 100%);
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(6, 78, 59, 0.3);
}

.search-btn:hover {
  background: linear-gradient(135deg, #047857 0%, #059669 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(6, 78, 59, 0.4);
}

.search-btn:active {
  transform: translateY(0);
}

.reset-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%);
  border: 2px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
}

.reset-btn:hover {
  background: linear-gradient(135deg, #e8e8e8 0%, #d9d9d9 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.batch-download-btn {
  padding: 10px 24px;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.3);
}

.batch-download-btn:hover {
  background: linear-gradient(135deg, #d97706 0%, #b45309 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.4);
}

.batch-download-btn:active {
  transform: translateY(0);
}

.filter-summary {
  font-size: 14px;
  color: #666;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.filter-summary strong {
  color: #064e3b;
  font-size: 16px;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #064e3b;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state h3 {
  font-size: 20px;
  color: #333;
  margin: 0 0 8px 0;
}

.empty-state p {
  font-size: 14px;
  color: #999;
  margin: 0;
}

/* 初始状态 */
.initial-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.initial-state h3 {
  font-size: 20px;
  color: #333;
  margin: 0 0 8px 0;
}

.initial-state p {
  font-size: 14px;
  color: #999;
  margin: 0;
}

/* 项目网格 */
.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

/* 项目卡片 */
.project-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 2px solid transparent;
}

.project-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(6, 78, 59, 0.15);
  border-color: #064e3b;
}

.project-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.project-name {
  font-size: 18px;
  font-weight: 700;
  color: #064e3b;
  flex: 1;
}

.project-type-badge {
  padding: 4px 10px;
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  color: #064e3b;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.project-description {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 项目信息 */
.project-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.info-label {
  color: #999;
  min-width: 60px;
}

.info-value {
  color: #333;
  font-weight: 500;
}

/* 标签 */
.project-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.tech-tag {
  padding: 4px 10px;
  background-color: rgba(6, 78, 59, 0.08);
  border-radius: 6px;
  font-size: 12px;
  color: #064e3b;
  font-weight: 500;
}

/* 项目底部 */
.project-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.project-stats {
  display: flex;
  gap: 16px;
}

.stat {
  font-size: 13px;
  color: #666;
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
  border: 2px solid #d9d9d9;
  border-radius: 8px;
  color: #333;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.page-btn:hover:not(:disabled) {
  background-color: #064e3b;
  border-color: #064e3b;
  color: #ffffff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(6, 78, 59, 0.25);
}

.page-btn:disabled {
  background-color: #f5f5f5;
  color: #ccc;
  cursor: not-allowed;
}

.page-btn.active {
  background-color: #064e3b;
  border-color: #064e3b;
  color: #ffffff;
}

.page-info {
  font-size: 14px;
  color: #666;
}

/* 响应式 */
@media (max-width: 768px) {
  .class-management-layout {
    flex-direction: column;
  }

  .filter-row {
    flex-direction: column;
  }

  .project-grid {
    grid-template-columns: 1fr;
  }

  .page-header h1 {
    font-size: 24px;
  }
}
</style>
