<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { filterProjects, getFilteredProjectIds, batchDownloadAsync, getDownloadTaskStatus, cancelDownloadTask } from '@/api/project'
import { getActiveCourses } from '@/api/course'
import { toast } from '@/utils/toast'
import { log, error as logError } from '@/utils/logger'
import tokenManager from '@/utils/tokenManager'
import UserSidebar from '@/components/user/UserSidebar.vue'
import { formatNumber } from '@/utils/helpers'

import { getProjectTypeText } from '@/constants/project'

const router = useRouter()

// 筛选条件
const filters = ref({
  className: '',
  courseName: ''
})

// 课程列表（从课程字典获取）
const courseList = ref([])

// 当前页的项目列表
const currentPageProjects = ref([])

// 总记录数
const total = ref(0)

// 是否已执行查询
const hasSearched = ref(false)

// 加载状态
const loading = ref(false)

// 下载状态
const isDownloading = ref(false)
const downloadInfo = ref({
  total: 0,
  current: 0,
  progress: 0, // 百分比
  projectName: '',
  estimatedTime: ''
})

// WebSocket 进度监听器
let wsProgressHandler = null

// 异步任务恢复
const PENDING_DOWNLOAD_KEY = 'pendingBatchDownload'
let pollTimer = null
let pendingTaskId = null

// 每页显示数量
const PAGE_SIZE = 12
const currentPageNum = ref(1)

// 计算总页数
const totalPages = computed(() => Math.ceil(total.value / PAGE_SIZE))

// 切换页码
const changePage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPageNum.value = page
    loadFilteredProjects() // 重新加载数据
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

// 重置筛选
const resetFilters = () => {
  filters.value = {
    className: '',
    courseName: ''
  }
  currentPageNum.value = 1
  hasSearched.value = false
  currentPageProjects.value = []
  total.value = 0
}

// 执行查询
const handleSearch = () => {
  hasSearched.value = true
  currentPageNum.value = 1
  loadFilteredProjects()
}

