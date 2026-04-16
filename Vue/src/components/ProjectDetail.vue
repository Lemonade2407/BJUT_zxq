<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProjectDetail, starProject, unstarProject, watchProject, unwatchProject, downloadProject, updateProject, deleteProject } from '@/api/project'
import { getProjectComments, createComment } from '@/api/comment'
import { getTags } from '@/api/tag'
import { toast } from '@/utils/toast'
import { log, error as logError, warn } from '@/utils/logger'
import tokenManager from '@/utils/tokenManager'

const route = useRoute()
const router = useRouter()

// 项目 ID（从路由参数获取）
const projectId = ref(route.params.id)

// 当前用户信息
const currentUser = computed(() => tokenManager.getUserInfo())
const isOwner = computed(() => {
  return project.value && currentUser.value && project.value.ownerId === currentUser.value.id
})

// 项目详情
const project = ref(null)
const isLoading = ref(false)

// 评论列表
const comments = ref([])
const newComment = ref('')

// 标签页
const activeTab = ref('readme') // readme, code, issues, comments, settings

// 标签列表
const allTags = ref([])

// 编辑表单
const editForm = ref({
  name: '',
  description: '',
  visibility: 1,
  tagIds: []
})
const isEditing = ref(false)
const showDeleteConfirm = ref(false)

// 加载项目详情
const loadProjectDetail = async () => {
  if (!projectId.value) {
    warn('项目 ID 为空')
    return
  }

  isLoading.value = true
  try {
    log('加载项目详情，项目 ID:', projectId.value)
    
    // 调用真实 API 获取项目详情（后端会自动增加浏览量）
    const res = await getProjectDetail(projectId.value)
    
    if (res.code === 200 && res.data) {
      project.value = res.data
      
      // 格式化时间字段
      if (project.value.createdAt) {
        project.value.createdAt = formatDateTime(project.value.createdAt)
      }
      if (project.value.updatedAt) {
        project.value.updatedAt = formatDateTime(project.value.updatedAt)
      }
      
      log('项目详情加载成功:', project.value.name)
      
      // 加载评论数据
      await loadComments()
      
      // 如果是所有者，初始化编辑表单
      if (isOwner.value) {
        initEditForm()
      }
    } else {
      logError('API 返回数据异常:', res)
      toast.error(res.message || '加载项目详情失败')
    }
  } catch (error) {
    logError('加载项目详情失败:', error)
    toast.error(error.message || '加载项目详情失败，请稍后重试')
  } finally {
    isLoading.value = false
  }
}

// 加载评论列表
const loadComments = async () => {
  try {
    const res = await getProjectComments(projectId.value)
    if (res.code === 200 && res.data) {
      comments.value = res.data
      log('评论加载成功，数量:', comments.value.length)
    }
  } catch (error) {
    logError('加载评论失败:', error)
    // 评论加载失败不影响主流程
  }
}

// 切换标签页
const switchTab = (tab) => {
  activeTab.value = tab
}

// 提交评论
const submitComment = async () => {
  if (!newComment.value.trim()) {
    toast.warning('请输入评论内容')
    return
  }

  try {
    // 调用真实 API 提交评论
    const res = await createComment(projectId.value, {
      content: newComment.value.trim()
    })
    
    if (res.code === 200 && res.data) {
      // 将新评论添加到列表开头
      comments.value.unshift(res.data)
      newComment.value = ''
      toast.success('评论成功！')
      log('评论提交成功')
    }
  } catch (error) {
    logError('提交评论失败:', error)
    toast.error(error.message || '评论失败，请稍后重试')
  }
}

// 点赞项目
const toggleLike = async () => {
  try {
    if (project.value.isLiked) {
      // 取消点赞
      const res = await unstarProject(project.value.id)
      // 使用后端返回的实际数量
      if (res.code === 200 && res.data !== undefined) {
        project.value.likes = res.data
      }
      project.value.isLiked = false
    } else {
      // 点赞
      const res = await starProject(project.value.id)
      // 使用后端返回的实际数量
      if (res.code === 200 && res.data !== undefined) {
        project.value.likes = res.data
      }
      project.value.isLiked = true
    }
  } catch (error) {
    logError('点赞操作失败:', error)
    toast.error(error.message || '操作失败，请稍后重试')
  }
}

