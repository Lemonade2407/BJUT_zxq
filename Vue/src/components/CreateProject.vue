<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { createProject, uploadFiles } from '@/api/project'
import { getTags } from '@/api/tag'
import { toast } from '@/utils/toast'
import { log, error as logError } from '@/utils/logger'

const router = useRouter()

// 表单数据
const form = ref({
  name: '',
  description: '',
  tagIds: [],
  visibility: 1 // 默认公开
})

// 标签列表
const tags = ref([])

// 加载状态
const isLoading = ref(false)
const isSubmitting = ref(false)

// 文件上传相关
const selectedFiles = ref([])
const uploadedFiles = ref([])
const isUploading = ref(false)
const uploadProgress = ref(0)

// 可见性选项
const visibilityOptions = [
  { value: 1, label: '公开', description: '所有人都可以查看和访问' },
  { value: 0, label: '私有', description: '只有你可以查看和访问' }
]

// 加载标签列表
const loadTags = async () => {
  try {
    const res = await getTags()
    if (res.code === 200 && res.data) {
      tags.value = res.data
      log('加载标签列表成功，数量:', tags.value.length)
    }
  } catch (error) {
    logError('加载标签列表失败:', error)
  }
}

// 切换标签选择
const toggleTag = (tagId) => {
  const index = form.value.tagIds.indexOf(tagId)
  if (index > -1) {
    form.value.tagIds.splice(index, 1)
  } else {
    form.value.tagIds.push(tagId)
  }
}

// 验证表单
const validateForm = () => {
  if (!form.value.name || form.value.name.trim() === '') {
    toast.warning('请输入项目名称')
    return false
  }
  
  if (form.value.name.length < 2 || form.value.name.length > 50) {
    toast.warning('项目名称长度应在 2-50 个字符之间')
    return false
  }
  
  if (!form.value.description || form.value.description.trim() === '') {
    toast.warning('请输入项目描述')
    return false
  }
  
  if (form.value.description.length < 10 || form.value.description.length > 500) {
    toast.warning('项目描述长度应在 10-500 个字符之间')
    return false
  }
  
  return true
}

// 提交表单
const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }
  
  isSubmitting.value = true
  
  try {
    log('开始创建项目:', form.value)
    
    // 1. 创建项目
    const res = await createProject(form.value)
    
    if (res.code === 200) {
      toast.success('项目创建成功！')
      log('项目创建成功，ID:', res.data.id)
      
      const projectId = res.data.id
      
      // 2. 如果有待上传的文件，则上传文件
      if (selectedFiles.value.length > 0) {
        await uploadProjectFiles(projectId)
      } else {
        // 没有文件，直接跳转
        setTimeout(() => {
          router.push(`/project/${projectId}`)
        }, 1000)
      }
    } else {
      toast.error(res.message || '创建项目失败')
    }
  } catch (error) {
    logError('创建项目失败:', error)
    toast.error(error.message || '创建项目失败，请稍后重试')
  } finally {
    isSubmitting.value = false
  }
}

// 上传项目文件
const uploadProjectFiles = async (projectId) => {
  isUploading.value = true
  uploadProgress.value = 0
  
  try {
    log('开始上传文件，数量:', selectedFiles.value.length)
    
    // 使用批量上传接口
    const res = await uploadFiles(projectId, selectedFiles.value)
    
    if (res.code === 200) {
      uploadedFiles.value = res.data
      uploadProgress.value = 100
      toast.success(`成功上传 ${uploadedFiles.value.length} 个文件`)
      log('文件上传成功')
      
      // 延迟跳转，让用户看到上传成功的提示
      setTimeout(() => {
        router.push(`/project/${projectId}`)
      }, 1500)
    } else {
      toast.warning('项目创建成功，但文件上传失败：' + (res.message || '未知错误'))
      // 即使文件上传失败，也跳转到项目页
      setTimeout(() => {
        router.push(`/project/${projectId}`)
      }, 2000)
    }
  } catch (error) {
    logError('文件上传失败:', error)
    toast.warning('项目创建成功，但文件上传失败')
    // 即使文件上传失败，也跳转到项目页
    setTimeout(() => {
      router.push(`/project/${projectId}`)
    }, 2000)
  } finally {
    isUploading.value = false
  }
}

