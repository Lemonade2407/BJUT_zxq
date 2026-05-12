<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { createProject, uploadFiles, uploadProjectDocument, getProjectTypes } from '@/api/project'
import { getTagsByCategory } from '@/api/tag'
import { getActiveCourses } from '@/api/course'
import { useFileTree } from '@/composables/useFileTree'
import { toast } from '@/utils/toast'
import { log, error as logError } from '@/utils/logger'


const router = useRouter()

// 表单数据
const form = ref({
  name: '',
  description: '',
  projectType: 'OTHER', // 默认其他
  courseName: '',
  thesisType: '',
  tagIds: [],
  visibility: 1 // 默认公开
})

// 项目类型选项（从后端动态获取）
const projectTypeOptions = ref([])

// 课程列表（从数据库加载）
const courseList = ref([])

// 毕设类型选项
const thesisTypeOptions = [
  { value: 'UNDERGRADUATE', label: '本科生毕设' },
  { value: 'MASTER', label: '研究生毕设' },
  { value: 'DOCTOR', label: '博士生毕设' }
]

// 可见性选项
const visibilityOptions = [
  { value: 1, label: '公开' },
  { value: 0, label: '私有' }
]

// 标签列表（按分组）
const tagsByCategory = ref({
  '技术栈': [],
  '领域': []
})

// 每个分类显示的标签数量（分页）
const displayCount = ref({
  '技术栈': 10,
  '领域': 10
})

// 加载状态
const isLoading = ref(false)
const isSubmitting = ref(false)

// 文件上传相关
const selectedFiles = ref([])
const uploadedFiles = ref([])
const isUploading = ref(false)
const uploadProgress = ref(0)
const documentFile = ref(null)

// 可见性选项
const { organizedFiles, displayFiles, toggleFolder } = useFileTree(selectedFiles)

// 加载标签列表
const loadTags = async () => {
  try {
    // 并行请求两个分组的标签
    const [techRes, domainRes] = await Promise.all([
      getTagsByCategory('技术栈'),
      getTagsByCategory('领域')
    ])
    
    if (techRes.code === 200) {
      tagsByCategory.value['技术栈'] = techRes.data || []
    }
    if (domainRes.code === 200) {
      tagsByCategory.value['领域'] = domainRes.data || []
    }
    
    log('加载标签列表成功')
  } catch (error) {
    logError('加载标签列表失败:', error)
  }
}