// 收藏项目
const toggleFavorite = async () => {
  try {
    if (project.value.isFavorited) {
      // 取消收藏
      const res = await unwatchProject(project.value.id)
      // 使用后端返回的实际数量
      if (res.code === 200 && res.data !== undefined) {
        project.value.favorites = res.data
      }
      project.value.isFavorited = false
    } else {
      // 收藏
      const res = await watchProject(project.value.id)
      // 使用后端返回的实际数量
      if (res.code === 200 && res.data !== undefined) {
        project.value.favorites = res.data
      }
      project.value.isFavorited = true
    }
  } catch (error) {
    logError('收藏操作失败:', error)
    toast.error(error.message || '操作失败，请稍后重试')
  }
}

// 下载项目
const downloadProjectHandler = async () => {
  try {
    toast.info('正在准备下载...')
    
    // 调用真实 API 下载项目（后端会自动增加下载次数）
    await downloadProject(projectId.value)
    
    toast.success('下载成功！')
    log('项目下载成功，项目 ID:', projectId.value)
  } catch (error) {
    logError('下载项目失败:', error)
    toast.error(error.message || '下载失败，请稍后重试')
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

// 格式化时间(只保留年月日)
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 获取作者名称（从标签或分类中推断）
const getAuthorName = () => {
  if (!project.value) return '-'
  // 如果有ownerId，可以显示用户信息
  // 目前先显示项目ID作为标识
  return `用户 #${project.value.ownerId || '未知'}`
}

// 初始化编辑表单
const initEditForm = () => {
  if (!project.value) return
  
  editForm.value = {
    name: project.value.name || '',
    description: project.value.description || '',
    visibility: project.value.visibility !== undefined ? project.value.visibility : 1,
    tagIds: project.value.tags ? project.value.tags.map(tag => tag.id) : []
  }
}

// 加载标签列表
const loadTags = async () => {
  try {
    const tagsRes = await getTags()
    
    if (tagsRes.code === 200) {
      allTags.value = tagsRes.data || []
    }
  } catch (error) {
    logError('加载标签失败:', error)
  }
}

// 开始编辑
const startEdit = () => {
  initEditForm()
  isEditing.value = true
}

// 取消编辑
const cancelEdit = () => {
  isEditing.value = false
  initEditForm()
}

// 保存修改
const saveChanges = async () => {
  if (!editForm.value.name.trim()) {
    toast.warning('项目名称不能为空')
    return
  }

  try {
    const res = await updateProject(projectId.value, {
      name: editForm.value.name.trim(),
      description: editForm.value.description,
      visibility: editForm.value.visibility,
      tagIds: editForm.value.tagIds
    })
    
    if (res.code === 200) {
      toast.success('项目信息更新成功！')
      isEditing.value = false
      // 重新加载项目详情
      await loadProjectDetail()
    }
  } catch (error) {
    logError('更新项目失败:', error)
    toast.error(error.message || '更新失败，请稍后重试')
  }
}

// 删除项目
const confirmDelete = async () => {
  try {
    const res = await deleteProject(projectId.value)
    
    if (res.code === 200) {
      toast.success('项目删除成功！')
      showDeleteConfirm.value = false
      // 跳转到项目广场
      setTimeout(() => {
        router.push('/square')
      }, 1000)
    }
  } catch (error) {
    logError('删除项目失败:', error)
    toast.error(error.message || '删除失败，请稍后重试')
  }
}

// 切换标签选择
const toggleTag = (tagId) => {
  const index = editForm.value.tagIds.indexOf(tagId)
  if (index > -1) {
    editForm.value.tagIds.splice(index, 1)
  } else {
    editForm.value.tagIds.push(tagId)
  }
}

onMounted(() => {
  loadProjectDetail()
  loadTags()
})
</script>

<template>
  <main class="app-main">
    <div class="project-detail-container">
      <!-- 项目头部 -->
      <div v-if="project" class="project-header">
        <div class="project-title-section">
          <h1 class="project-title">{{ project.name }}</h1>
          <span class="project-status">Public</span>
        </div>

        <div class="project-actions-bar">
          <button 
            :class="['action-btn', { active: project.isLiked }]"
            @click="toggleLike"
          >
            {{ project.isLiked ? '❤️' : '🤍' }} {{ formatNumber(project.likes) }}
          </button>
          <button 
            :class="['action-btn', { active: project.isFavorited }]"
            @click="toggleFavorite"
          >
            {{ project.isFavorited ? '⭐' : '☆' }} {{ formatNumber(project.favorites) }}
          </button>
          <button class="action-btn primary" @click="downloadProjectHandler">
            下载代码
          </button>
        </div>
      </div>

      <!-- 项目信息 -->
      <div v-if="project" class="project-info-bar">
        <div class="info-item">
          <span class="info-label">作者</span>
          <span class="info-value">{{ project.author }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">创建时间</span>
          <span class="info-value">{{ project.createdAt }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">更新时间</span>
          <span class="info-value">{{ project.updatedAt }}</span>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="isLoading" class="loading-state">
        <span class="loading-spinner">⟳</span>
        <p>加载中...</p>
      </div>

      <!-- 标签页内容 -->
      <template v-else-if="project">
        <div class="tabs-container">
          <div class="tabs">
            <button 
              :class="['tab-btn', { active: activeTab === 'readme' }]"
              @click="switchTab('readme')"
            >
              📄 README
            </button>
            <button 
              :class="['tab-btn', { active: activeTab === 'code' }]"
              @click="switchTab('code')"
            >
              💻 代码
            </button>
            <button 
              :class="['tab-btn', { active: activeTab === 'issues' }]"
              @click="switchTab('issues')"
            >
              🐛 Issues
            </button>
            <button 
              :class="['tab-btn', { active: activeTab === 'comments' }]"
              @click="switchTab('comments')"
            >
              💬 评论 ({{ comments.length }})
            </button>
            <button 
              v-if="isOwner"
              :class="['tab-btn', { active: activeTab === 'settings' }]"
              @click="switchTab('settings')"
            >
              ⚙️ 设置
            </button>
          </div>
        </div>

        <!-- README 内容 -->
        <div v-if="activeTab === 'readme'" class="content-section readme-content">
          <pre class="readme-text">{{ project.readme }}</pre>
        </div>

        <!-- 代码内容 -->
        <div v-if="activeTab === 'code'" class="content-section code-content">
          <div class="file-tree">
            <h3>项目文件</h3>
            <ul class="file-list">
              <li>📁 src/</li>
              <li class="indent">📁 components/</li>
              <li class="indent">📁 views/</li>
              <li class="indent">📄 main.js</li>
              <li class="indent">📄 App.vue</li>
              <li>📄 package.json</li>
              <li>📄 README.md</li>
            </ul>
          </div>
        </div>

        <!-- Issues 内容 -->
        <div v-if="activeTab === 'issues'" class="content-section issues-content">
          <div class="empty-issues">
            <span class="empty-icon">🐛</span>
            <p>功能待更新</p>
            <button class="create-issue-btn">创建 Issue</button>
          </div>
        </div>

        <!-- 评论内容 -->
        <div v-if="activeTab === 'comments'" class="content-section comments-content">
          <!-- 评论输入框 -->
          <div class="comment-input-section">
            <textarea 
              v-model="newComment"
              placeholder="写下你的评论..."
              class="comment-textarea"
              rows="4"
            ></textarea>
            <button class="submit-comment-btn" @click="submitComment">
              提交评论
            </button>
          </div>

          <!-- 评论列表 -->
          <div class="comments-list">
            <div 
              v-for="comment in comments" 
              :key="comment.id"
              class="comment-item"
            >
              <div class="comment-header">
                <span class="comment-avatar">{{ comment.avatar }}</span>
                <span class="comment-user">{{ comment.user }}</span>
                <span class="comment-time">{{ comment.createdAt }}</span>
              </div>
              <p class="comment-content">{{ comment.content }}</p>
            </div>
          </div>
        </div>

        <!-- 设置内容 -->
        <div v-if="activeTab === 'settings' && isOwner" class="content-section settings-content">
          <h3 class="settings-title">项目设置</h3>
          
          <!-- 编辑表单 -->
          <div class="settings-form">
            <div class="form-group">
              <label class="form-label">项目名称</label>
              <input 
                v-model="editForm.name"
                type="text"
                class="form-input"
                placeholder="请输入项目名称"
                :disabled="!isEditing"
              />
            </div>

            <div class="form-group">
              <label class="form-label">项目描述</label>
              <textarea 
                v-model="editForm.description"
                class="form-textarea"
                rows="4"
                placeholder="请输入项目描述"
                :disabled="!isEditing"
              ></textarea>
            </div>

            <div class="form-group">
              <label class="form-label">可见性</label>
              <select 
                v-model="editForm.visibility"
                class="form-select"
                :disabled="!isEditing"
              >
                <option :value="1">公开</option>
                <option :value="0">私有</option>
              </select>
            </div>

            <div class="form-group" v-if="allTags.length > 0">
              <label class="form-label">标签</label>
              <div class="tags-selector">
                <span 
                  v-for="tag in allTags" 
                  :key="tag.id"
                  :class="['tag-option', { selected: editForm.tagIds.includes(tag.id) }]"
                  @click="isEditing && toggleTag(tag.id)"
                >
                  {{ tag.name }}
                </span>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="form-actions">
              <template v-if="!isEditing">
                <button class="btn btn-primary" @click="startEdit">
                  编辑项目
                </button>
              </template>
              <template v-else>
                <button class="btn btn-secondary" @click="cancelEdit">
                  取消
                </button>
                <button class="btn btn-primary" @click="saveChanges">
                  保存修改
                </button>
              </template>
            </div>
          </div>

          <!-- 删除项目区域 -->
          <div class="danger-zone">
            <h4 class="danger-title">危险区域</h4>
            <p class="danger-description">
              删除项目后无法恢复，请谨慎操作。
            </p>
            <button class="btn btn-danger" @click="showDeleteConfirm = true">
              删除项目
            </button>
          </div>

          <!-- 删除确认对话框 -->
          <div v-if="showDeleteConfirm" class="modal-overlay" @click="showDeleteConfirm = false">
            <div class="modal-content" @click.stop>
              <h3 class="modal-title">确认删除</h3>
              <p class="modal-description">
                您确定要删除项目 "{{ project.name }}" 吗？此操作无法撤销！
              </p>
              <div class="modal-actions">
                <button class="btn btn-secondary" @click="showDeleteConfirm = false">
                  取消
                </button>
                <button class="btn btn-danger" @click="confirmDelete">
                  确认删除
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <span class="empty-icon"></span>
        <p class="empty-text">项目不存在</p>
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

.project-detail-container {
  max-width: 1000px;
  margin: 0 auto;
  padding-bottom: 24px;
}

/* 项目头部 */
.project-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e0e0e0;
}

.project-title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.project-icon {
  font-size: 32px;
}

.project-title {
  font-size: 28px;
  font-weight: 600;
  color: #003366;
  margin: 0;
}

.project-status {
  padding: 4px 12px;
  background-color: #e8f5e9;
  color: #2e7d32;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.project-actions-bar {
  display: flex;
  gap: 12px;
}

.action-btn {
  padding: 8px 16px;
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  color: #666666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.action-btn:hover {
  border-color: #0059b3;
  color: #0059b3;
}

.action-btn.active {
  background-color: #0059b3;
  border-color: #0059b3;
  color: #ffffff;
}

.action-btn.primary {
  background-color: #0059b3;
  border-color: #0059b3;
  color: #ffffff;
}

.action-btn.primary:hover {
  background-color: #004080;
}

/* 项目信息栏 */
.project-info-bar {
  display: flex;
  gap: 24px;
  padding: 16px;
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  margin-bottom: 24px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #999999;
}

.info-value {
  font-size: 14px;
  color: #333333;
  display: flex;
  align-items: center;
  gap: 4px;
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

/* 标签页 */
.tabs-container {
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px 6px 0 0;
  margin-bottom: 0;
}

.tabs {
  display: flex;
  gap: 0;
}

.tab-btn {
  padding: 12px 20px;
  background-color: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  color: #666666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn:hover {
  background-color: #f5f5f5;
}

.tab-btn.active {
  color: #0059b3;
  border-bottom-color: #0059b3;
  font-weight: 500;
}

/* 内容区域 */
.content-section {
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 0 0 6px 6px;
  padding: 24px;
  margin-bottom: 24px;
}

.readme-content {
  border-radius: 0 0 6px 6px;
}

.readme-text {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif;
  font-size: 14px;
  line-height: 1.8;
  color: #333333;
  white-space: pre-wrap;
  margin: 0;
}

/* 文件树 */
.file-tree h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #333333;
}

.file-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.file-list li {
  padding: 8px 12px;
  font-size: 14px;
  color: #333333;
  border-bottom: 1px solid #f0f0f0;
}

.file-list li:last-child {
  border-bottom: none;
}

.file-list .indent {
  padding-left: 32px;
}

/* Issues */
.empty-issues {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-issues p {
  font-size: 16px;
  color: #999999;
  margin-bottom: 24px;
}

.create-issue-btn {
  padding: 10px 24px;
  background-color: #0059b3;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.create-issue-btn:hover {
  background-color: #004080;
}

/* 评论区域 */
.comment-input-section {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e0e0e0;
}

.comment-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  margin-bottom: 12px;
}

.comment-textarea:focus {
  outline: none;
  border-color: #0059b3;
  box-shadow: 0 0 0 3px rgba(0, 89, 179, 0.1);
}

.submit-comment-btn {
  padding: 10px 24px;
  background-color: #0059b3;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.submit-comment-btn:hover {
  background-color: #004080;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  padding: 16px;
  background-color: #f9f9f9;
  border-radius: 6px;
  border-left: 3px solid #0059b3;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.comment-avatar {
  font-size: 20px;
}

.comment-user {
  font-weight: 600;
  color: #333333;
}

.comment-time {
  font-size: 12px;
  color: #999999;
}

.comment-content {
  font-size: 14px;
  color: #666666;
  line-height: 1.6;
  margin: 0;
}

/* 设置页面 */
.settings-content {
  padding: 32px;
  min-height: 400px;
}

.settings-title {
  font-size: 20px;
  font-weight: 600;
  color: #333333;
  margin: 0 0 24px 0;
  padding-bottom: 16px;
  border-bottom: 2px solid #e0e0e0;
}

.settings-form {
  margin-bottom: 32px;
}

.form-group {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333333;
  margin-bottom: 8px;
}

.form-input,
.form-textarea,
.form-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  font-family: inherit;
  transition: all 0.2s;
}

.form-input:focus,
.form-textarea:focus,
.form-select:focus {
  outline: none;
  border-color: #0059b3;
  box-shadow: 0 0 0 3px rgba(0, 89, 179, 0.1);
}

.form-input:disabled,
.form-textarea:disabled,
.form-select:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
  opacity: 0.6;
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
}

.tags-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  min-height: 50px;
}