// 取消操作
const handleCancel = () => {
  router.back()
}

// 处理文件选择
const handleFileSelect = (event) => {
  const files = Array.from(event.target.files)
  if (files.length === 0) return
  
  // 验证文件大小（单个文件不超过 50MB）
  const maxSize = 50 * 1024 * 1024
  const oversizedFiles = files.filter(file => file.size > maxSize)
  
  if (oversizedFiles.length > 0) {
    toast.warning(`以下文件超过 50MB，无法上传：${oversizedFiles.map(f => f.name).join(', ')}`)
  }
  
  // 过滤出符合要求的文件
  const validFiles = files.filter(file => file.size <= maxSize)
  
  // 添加到待上传列表
  selectedFiles.value.push(...validFiles)
  log('已选择文件:', validFiles.length, '个')
}

// 拖拽处理
const handleDragOver = (event) => {
  event.preventDefault()
  event.stopPropagation()
}

const handleDrop = (event) => {
  event.preventDefault()
  event.stopPropagation()
  
  const files = Array.from(event.dataTransfer.files)
  if (files.length === 0) return
  
  // 验证文件大小
  const maxSize = 50 * 1024 * 1024
  const validFiles = files.filter(file => file.size <= maxSize)
  
  if (validFiles.length < files.length) {
    toast.warning('部分文件超过 50MB，已自动过滤')
  }
  
  selectedFiles.value.push(...validFiles)
  log('拖拽添加文件:', validFiles.length, '个')
}