// 加载课程列表
const loadCourses = async () => {
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

// 切换标签选择
const toggleTag = (tagId) => {
  const index = form.value.tagIds.indexOf(tagId)
  index > -1 ? form.value.tagIds.splice(index, 1) : form.value.tagIds.push(tagId)
}

// 查看更多标签
const showMoreTags = (category) => {
  const totalCount = tagsByCategory.value[category].length
  displayCount.value[category] = Math.min(
    displayCount.value[category] + TAGS_PER_PAGE, 
    totalCount
  )
}

// 获取当前分类显示的标签
const getDisplayedTags = (category) => {
  return tagsByCategory.value[category].slice(0, displayCount.value[category])
}

// 是否还有更多标签
const hasMoreTags = (category) => {
  return displayCount.value[category] < tagsByCategory.value[category].length
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
  
  // 验证项目类型
  if (!form.value.projectType) {
    toast.warning('请选择项目类型')
    return false
  }
  
  // 如果是课程设计，必须选择课程名称
  if (form.value.projectType === 'COURSE' && !form.value.courseName) {
    toast.warning('请选择课程名称')
    return false
  }
  
  // 如果是毕业设计，必须选择毕设类型
  if (form.value.projectType === 'THESIS' && !form.value.thesisType) {
    toast.warning('请选择毕设类型')
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

      // 1. 先创建项目（获取 ID 才能上传文件）
      const res = await createProject(form.value)

      if (res.code !== 200) {
        toast.error(res.message || '创建项目失败')
        return
      }

      const projectId = res.data.id
      log('项目已创建，ID:', projectId)

      // 2. 上传项目文档（使用专用文档接口）
      if (documentFile.value) {
        const docRes = await uploadProjectDocument(projectId, documentFile.value)
        if (docRes.code !== 200) {
          toast.warning('项目文档上传失败：' + (docRes.message || '未知错误'))
        }
      }

      // 3. 上传项目代码文件
      if (selectedFiles.value.length > 0) {
        const fileRes = await uploadFiles(projectId, selectedFiles.value)
        if (fileRes.code === 200) {
          uploadedFiles.value = fileRes.data
        } else {
          toast.warning('项目文件上传失败：' + (fileRes.message || '未知错误'))
        }
      }

      toast.success('项目创建成功！')
      router.push(`/project/${projectId}`)
    } catch (error) {
      logError('创建项目失败:', error)
      toast.error(error.message || '创建项目失败，请稍后重试')
    } finally {
      isSubmitting.value = false
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
  
  // 如果是文件夹上传，保留相对路径信息
  validFiles.forEach((file, index) => {
    // webkitRelativePath 包含文件夹路径，如 "folder/subfolder/file.txt"
    if (file.webkitRelativePath) {
      file.relativePath = file.webkitRelativePath
      if (index < 3) { // 只打印前3个文件的日志
        log(`文件 ${index + 1}:`, file.name, '路径:', file.webkitRelativePath)
      }
    } else {
      if (index < 3) {
        log(`文件 ${index + 1}:`, file.name, '(无路径信息)')
      }
    }
  })
  
  // 添加到待上传列表
  selectedFiles.value.push(...validFiles)
  log('已选择文件:', validFiles.length, '个')
}

// 拖拽处理
// 文档拖拽上传
const handleDocDrop = (event) => {
  const file = event.dataTransfer.files[0]
  if (file) { documentFile.value = file }
}

// 文档文件选择
const handleDocSelect = (event) => {
  const file = event.target.files[0]
  if (file) { documentFile.value = file }
}

const handleDragOver = (event) => {
  event.preventDefault()
  event.stopPropagation()
}

const handleDrop = (event) => {
  event.preventDefault()
  event.stopPropagation()
  
  const items = event.dataTransfer.items
  const files = []
  
  // 处理拖拽的文件夹
  if (items) {
    // 使用 DataTransferItemList API 处理文件夹
    const traverseFileTree = (item, path = '') => {
      return new Promise((resolve) => {
        if (item.isFile) {
          item.file((file) => {
            // 创建新对象，复制所有属性并添加路径
            const fileWithPath = new File([file], file.name, {
              type: file.type,
              lastModified: file.lastModified
            })
            // 添加自定义属性
            Object.defineProperty(fileWithPath, 'relativePath', {
              value: path + file.name,
              writable: false,
              enumerable: true,
              configurable: true
            })
            files.push(fileWithPath)
            resolve()
          })
        } else if (item.isDirectory) {
          const dirReader = item.createReader()
          dirReader.readEntries(async (entries) => {
            for (const entry of entries) {
              await traverseFileTree(entry, path + item.name + '/')
            }
            resolve()
          })
        } else {
          resolve()
        }
      })
    }
    
    // 遍历所有拖拽项
    const promises = Array.from(items).map(item => {
      const entry = item.webkitGetAsEntry()
      if (entry) {
        return traverseFileTree(entry)
      }
      return Promise.resolve()
    })
    
    Promise.all(promises).then(() => {
      processFiles(files)
    })
  } else {
    // 降级处理：直接使用 files
    const fileList = Array.from(event.dataTransfer.files)
    fileList.forEach(file => {
      if (file.webkitRelativePath) {
        file.relativePath = file.webkitRelativePath
      }
    })
    processFiles(fileList)
  }
}

// 处理文件（验证和添加）
const processFiles = (files) => {
  if (files.length === 0) return
  
  log('拖拽的文件数量:', files.length)
  
  // 验证文件大小
  const maxSize = 50 * 1024 * 1024
  const validFiles = files.filter(file => file.size <= maxSize)
  
  if (validFiles.length < files.length) {
    toast.warning('部分文件超过 50MB，已自动过滤')
  }
  
  // 打印前3个文件的路径信息
  validFiles.forEach((file, index) => {
    if (index < 3) {
      if (file.relativePath || file.webkitRelativePath) {
        log(`文件 ${index + 1}:`, file.name, '路径:', file.relativePath || file.webkitRelativePath)
      } else {
        log(`文件 ${index + 1}:`, file.name, '(无路径信息)')
      }
    }
  })
  
  selectedFiles.value.push(...validFiles)
  log('拖拽添加文件:', validFiles.length, '个')
}

// 移除文件
const removeFile = (index) => {
  selectedFiles.value.splice(index, 1)
  log('移除文件，剩余:', selectedFiles.value.length, '个')
}

// 按路径移除文件
const removeFileByPath = (path) => {
  const index = selectedFiles.value.findIndex(file => {
    const filePath = file.relativePath || file.webkitRelativePath || file.name
    return filePath === path
  })
  if (index > -1) {
    selectedFiles.value.splice(index, 1)
    log('移除文件，剩余:', selectedFiles.value.length, '个')
  }
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

// 加载项目类型列表
const loadProjectTypes = async () => {
  try {
    log('加载项目类型列表...')
    const res = await getProjectTypes()
    
    if (res.code === 200 && res.data) {
      // 转换为前端需要的格式
      projectTypeOptions.value = res.data.map(type => ({
        value: type.typeCode,
        label: type.typeName
      }))
      log(`加载完成，项目类型数量: ${projectTypeOptions.value.length}`)
    } else {
      logError('项目类型 API 返回数据异常:', res)
      // 降级：使用默认值
      projectTypeOptions.value = [
        { value: 'COURSE', label: '课程设计' },
        { value: 'THESIS', label: '毕业设计' },
        { value: 'COMPETITION', label: '竞赛作品' },
        { value: 'PERSONAL', label: '个人项目' },
        { value: 'OTHER', label: '其他' }
      ]
    }
  } catch (error) {
    logError('加载项目类型列表失败:', error)
    // 降级：使用默认值
    projectTypeOptions.value = [
      { value: 'COURSE', label: '课程设计' },
      { value: 'THESIS', label: '毕业设计' },
      { value: 'COMPETITION', label: '竞赛作品' },
      { value: 'PERSONAL', label: '个人项目' },
      { value: 'OTHER', label: '其他' }
    ]
  }
}

// 组件挂载时加载数据
onMounted(() => {
  isLoading.value = true
  Promise.all([
    loadProjectTypes(),
    loadTags(),
    loadCourses()
  ]).finally(() => {
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
        <span class="loading-spinner">⏳</span>
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

          <!-- 项目类型 -->
          <div class="form-group">
            <label class="form-label">
              项目类型 <span class="required">*</span>
            </label>
            <div class="project-type-grid">
              <div
                v-for="option in projectTypeOptions"
                :key="option.value"
                :class="['type-option', { selected: form.projectType === option.value }]"
                @click="form.projectType = option.value"
              >
                <span class="type-label">{{ option.label }}</span>
              </div>
            </div>
          </div>

          <!-- 课程名称（仅当选择课程设计时显示） -->
          <div v-if="form.projectType === 'COURSE'" class="form-group">
            <label for="courseName" class="form-label">
              课程名称 <span class="required">*</span>
            </label>
            <select
              id="courseName"
              v-model="form.courseName"
              class="form-select"
            >
              <option value="">请选择课程</option>
              <option v-for="course in courseList" :key="course" :value="course">
                {{ course }}
              </option>
            </select>
          </div>

          <!-- 毕设类型（仅当选择毕业设计时显示） -->
          <div v-if="form.projectType === 'THESIS'" class="form-group">
            <label class="form-label">
              毕设类型 <span class="required">*</span>
            </label>
            <div class="thesis-type-options">
              <div
                v-for="option in thesisTypeOptions"
                :key="option.value"
                :class="['thesis-option', { selected: form.thesisType === option.value }]"
                @click="form.thesisType = option.value"
              >
                {{ option.label }}
              </div>
            </div>
          </div>

          <!-- 项目标签 -->
          <div class="form-group">
            <label class="form-label">
              项目标签 <span class="optional">(可选)</span>
            </label>
            
            <!-- 技术栈标签 -->
            <div v-if="tagsByCategory['技术栈'].length > 0" class="tag-category">
              <h4 class="category-title">🔧 技术栈</h4>
              <div class="tags-grid">
                <div
                  v-for="tag in getDisplayedTags('技术栈')"
                  :key="tag.id"
                  :class="['tag-item', { selected: form.tagIds.includes(tag.id) }]"
                  @click="toggleTag(tag.id)"
                >
                  {{ tag.name }}
                </div>
              </div>
              <button
                v-if="hasMoreTags('技术栈')"
                type="button"
                class="show-more-btn"
                @click="showMoreTags('技术栈')"
              >
                查看更多 ({{ tagsByCategory['技术栈'].length - displayCount['技术栈'] }})
              </button>
            </div>
            
            <!-- 领域标签 -->
            <div v-if="tagsByCategory['领域'].length > 0" class="tag-category">
              <h4 class="category-title">🎯 领域</h4>
              <div class="tags-grid">
                <div
                  v-for="tag in getDisplayedTags('领域')"
                  :key="tag.id"
                  :class="['tag-item', { selected: form.tagIds.includes(tag.id) }]"
                  @click="toggleTag(tag.id)"
                >
                  {{ tag.name }}
                </div>
              </div>
              <button
                v-if="hasMoreTags('领域')"
                type="button"
                class="show-more-btn"
                @click="showMoreTags('领域')"
              >
                查看更多 ({{ tagsByCategory['领域'].length - displayCount['领域'] }})
              </button>
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
                  <span class="option-label">{{ option.label }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 项目文档上传 -->
          <div class="form-group">
            <label class="form-label">
              项目文档 <span class="optional">(可选)</span>
            </label>
            <div
              class="upload-area document-upload"
              :class="{ 'has-file': documentFile }"
              @dragover.prevent
              @drop.prevent="handleDocDrop"
              @click="$refs.docInput.click()"
            >
              <input
                ref="docInput"
                type="file"
                accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt,.md,.zip,.rar,.7z"
                style="display: none"
                @change="handleDocSelect"
              />
              <template v-if="!documentFile">
                <div class="upload-icon">📄</div>
                <p class="upload-text">上传项目文档（说明文档、答辩PPT等）</p>
                <p class="upload-hint">支持 PDF、Word、PPT、Excel、Markdown 等格式，单个文件不超过 100MB</p>
              </template>
              <template v-else>
                <div class="doc-file-info">
                  <span class="file-icon">📄</span>
                  <div class="file-details">
                    <span class="file-name">{{ documentFile.name }}</span>
                    <span class="file-size">{{ formatFileSize(documentFile.size) }}</span>
                  </div>
                  <button type="button" class="remove-btn" @click.stop="documentFile = null">✕</button>
                </div>
              </template>
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
                webkitdirectory
                mozdirectory
                directory
                style="display: none"
                @change="handleFileSelect"
              />
              <div class="upload-icon">📁</div>
              <p class="upload-text">点击选择文件或文件夹，或直接拖拽到此处</p>
              <p class="upload-hint">支持批量上传文件和整个文件夹，单个文件不超过 100MB（大文件自动分片上传）</p>
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
                v-for="(item, index) in displayFiles"
                :key="index"
                :class="['file-item', { 'folder-item': item.isFolder }]"
                :style="{ paddingLeft: (item.level * 20 + 12) + 'px' }"
              >
                <div class="file-info">
                  <span class="file-icon">
                    {{ item.isFolder ? '📁' : '📄' }}
                  </span>
                  <div class="file-details">
                    <span class="file-name">{{ item.name }}</span>
                    <span v-if="!item.isFolder" class="file-size">{{ formatFileSize(item.size) }}</span>
                  </div>
                </div>
                <button
                  v-if="!item.isFolder"
                  type="button"
                  class="remove-btn"
                  @click="removeFileByPath(item.relativePath || item.webkitRelativePath || item.name)"
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
  color: #064e3b;
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
  color: #10b981;
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
  border-radius: 8px;
  padding: 32px;
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
  border-color: #10b981;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.1);
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

/* 标签分组 */
.tag-category {
  margin-bottom: 24px;
}

.category-title {
  font-size: 16px;
  font-weight: 600;
  color: #064e3b;
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 2px solid #d1fae5;
}

/* 查看更多按钮 */
.show-more-btn {
  margin-top: 12px;
  padding: 8px 20px;
  background-color: transparent;
  border: 1px dashed #10b981;
  border-radius: 6px;
  font-size: 13px;
  color: #10b981;
  cursor: pointer;
  transition: all 0.2s;
  width: 100%;
}

.show-more-btn:hover {
  background-color: rgba(16, 185, 129, 0.05);
  border-style: solid;
}

.tag-item {
  padding: 8px 16px;
  background-color: rgba(16, 185, 129, 0.08);
  border: 1px solid transparent;
  border-radius: 6px;
  font-size: 13px;
  color: #059669;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}

.tag-item:hover {
  background-color: rgba(16, 185, 129, 0.15);
}

.tag-item.selected {
  background-color: #10b981;
  color: #ffffff;
  border-color: #10b981;
  font-weight: 600;
}

/* 项目类型选择 */
.project-type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.type-option {
  padding: 16px;
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.type-option:hover {
  border-color: #10b981;
  background-color: #f9f9f9;
}

.type-option.selected {
  border-color: #10b981;
  background-color: rgba(16, 185, 129, 0.05);
}

.type-label {
  font-size: 14px;
  font-weight: 600;
  color: #333333;
}

.type-option.selected .type-label {
  color: #059669;
}

/* 课程名称下拉框 */
.form-select {
  width: 100%;
  padding: 10px 12px;
  border: 2px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  color: #333333;
  background-color: #ffffff;
  cursor: pointer;
  transition: all 0.2s;
  outline: none;
}

.form-select:focus {
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

/* 毕设类型选项 */
.thesis-type-options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}

.thesis-option {
  padding: 12px 20px;
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  color: #333333;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
  font-weight: 500;
}

.thesis-option:hover {
  border-color: #10b981;
  background-color: #f9f9f9;
}

.thesis-option.selected {
  background-color: #10b981;
  color: #ffffff;
  border-color: #10b981;
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
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.visibility-option:hover {
  border-color: #10b981;
  background-color: #f9f9f9;
}

.visibility-option.selected {
  border-color: #10b981;
  background-color: rgba(16, 185, 129, 0.05);
}

.option-header {
  margin-bottom: 8px;
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
  border-color: #10b981;
  background-color: rgba(16, 185, 129, 0.02);
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

.upload-area.has-file {
  padding: 16px 20px;
  border-color: #10b981;
  background-color: rgba(16, 185, 129, 0.04);
}

.doc-file-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.doc-file-info .file-icon {
  font-size: 32px;
}

.doc-file-info .file-details {
  flex: 1;
  text-align: left;
}

.doc-file-info .file-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  display: block;
}

.doc-file-info .file-size {
  font-size: 12px;
  color: #999;
}

.doc-file-info .remove-btn {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  font-size: 18px;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.doc-file-info .remove-btn:hover {
  color: #d93025;
  background: rgba(217, 48, 37, 0.08);
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

/* 文件夹项样式 */
.folder-item {
  background-color: #fafafa;
  font-weight: 500;
}

.folder-item .file-name {
  color: #059669;
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
  background-color: #ecfdf5;
  border: 1px solid #a7f3d0;
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
  background: #10b981;
  transition: width 0.3s ease;
  border-radius: 4px;
}

.progress-text {
  font-size: 13px;
  color: #059669;
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
  background-color: #10b981;
  color: #ffffff;
}

.btn-submit:hover:not(:disabled) {
  background-color: #059669;
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