.tag-option {
  padding: 6px 12px;
  background-color: #f5f5f5;
  border: 1px solid #d9d9d9;
  border-radius: 16px;
  font-size: 13px;
  color: #666666;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.tag-option:hover {
  background-color: #e8f0fe;
  border-color: #0059b3;
  color: #0059b3;
}

.tag-option.selected {
  background-color: #0059b3;
  border-color: #0059b3;
  color: #ffffff;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e0e0e0;
}

.btn {
  padding: 10px 24px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background-color: #0059b3;
  color: #ffffff;
}

.btn-primary:hover {
  background-color: #004080;
}

.btn-secondary {
  background-color: #f5f5f5;
  color: #666666;
  border: 1px solid #d9d9d9;
}

.btn-secondary:hover {
  background-color: #e8e8e8;
}

.btn-danger {
  background-color: #dc3545;
  color: #ffffff;
}

.btn-danger:hover {
  background-color: #c82333;
}

/* 危险区域 */
.danger-zone {
  margin-top: 32px;
  padding: 24px;
  background-color: #fff5f5;
  border: 1px solid #ffcdd2;
  border-radius: 6px;
}

.danger-title {
  font-size: 16px;
  font-weight: 600;
  color: #dc3545;
  margin: 0 0 8px 0;
}

.danger-description {
  font-size: 14px;
  color: #666666;
  margin: 0 0 16px 0;
}

/* 模态对话框 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background-color: #ffffff;
  border-radius: 8px;
  padding: 24px;
  max-width: 500px;
  width: 90%;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: #333333;
  margin: 0 0 12px 0;
}

.modal-description {
  font-size: 14px;
  color: #666666;
  margin: 0 0 24px 0;
  line-height: 1.6;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
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

.empty-state .empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state .empty-text {
  font-size: 16px;
  color: #999999;
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .project-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .project-info-bar {
    flex-wrap: wrap;
    gap: 16px;
  }

  .tabs {
    overflow-x: auto;
  }
}
</style>