// 移除文件
const removeFile = (index) => {
  selectedFiles.value.splice(index, 1)
  log('移除文件，剩余:', selectedFiles.value.length, '个')
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

// 组件挂载时加载数据
onMounted(() => {
  isLoading.value = true
  loadTags().finally(() => {
    isLoading.value = false
  })
})
</script>

<template>
  <main class="app-main">
    <div class="create-project-container">
      <!-- 页面头部 -->
      <div class="page-header">
        <h1 class="page-title">➕ 创建新项目</h1>
        <p class="page-description">填写项目信息，开始你的创作之旅</p>
      </div>

      <!-- 加载状态 -->
      <div v-if="isLoading" class="loading-state">
        <span class="loading-spinner">⟳</span>
        <p>加载中...</p>
      </div>

      <!-- 表单内容 -->
      <div v-else class="form-container">
        <form @submit.prevent="handleSubmit" class="project-form">
          <!-- 项目名称 -->
          <div class="form-group">
            <label for="name" class="form-label">
              项目名称 <span class="required">*</span>
            </label>
            <input
              id="name"
              v-model="form.name"
              type="text"
              class="form-input"
              placeholder="请输入项目名称（2-50个字符）"
              maxlength="50"
            />
            <div class="char-count">{{ form.name.length }}/50</div>
          </div>

          <!-- 项目描述 -->
          <div class="form-group">
            <label for="description" class="form-label">
              项目描述 <span class="required">*</span>
            </label>
            <textarea
              id="description"
              v-model="form.description"
              class="form-textarea"
              placeholder="请详细描述你的项目（10-500个字符）"
              rows="6"
              maxlength="500"
            ></textarea>
            <div class="char-count">{{ form.description.length }}/500</div>
          </div>

          <!-- 项目标签 -->
          <div class="form-group">
            <label class="form-label">
              项目标签 <span class="optional">(可选)</span>
            </label>
            <div class="tags-grid">
              <div
                v-for="tag in tags"
                :key="tag.id"
                :class="['tag-item', { selected: form.tagIds.includes(tag.id) }]"
                @click="toggleTag(tag.id)"
              >
                {{ tag.name }}
              </div>
            </div>
            <p class="form-hint">点击标签进行选择，最多可选择多个标签</p>
          </div>

          <!-- 可见性设置 -->
          <div class="form-group">
            <label class="form-label">
              可见性 <span class="required">*</span>
            </label>
            <div class="visibility-options">
              <div
                v-for="option in visibilityOptions"
                :key="option.value"
                :class="['visibility-option', { selected: form.visibility === option.value }]"
                @click="form.visibility = option.value"
              >
                <div class="option-header">
                  <span class="option-icon">{{ option.value === 1 ? '🌍' : '🔒' }}</span>
                  <span class="option-label">{{ option.label }}</span>
                </div>
                <p class="option-description">{{ option.description }}</p>
              </div>
            </div>
          </div>

          <!-- 文件上传 -->
          <div class="form-group">
            <label class="form-label">
              项目文件 <span class="optional">(可选)</span>
            </label>
            
            <!-- 拖拽上传区域 -->
            <div
              class="upload-area"
              @dragover="handleDragOver"
              @drop="handleDrop"
              @click="$refs.fileInput.click()"
            >
              <input
                ref="fileInput"
                type="file"
                multiple
                style="display: none"
                @change="handleFileSelect"
              />
              <div class="upload-icon">📁</div>
              <p class="upload-text">点击选择文件或拖拽文件到此处</p>
              <p class="upload-hint">支持批量上传，单个文件不超过 50MB</p>
            </div>

            <!-- 文件列表 -->
            <div v-if="selectedFiles.length > 0" class="file-list">
              <div class="file-list-header">
                <span>已选择 {{ selectedFiles.length }} 个文件</span>
                <button
                  type="button"
                  class="clear-btn"
                  @click="selectedFiles = []"
                >
                  清空
                </button>
              </div>
              <div
                v-for="(file, index) in selectedFiles"
                :key="index"
                class="file-item"
              >
                <div class="file-info">
                  <span class="file-icon">📄</span>
                  <div class="file-details">
                    <span class="file-name">{{ file.name }}</span>
                    <span class="file-size">{{ formatFileSize(file.size) }}</span>
                  </div>
                </div>
                <button
                  type="button"
                  class="remove-btn"
                  @click="removeFile(index)"
                >
                  ✕
                </button>
              </div>
            </div>

            <!-- 上传进度 -->
            <div v-if="isUploading" class="upload-progress">
              <div class="progress-bar">
                <div
                  class="progress-fill"
                  :style="{ width: uploadProgress + '%' }"
                ></div>
              </div>
              <span class="progress-text">上传中... {{ uploadProgress }}%</span>
            </div>
          </div>

          <!-- 按钮组 -->
          <div class="form-actions">
            <button
              type="button"
              class="btn btn-cancel"
              @click="handleCancel"
              :disabled="isSubmitting || isUploading"
            >
              取消
            </button>
            <button
              type="submit"
              class="btn btn-submit"
              :disabled="isSubmitting || isUploading"
            >
              {{ isSubmitting ? '创建中...' : (isUploading ? '上传文件中...' : '创建项目') }}
            </button>
          </div>
        </form>
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

.create-project-container {
  max-width: 800px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  margin-bottom: 32px;
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

/* 表单容器 */
.form-container {
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 32px;
  box-shadow: 0 2px 4px rgba(0, 51, 102, 0.05);
}

.project-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 表单组 */
.form-group {
  position: relative;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #333333;
  margin-bottom: 8px;
}

.required {
  color: #ff4d4f;
  margin-left: 2px;
}

.optional {
  color: #999999;
  font-weight: 400;
  font-size: 12px;
}

/* 输入框 */
.form-input,
.form-textarea,
.form-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  color: #333333;
  transition: all 0.2s;
  background-color: #ffffff;
}

.form-input:focus,
.form-textarea:focus,
.form-select:focus {
  outline: none;
  border-color: #0059b3;
  box-shadow: 0 0 0 2px rgba(0, 89, 179, 0.1);
}

.form-textarea {
  resize: vertical;
  font-family: inherit;
  line-height: 1.6;
}

/* 字符计数 */
.char-count {
  position: absolute;
  right: 12px;
  bottom: 12px;
  font-size: 12px;
  color: #999999;
}

/* 提示文本 */
.form-hint {
  font-size: 12px;
  color: #999999;
  margin-top: 8px;
  margin-bottom: 0;
}

/* 标签网格 */
.tags-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}