// 加载筛选后的项目
const loadFilteredProjects = async () => {
  if (!hasSearched.value) {
    return
  }

  loading.value = true
  try {
    log('加载筛选项目，页码:', currentPageNum.value)

    const res = await filterProjects({
      className: filters.value.className,
      courseName: filters.value.courseName,
      projectType: 'COURSE', // 固定为课程设计
      pageNum: currentPageNum.value,
      pageSize: PAGE_SIZE
    })

    if (res.code === 200 && res.data) {
      // 解构响应数据 - 后端返回的是 PageResult 对象，项目列表在 records 字段中
      const { records = [], total: totalCount = 0 } = res.data
      currentPageProjects.value = records
      total.value = totalCount
      
      log(`加载完成，当前页项目数量: ${currentPageProjects.value.length}, 总数: ${totalCount}`)
    } else {
      logError('API 返回数据异常:', res)
      currentPageProjects.value = []
      total.value = 0
    }
  } catch (error) {
    logError('加载筛选项目失败:', error)
    toast.error(error.message || '加载项目失败，请稍后重试')
    currentPageProjects.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 清除待恢复的下载任务
const clearPendingDownload = () => {
  localStorage.removeItem(PENDING_DOWNLOAD_KEY)
  pendingTaskId = null
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// 开始轮询任务状态
const startPolling = (taskId) => {
  if (pollTimer) clearInterval(pollTimer)
  pollTimer = setInterval(async () => {
    try {
      const res = await getDownloadTaskStatus(taskId)
      if (res.code !== 200 || !res.data) return

      const task = res.data
      if (task.status === 'COMPLETED') {
        clearPendingDownload()
        if (task.downloadUrl) {
          window.location.href = task.downloadUrl
        }
        setTimeout(() => {
          isDownloading.value = false
          toast.success(`批量下载完成！成功: ${task.successCount}, 失败: ${task.failCount}`)
        }, 2000)
      } else if (task.status === 'FAILED' || task.status === 'CANCELLED') {
        clearPendingDownload()
        isDownloading.value = false
        if (task.status === 'FAILED') {
          toast.error(task.errorMessage || '批量下载失败')
        }
      } else {
        // PROCESSING — 更新进度显示
        downloadInfo.value.current = task.current || 0
        downloadInfo.value.total = task.total || downloadInfo.value.total
        downloadInfo.value.progress = task.progress || 0
        downloadInfo.value.projectName = task.projectName || '正在打包...'
      }
    } catch {
      // 网络错误时静默重试
    }
  }, 2000)
}

// 恢复未完成的下载任务（页面刷新后调用）
const recoverPendingDownload = () => {
  try {
    const saved = localStorage.getItem(PENDING_DOWNLOAD_KEY)
    if (!saved) return
    const { taskId, total } = JSON.parse(saved)
    if (!taskId) return

    log('发现未完成的下载任务，正在恢复:', taskId)
    pendingTaskId = taskId
    isDownloading.value = true
    downloadInfo.value = {
      total: total || 0,
      current: 0,
      progress: 0,
      projectName: '正在恢复下载任务...',
      estimatedTime: ''
    }

    // 注册 WebSocket 监听（可能收到实时消息）
    registerWsProgressHandler()

    // 启动轮询兜底
    startPolling(taskId)
  } catch {
    localStorage.removeItem(PENDING_DOWNLOAD_KEY)
  }
}

// 取消下载任务
const handleCancelDownload = async () => {
  const taskId = pendingTaskId
  if (!taskId) {
    isDownloading.value = false
    return
  }
  try {
    log('取消下载任务:', taskId)
    await cancelDownloadTask(taskId)
    toast.info('下载任务已取消')
  } catch {
    // 即使后端返回错误也关闭遮罩（任务可能已完成）
  }
  clearPendingDownload()
  isDownloading.value = false
}

// 批量下载学生项目
const handleBatchDownload = async () => {
  if (!hasSearched.value || total.value === 0) {
    toast.warning('请先查询并筛选出要下载的项目')
    return
  }

  if (!filters.value.className && !filters.value.courseName) {
    toast.warning('请至少选择班级或课程之一')
    return
  }

  try {
    log('开始批量下载...')
    toast.info('正在创建下载任务...')

    // 获取所有符合条件的项目ID
    const res = await getFilteredProjectIds({
      className: filters.value.className,
      courseName: filters.value.courseName,
      projectType: 'COURSE'
    })

    if (res.code !== 200 || !res.data) {
      logError('获取项目ID失败:', res)
      toast.error('获取项目列表失败，请稍后重试')
      return
    }

    const projectIds = res.data

    if (projectIds.length === 0) {
      toast.warning('当前没有可下载的项目')
      return
    }

    log(`获取到 ${projectIds.length} 个项目ID，创建异步下载任务...`)

    // 显示加载遮罩
    isDownloading.value = true
    downloadInfo.value = {
      total: projectIds.length,
      current: 0,
      progress: 0,
      projectName: '正在创建下载任务...',
      estimatedTime: ''
    }

    // 调用异步下载接口，后端立即返回 taskId
    const asyncRes = await batchDownloadAsync({
      projectIds: projectIds,
      className: filters.value.className || '未知班级',
      courseName: filters.value.courseName || '未知课程'
    })

    if (asyncRes.code !== 200 || !asyncRes.data || !asyncRes.data.taskId) {
      throw new Error('创建下载任务失败')
    }

    const taskId = asyncRes.data.taskId
    pendingTaskId = taskId
    log('异步下载任务已创建，taskId:', taskId)

    // 持久化任务信息，刷新后恢复
    localStorage.setItem(PENDING_DOWNLOAD_KEY, JSON.stringify({
      taskId: taskId,
      total: projectIds.length,
      timestamp: Date.now()
    }))

    // 启动轮询 + WebSocket 双通道
    startPolling(taskId)

  } catch (error) {
    logError('批量下载失败:', error)
    clearPendingDownload()
    isDownloading.value = false
    toast.error(error.message || '批量下载失败，请稍后重试')
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

// WebSocket 进度消息处理器
const createWsHandler = () => {
  return (data) => {
    log('收到 WebSocket 消息:', data)

    if (data.type === 'download_progress') {
      downloadInfo.value.current = data.current
      downloadInfo.value.total = data.total
      downloadInfo.value.progress = data.progress
      downloadInfo.value.projectName = data.projectName
      log(`下载进度: ${data.current}/${data.total} (${data.progress}%) - ${data.projectName}`)
    } else if (data.type === 'download_complete') {
      log(`下载完成，成功: ${data.successCount}, 失败: ${data.failCount}`)
      clearPendingDownload()

      if (data.downloadUrl) {
        log('使用后端提供的下载链接:', data.downloadUrl)
        window.location.href = data.downloadUrl
        setTimeout(() => {
          isDownloading.value = false
          toast.success(`批量下载完成！成功: ${data.successCount}, 失败: ${data.failCount}`)
        }, 2000)
      } else {
        setTimeout(() => {
          isDownloading.value = false
          toast.success(`批量下载完成！成功: ${data.successCount}, 失败: ${data.failCount}`)
        }, 1000)
      }
    } else if (data.type === 'download_failed') {
      logError('下载失败:', data.errorMessage)
      clearPendingDownload()
      isDownloading.value = false
      toast.error(`下载失败: ${data.errorMessage}`)
    }
  }
}

// 注册 WebSocket 进度监听（幂等）
const registerWsProgressHandler = () => {
  if (wsProgressHandler) return // 已注册
  import('@/utils/websocket').then(({ default: notificationWS }) => {
    wsProgressHandler = createWsHandler()
    notificationWS.on('message', wsProgressHandler)
    log('已注册 WebSocket 进度监听器')
  })
}

// 组件挂载时加载数据
onMounted(() => {
  const userInfoFromToken = tokenManager.getUserInfo()
  if (!userInfoFromToken || userInfoFromToken.role !== 'TEACHER') {
    toast.warning('只有教师可以访问此页面')
    router.push('/profile')
    return
  }

  loadCourseList()
  registerWsProgressHandler()

  // 尝试恢复未完成的下载任务
  recoverPendingDownload()
})

// 组件卸载时清理
onUnmounted(() => {
  if (wsProgressHandler) {
    import('@/utils/websocket').then(({ default: notificationWS }) => {
      notificationWS.off('message', wsProgressHandler)
      log('已移除 WebSocket 进度监听器')
    })
    wsProgressHandler = null
  }
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
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
                  v-if="hasSearched && total > 0"
                  @click="handleBatchDownload" 
                  class="batch-download-btn"
                  :disabled="isDownloading"
                >
                  {{ isDownloading ? '⏳ 打包中...' : '📁 批量下载' }}
                </button>
              </div>
            </div>

            <div class="filter-summary">
              <span v-if="!hasSearched">请输入筛选条件后点击"查询"按钮</span>
              <span v-else>共找到 <strong>{{ total }}</strong> 个项目，当前第 {{ currentPageNum }} / {{ totalPages }} 页</span>
            </div>
          </div>

          <!-- 下载进度遮罩 -->
          <div v-if="isDownloading" class="download-overlay">
            <div class="download-progress-card">
              <div class="spinner-large"></div>
              <h3>📦 正在打包项目</h3>
              
              <!-- 进度条 -->
              <div class="progress-bar-container">
                <div class="progress-bar" :style="{ width: downloadInfo.progress + '%' }"></div>
              </div>
              
              <p class="progress-text">
                {{ downloadInfo.current }} / {{ downloadInfo.total }} 个项目
              </p>
              
              <p class="project-name-text" v-if="downloadInfo.projectName">
                📄 {{ downloadInfo.projectName }}
              </p>
              
              <p class="time-estimate">
                ⏱️ 已完成 {{ downloadInfo.progress }}%
              </p>
              
              <div class="cancel-download-section">
                <button class="btn-cancel-download" @click="handleCancelDownload">
                  取消下载
                </button>
                <p class="tip-text">
                  💡 提示：打包完成后浏览器会自动开始下载
                </p>
              </div>
            </div>
          </div>

          <!-- 初始状态 -->
          <div v-else-if="!hasSearched" class="initial-state">
            <div class="empty-icon">🔍</div>
            <h3>开始查询学生项目</h3>
            <p>输入班级名称或选择课程，然后点击"查询"按钮</p>
          </div>

          <!-- 加载状态 -->
          <div v-else-if="loading" class="loading-state">
            <div class="spinner"></div>
            <p>加载中...</p>
          </div>

          <!-- 空状态 -->
          <div v-else-if="currentPageProjects.length === 0" class="empty-state">
            <div class="empty-icon">📥</div>
            <h3>未找到相关项目</h3>
            <p>尝试调整筛选条件或等待学生提交项目</p>
          </div>

          <!-- 项目列表 -->
          <div v-else class="project-grid">
            <div
              v-for="project in currentPageProjects"
              :key="project.id"
              class="project-card"
              @click="router.push(`/project/${project.id}`)"
            >
              <div class="project-card-header">
                <span class="project-name">{{ project.name }}</span>
                <span class="project-type-badge">{{ getProjectTypeText(project.projectType) }}</span>
              </div>
              
              <p class="project-description">{{ project.description || '暂无描述' }}</p>
              
              <div class="project-info">
                <div class="info-item">
                  <span class="info-label">👤 作者:</span>
                  <span class="info-value">{{ project.ownerUsername || project.author || '未知' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">🏫 班级:</span>
                  <span class="info-value">{{ project.ownerClassName || '未设置' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">📚 课程:</span>
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
          <div v-if="currentPageProjects.length > 0" class="pagination-container">
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
              共 {{ total }} 个项目，第 {{ currentPageNum }} / {{ totalPages }} 页
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
  line-clamp: 2;
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

/* 下载进度遮罩 */
.download-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(4px);
}

.download-progress-card {
  background: white;
  padding: 40px 50px;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  text-align: center;
  max-width: 450px;
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.spinner-large {
  width: 60px;
  height: 60px;
  border: 5px solid #e5e7eb;
  border-top-color: #064e3b;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.download-progress-card h3 {
  font-size: 22px;
  color: #064e3b;
  margin-bottom: 16px;
  font-weight: 700;
}

.progress-text {
  font-size: 16px;
  color: #374151;
  margin-bottom: 12px;
  line-height: 1.5;
}

/* 进度条容器 */
.progress-bar-container {
  width: 100%;
  height: 8px;
  background-color: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 16px;
}

/* 进度条 */
.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #064e3b 0%, #059669 100%);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.project-name-text {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f9fafb;
  border-radius: 6px;
  word-break: break-all;
}

.time-estimate {
  font-size: 15px;
  color: #059669;
  font-weight: 600;
  margin-bottom: 16px;
  padding: 10px 16px;
  background: #ecfdf5;
  border-radius: 8px;
  display: inline-block;
}

.tip-text {
  font-size: 13px;
  color: #6b7280;
  margin-top: 8px;
  line-height: 1.6;
}

.cancel-download-section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.btn-cancel-download {
  padding: 8px 24px;
  background: #fff;
  color: #ef4444;
  border: 1px solid #fecaca;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel-download:hover {
  background: #fef2f2;
  border-color: #fca5a5;
}
</style>