.tag-item {
  padding: 8px 16px;
  background-color: rgba(0, 89, 179, 0.08);
  border: 2px solid transparent;
  border-radius: 6px;
  font-size: 13px;
  color: #0059b3;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}

.tag-item:hover {
  background-color: rgba(0, 89, 179, 0.15);
  transform: translateY(-1px);
}

.tag-item.selected {
  background-color: #0059b3;
  color: #ffffff;
  border-color: #0059b3;
  font-weight: 600;
}

/* 可见性选项 */
.visibility-options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.visibility-option {
  padding: 16px;
  border: 2px solid #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.visibility-option:hover {
  border-color: #0059b3;
  background-color: rgba(0, 89, 179, 0.02);
}

.visibility-option.selected {
  border-color: #0059b3;
  background-color: rgba(0, 89, 179, 0.05);
}

.option-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.option-icon {
  font-size: 20px;
}

.option-label {
  font-size: 14px;
  font-weight: 600;
  color: #333333;
}

.option-description {
  font-size: 12px;
  color: #666666;
  margin: 0;
  line-height: 1.5;
}

/* 文件上传区域 */
.upload-area {
  border: 2px dashed #d9d9d9;
  border-radius: 6px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  background-color: #fafafa;
}

.upload-area:hover {
  border-color: #0059b3;
  background-color: rgba(0, 89, 179, 0.02);
}

.upload-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.upload-text {
  font-size: 14px;
  color: #333333;
  margin: 0 0 8px 0;
  font-weight: 500;
}

.upload-hint {
  font-size: 12px;
  color: #999999;
  margin: 0;
}

/* 文件列表 */
.file-list {
  margin-top: 16px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  max-height: 300px;
  overflow-y: auto;
}

.file-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: #f5f5f5;
  border-bottom: 1px solid #e8e8e8;
  font-size: 14px;
  font-weight: 600;
  color: #333333;
}

.clear-btn {
  padding: 4px 12px;
  background-color: transparent;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 12px;
  color: #666666;
  cursor: pointer;
  transition: all 0.2s;
}

.clear-btn:hover {
  background-color: #ff4d4f;
  border-color: #ff4d4f;
  color: #ffffff;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s;
}

.file-item:last-child {
  border-bottom: none;
}

.file-item:hover {
  background-color: #f9f9f9;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.file-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.file-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  color: #333333;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 12px;
  color: #999999;
}

.remove-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: transparent;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  color: #999999;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.remove-btn:hover {
  background-color: #ff4d4f;
  border-color: #ff4d4f;
  color: #ffffff;
}

/* 上传进度 */
.upload-progress {
  margin-top: 16px;
  padding: 16px;
  background-color: #f0f9ff;
  border: 1px solid #bae7ff;
  border-radius: 6px;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background-color: #e8e8e8;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #0059b3, #0077cc);
  transition: width 0.3s ease;
  border-radius: 4px;
}

.progress-text {
  font-size: 13px;
  color: #0059b3;
  font-weight: 500;
}

/* 按钮组 */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
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

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-cancel {
  background-color: #f5f5f5;
  color: #666666;
  border: 1px solid #d9d9d9;
}

.btn-cancel:hover:not(:disabled) {
  background-color: #e8e8e8;
  border-color: #999999;
}

.btn-submit {
  background-color: #0059b3;
  color: #ffffff;
}

.btn-submit:hover:not(:disabled) {
  background-color: #004080;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 89, 179, 0.3);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-title {
    font-size: 24px;
  }

  .form-container {
    padding: 24px;
  }

  .tags-grid {
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  }

  .visibility-options {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column-reverse;
  }

  .btn {
    width: 100%;
  }
}
</style>
