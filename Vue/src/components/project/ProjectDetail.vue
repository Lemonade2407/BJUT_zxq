<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProjectDetail, starProject, unstarProject, watchProject, unwatchProject, downloadProject, updateProject, deleteProject, getAllProjectFiles, deleteAllProjectFiles, getUploadSignatures, uploadToPresignedUrl, confirmUpload, uploadProjectDocument, deleteProjectDocument, getProjectDocument, getProjectTypes } from '@/api/project'
import { getTagsByCategory } from '@/api/tag'
import { getActiveCourses } from '@/api/course'
import { toast } from '@/utils/toast'
import { error as logError, warn } from '@/utils/logger'
import tokenManager from '@/utils/tokenManager'
import FileTreeItem from '../layout/FileTreeItem.vue'
import { formatNumber, formatDateShort } from '@/utils/helpers'
import { useFileTree } from '@/composables/useFileTree'
import ProjectCommentsTab from './ProjectCommentsTab.vue'


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
// 标签页
const activeTab = ref('readme') // readme, code, comments, settings

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

// 课程列表（从数据库加载）
const courseList = ref([])

// 项目类型选项（从后端动态获取）
const projectTypeOptions = ref([])

// 毕设类型选项
const thesisTypeOptions = [
  { value: 'UNDERGRADUATE', label: '本科生毕设' },
  { value: 'MASTER', label: '研究生毕设' },
  { value: 'DOCTOR', label: '博士生毕设' }
]

// 编辑表单
const editForm = ref({
  name: '',
  description: '',
  visibility: 1,
  projectType: 'OTHER', // 默认其他
  courseName: '',
  thesisType: '',
  tagIds: []
})
const isEditing = ref(false)
const showDeleteConfirm = ref(false)

// 文件上传相关
const selectedFiles = ref([])
const uploadedFiles = ref([])
const isUploading = ref(false)
const uploadProgress = ref(0)
const isOverwriteMode = ref(true) // 默认开启覆盖模式

// 项目文件列表（从 OSS 获取）
const projectFiles = ref([])
const isLoadingFiles = ref(false)

// 项目文档相关
const documentUrl = ref('')
const documentName = ref('') // 原始文件名
const isUploadingDocument = ref(false)
const documentUploadProgress = ref(0)
const selectedDocumentFile = ref(null)
const documentContent = ref('') // 渲染后的 HTML 内容
const isRenderingDocument = ref(false) // 是否正在渲染文档

// 加载项目详情
const loadProjectDetail = async () => {
  if (!projectId.value) {
    warn('项目 ID 为空')
    return
  }

  isLoading.value = true
  try {
    const res = await getProjectDetail(projectId.value)
    if (res.code === 200 && res.data) {
      project.value = res.data
      if (project.value.createdAt) project.value.createdAt = formatDateShort(project.value.createdAt)
      if (project.value.updatedAt) project.value.updatedAt = formatDateShort(project.value.updatedAt)
      if (isOwner.value) initEditForm()

      // 并行加载文件和文档（不阻塞页面渲染）
      Promise.all([loadProjectFiles(), loadProjectDocument()])
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

// 加载项目文件列表
const loadProjectFiles = async () => {
  isLoadingFiles.value = true
  try {
    // 使用新的 API 获取所有文件（用于构建完整的树形结构）
    const res = await getAllProjectFiles(projectId.value)
    if (res.code === 200 && res.data) {
      projectFiles.value = res.data
    }
  } catch (error) {
    logError('加载项目文件失败:', error)
    // 文件加载失败不影响主流程
  } finally {
    isLoadingFiles.value = false
  }
}

// 加载项目文档
const loadProjectDocument = async () => {
  try {
    const res = await getProjectDocument(projectId.value)
    if (res.code === 200 && res.data) {
      documentUrl.value = res.data
      // 从 URL 提取文件名作为显示名称
      const urlParts = res.data.split('/')
      documentName.value = urlParts[urlParts.length - 1] || '未知文档'
      
      // 自动渲染 Word 或 Markdown 文档
      if (isWordDocument()) {
        await renderWordDocument()
      } else if (isMarkdownDocument()) {
        await renderMarkdownDocument()
      }
    }
  } catch (error) {
    logError('加载项目文档失败:', error)
    // 文档加载失败不影响主流程
  }
}

// 选择文档文件
const handleDocumentFileSelect = (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  // 验证文件类型
  const allowedTypes = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'text/plain',
    'text/markdown',
    'application/vnd.ms-powerpoint',
    'application/vnd.openxmlformats-officedocument.presentationml.presentation'
  ]
  
  const fileExtension = file.name.split('.').pop().toLowerCase()
  const allowedExtensions = ['pdf', 'doc', 'docx', 'txt', 'md', 'ppt', 'pptx']
  
  if (!allowedExtensions.includes(fileExtension)) {
    toast.error('不支持的文件类型，仅支持 PDF、Word、TXT、Markdown、PPT 格式')
    event.target.value = '' // 清空选择
    return
  }
  
  selectedDocumentFile.value = file
}

// 上传项目文档
const uploadDocument = async () => {
  if (!selectedDocumentFile.value) {
    toast.warning('请先选择文档文件')
    return
  }
  
  isUploadingDocument.value = true
  documentUploadProgress.value = 0
  
  try {
    const res = await uploadProjectDocument(
      projectId.value,
      selectedDocumentFile.value,
      (progress) => {
        documentUploadProgress.value = progress
      }
    )
    
    if (res.code === 200 && res.data) {
      documentUrl.value = res.data
      // 保存原始文件名
      documentName.value = selectedDocumentFile.value.name
      selectedDocumentFile.value = null
      toast.success('文档上传成功！')
      
      // 自动渲染 Word 或 Markdown 文档
      if (isWordDocument()) {
        await renderWordDocument()
      } else if (isMarkdownDocument()) {
        await renderMarkdownDocument()
      }
      
      // 清空文件选择
      const fileInput = document.getElementById('document-file-input')
      if (fileInput) {
        fileInput.value = ''
      }
    }
  } catch (error) {
    logError('上传项目文档失败:', error)
    toast.error(error.message || '上传失败，请稍后重试')
  } finally {
    isUploadingDocument.value = false
    documentUploadProgress.value = 0
  }
}

// 删除项目文档
const deleteDocument = async () => {
  if (!confirm('确定要删除项目文档吗？')) {
    return
  }
  
  try {
    const res = await deleteProjectDocument(projectId.value)
    if (res.code === 200) {
      documentUrl.value = ''
      documentName.value = ''
      documentContent.value = ''
      toast.success('文档删除成功')
    }
  } catch (error) {
    logError('删除项目文档失败:', error)
    toast.error(error.message || '删除失败，请稍后重试')
  }
}

// 获取文档显示名称
const getDocumentName = () => {
  // 如果有原始文件名，优先使用
  if (documentName.value) {
    return documentName.value
  }
  // 否则从 URL 中提取
  if (!documentUrl.value) return ''
  const parts = documentUrl.value.split('/')
  return parts[parts.length - 1] || '未知文档'
}

// 判断是否为 PDF 文档
const isPdfDocument = () => {
  if (!documentUrl.value) return false
  return documentUrl.value.toLowerCase().endsWith('.pdf')
}

// 判断是否为 Word 文档
const isWordDocument = () => {
  if (!documentUrl.value) return false
  const ext = documentUrl.value.toLowerCase()
  return ext.endsWith('.doc') || ext.endsWith('.docx')
}

// 判断是否为 Markdown 文档
const isMarkdownDocument = () => {
  if (!documentUrl.value) return false
  return documentUrl.value.toLowerCase().endsWith('.md')
}

// 渲染 Word 文档为 HTML
const renderWordDocument = async () => {
  if (!isWordDocument()) return
  isRenderingDocument.value = true
  try {
    const response = await fetch(documentUrl.value)
    const arrayBuffer = await response.arrayBuffer()
    const mammoth = await import('mammoth')
    const result = await mammoth.default.convertToHtml({ arrayBuffer })
    documentContent.value = result.value
  } catch (error) {
    logError('渲染 Word 文档失败:', error)
    toast.error('Word 文档渲染失败，请下载后查看')
    documentContent.value = ''
  } finally {
    isRenderingDocument.value = false
  }
}

// 渲染 Markdown 文档为 HTML
const renderMarkdownDocument = async () => {
  if (!isMarkdownDocument()) return
  
  isRenderingDocument.value = true
  try {
    // 下载 Markdown 文件
    const response = await fetch(documentUrl.value)
    const markdownText = await response.text()
    
    const { marked } = await import('marked')
    documentContent.value = marked(markdownText)
  } catch (error) {
    logError('渲染 Markdown 文档失败:', error)
    toast.error('Markdown 文档渲染失败，请下载后查看')
    documentContent.value = ''
  } finally {
    isRenderingDocument.value = false
  }
}

// 切换标签页
const switchTab = (tab) => {
  activeTab.value = tab
}

// 提交评论
// 点赞项目
const toggleLike = async () => {
  try {
    if (project.value.isStarred) {
      // 取消点赞
      const res = await unstarProject(project.value.id)
      // 使用后端返回的实际数量更新状态
      if (res.code === 200 && res.data !== undefined) {
        project.value.starCount = res.data
        project.value.isStarred = false
      }
    } else {
      // 点赞
      const res = await starProject(project.value.id)
      // 使用后端返回的实际数量更新状态
      if (res.code === 200 && res.data !== undefined) {
        project.value.starCount = res.data
        project.value.isStarred = true
      }
    }
  } catch (error) {
    logError('点赞操作失败:', error)
    toast.error(error.message || '操作失败，请稍后重试')
    // 失败时重新加载项目数据以恢复正确状态
    await loadProjectDetail()
  }
}

// 收藏项目
const toggleFavorite = async () => {
  try {
    if (project.value.isWatched) {
      // 取消收藏
      const res = await unwatchProject(project.value.id)
      // 使用后端返回的实际数量更新状态
      if (res.code === 200 && res.data !== undefined) {
        project.value.watchCount = res.data
        project.value.isWatched = false
      }
    } else {
      // 收藏
      const res = await watchProject(project.value.id)
      // 使用后端返回的实际数量更新状态
      if (res.code === 200 && res.data !== undefined) {
        project.value.watchCount = res.data
        project.value.isWatched = true
      }
    }
  } catch (error) {
    logError('收藏操作失败:', error)
    toast.error(error.message || '操作失败，请稍后重试')
    // 失败时重新加载项目数据以恢复正确状态
    await loadProjectDetail()
  }
}

// 下载项目
const downloadProjectHandler = async () => {
  try {
    toast.info('正在准备下载...')
    
    // 调用真实 API 下载项目（后端会自动增加下载次数）
    await downloadProject(projectId.value)
    
    toast.success('下载成功！')
  } catch (error) {
    logError('下载项目失败:', error)
    toast.error(error.message || '下载失败，请稍后重试')
  }
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
    projectType: project.value.projectType || 'OTHER',
    courseName: project.value.courseName || '',
    thesisType: project.value.thesisType || '',
    tagIds: project.value.tags ? project.value.tags.map(tag => tag.id) : []
  }
}

// 加载标签列表（按分组）
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
  } catch (error) {
    logError('加载标签失败:', error)
  }
}

// 加载课程列表
const loadCourses = async () => {
  try {
    const res = await getActiveCourses()
    
    if (res.code === 200 && res.data) {
      // 提取课程名称列表
      courseList.value = res.data.map(course => course.courseName).sort()
    } else {
      logError('课程 API 返回数据异常:', res)
      courseList.value = []
    }
  } catch (error) {
    logError('加载课程列表失败:', error)
    courseList.value = []
  }
}

// 查看更多标签
const showMoreTags = (category) => {
  const totalCount = tagsByCategory.value[category].length
  const currentCount = displayCount.value[category]
  // 每次增加10个，但不超过总数
  displayCount.value[category] = Math.min(currentCount + 10, totalCount)
}

// 获取当前分类显示的标签
const getDisplayedTags = (category) => {
  return tagsByCategory.value[category].slice(0, displayCount.value[category])
}

// 是否还有更多标签
const hasMoreTags = (category) => {
  return displayCount.value[category] < tagsByCategory.value[category].length
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
      projectType: editForm.value.projectType,
      courseName: editForm.value.courseName,
      thesisType: editForm.value.thesisType,
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
        router.push('/projects')
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

const { organizedFiles, displayFiles, toggleFolder, expandedFolders } = useFileTree(selectedFiles)

// OSS 文件树（代码 Tab）
const ossExpandedFolders = ref(new Set())

const ossFileTree = computed(() => {
  if (!projectFiles.value || projectFiles.value.length === 0) return []
  const tree = []
  const fileMap = new Map()
  projectFiles.value.forEach(file => {
    fileMap.set(file.id, { ...file, children: [], level: 0 })
  })
  projectFiles.value.forEach(file => {
    const node = fileMap.get(file.id)
    if (file.parentId !== null && fileMap.has(file.parentId)) {
      const parent = fileMap.get(file.parentId)
      node.level = parent.level + 1
      parent.children.push(node)
    } else {
      tree.push(node)
    }
  })
  return tree
})

const toggleOssFolder = (fileId) => {
  const newSet = new Set(ossExpandedFolders.value)
  if (newSet.has(fileId)) newSet.delete(fileId)
  else newSet.add(fileId)
  ossExpandedFolders.value = newSet
}

// 处理文件选择
const handleFileSelect = (event) => {
  const files = Array.from(event.target.files)
  if (files.length === 0) return
  
  // 验证文件大小（单个文件不超过 100MB）
  const maxSize = 100 * 1024 * 1024
  const oversizedFiles = files.filter(file => file.size > maxSize)
  
  if (oversizedFiles.length > 0) {
    toast.warning(`以下文件超过 100MB，无法上传：${oversizedFiles.map(f => f.name).join(', ')}`)
  }
  
  // 过滤出符合要求的文件
  const validFiles = files.filter(file => file.size <= maxSize)
  
  // 如果是文件夹上传，保留相对路径信息
  validFiles.forEach((file, index) => {
    // webkitRelativePath 包含文件夹路径，如 "folder/subfolder/file.txt"
    if (file.webkitRelativePath) {
      file.relativePath = file.webkitRelativePath
    }
  })
  
  // 添加到待上传列表
  selectedFiles.value.push(...validFiles)
}

// 拖拽处理
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
      }
    }
  })
  
  selectedFiles.value.push(...validFiles)
}

// 按路径移除文件
const removeFileByPath = (path) => {
  const index = selectedFiles.value.findIndex(file => {
    const filePath = file.relativePath || file.webkitRelativePath || file.name
    return filePath === path
  })
  if (index > -1) {
    selectedFiles.value.splice(index, 1)
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

// 格式化评论时间
const formatCommentTime = (timeStr) => {
  if (!timeStr) return ''
  
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  // 超过7天显示具体日期
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 上传项目文件（OSS 直传，分批获取签名）
const uploadProjectFiles = async () => {
  if (selectedFiles.value.length === 0) {
    toast.warning('请先选择要上传的文件')
    return
  }

  const totalSize = selectedFiles.value.reduce((sum, f) => sum + f.size, 0)
  const totalSizeMB = (totalSize / 1024 / 1024).toFixed(2)

  if (isOverwriteMode.value && projectFiles.value.length > 0) {
    const confirmed = await new Promise((resolve) => {
      toast.confirm(
        `即将删除现有的 ${projectFiles.value.length} 个文件并上传新文件，是否继续？`,
        '确认覆盖上传',
        resolve
      )
    })
    if (!confirmed) return

    // 先删除旧文件记录
    await deleteAllProjectFiles(projectId.value)
  }

  if (selectedFiles.value.length > 100 || totalSize > 50 * 1024 * 1024) {
    toast.info(`正在上传 ${selectedFiles.value.length} 个文件（${totalSizeMB} MB），请耐心等待...`)
  }

  isUploading.value = true
  uploadProgress.value = 0

  try {
    const allFiles = selectedFiles.value
    const totalBytes = allFiles.reduce((sum, f) => sum + f.size, 0)
    let uploadedBytes = 0
    let fileLoadedPrev = 0

    // 分批获取签名，每批 200 个
    const BATCH = 200
    for (let i = 0; i < allFiles.length; i += BATCH) {
      const batch = allFiles.slice(i, i + BATCH)
      const fileList = batch.map(f => ({
            name: f.name,
            size: f.size,
            path: f.webkitRelativePath || f.relativePath || ''
          }))
      const sigRes = await getUploadSignatures(fileList)
      if (sigRes.code !== 200) throw new Error('获取上传签名失败')

      const signatures = sigRes.data
      const uploaded = []
      for (let j = 0; j < signatures.length; j++) {
        const sig = signatures[j]
        const file = batch[j]
        fileLoadedPrev = 0
        await uploadToPresignedUrl(sig, file, (loaded, total) => {
          uploadedBytes += loaded - fileLoadedPrev
          fileLoadedPrev = loaded
          uploadProgress.value = totalBytes > 0
            ? Math.min(99, Math.round((uploadedBytes / totalBytes) * 100))
            : Math.round(((i + j + 1) / allFiles.length) * 100)
        })
        uploaded.push({
          objectKey: sig.objectKey,
          fileName: sig.fileName,
          fileSize: file.size,
          path: sig.path || ''
        })
      }
      await confirmUpload(projectId.value, uploaded)
    }

    uploadProgress.value = 100
    toast.success(`成功上传 ${allFiles.length} 个文件`)
    selectedFiles.value = []
    setTimeout(() => {
      loadProjectDetail()
      isUploading.value = false
      uploadProgress.value = 0
    }, 1500)
  } catch (error) {
    logError('文件上传失败:', error)
    toast.error(error.message || '文件上传失败')
    isUploading.value = false
    uploadProgress.value = 0
  }
}

// 加载项目类型列表
const loadProjectTypes = async () => {
  try {
    const res = await getProjectTypes()
    
    if (res.code === 200 && res.data) {
      // 转换为前端需要的格式
      projectTypeOptions.value = res.data.map(type => ({
        value: type.typeCode,
        label: type.typeName
      }))
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
  loadProjectDetail()
  loadProjectTypes()
  loadTags()
  loadCourses() // 加载课程列表
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
            :class="['action-btn', { active: project.isStarred }]"
            @click="toggleLike"
          >
            ❤️
            {{ formatNumber(project.starCount) }}
          </button>
          <button 
            :class="['action-btn', { active: project.isWatched }]"
            @click="toggleFavorite"
          >
            ⭐
            {{ formatNumber(project.watchCount) }}
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
        <span class="loading-spinner">⏳</span>
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
              📄 项目文档
            </button>
            <button 
              :class="['tab-btn', { active: activeTab === 'code' }]"
              @click="switchTab('code')"
            >
              💻 代码
            </button>
            <button 
              :class="['tab-btn', { active: activeTab === 'comments' }]"
              @click="switchTab('comments')"
            >
              💬 评论
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

        <!-- 项目文档内容 -->
        <div v-if="activeTab === 'readme'" class="content-section document-content">
          <!-- 已有文档：显示预览 -->
          <div v-if="documentUrl" class="document-viewer">
            <!-- 删除按钮（仅所有者可见，放在右上角） -->
            <button 
              v-if="isOwner"
              class="btn-delete-document-float"
              @click="deleteDocument"
              title="删除文档"
            >
              删除文档
            </button>
            
            <!-- PDF 预览 -->
            <iframe 
              v-if="isPdfDocument()"
              :src="documentUrl"
              class="pdf-preview"
              frameborder="0"
            ></iframe>
            
            <!-- Word 文档预览 -->
            <div v-else-if="isWordDocument()" class="word-preview">
              <div v-if="isRenderingDocument" class="rendering-loading">
                <span class="loading-spinner">⏳</span>
                <p>正在渲染 Word 文档...</p>
              </div>
              <div v-else-if="documentContent" class="rendered-content" v-html="documentContent"></div>
              <div v-else class="render-failed">
                <p>⚠️ Word 文档渲染失败</p>
                <a :href="documentUrl" target="_blank" class="download-link">
                  ⬇️ 下载文档
                </a>
              </div>
            </div>
            
            <!-- Markdown 文档预览 -->
            <div v-else-if="isMarkdownDocument()" class="markdown-preview">
              <div v-if="isRenderingDocument" class="rendering-loading">
                <span class="loading-spinner">⏳</span>
                <p>正在渲染 Markdown 文档...</p>
              </div>
              <div v-else-if="documentContent" class="rendered-content markdown-body" v-html="documentContent"></div>
              <div v-else class="render-failed">
                <p>⚠️ Markdown 文档渲染失败</p>
                <a :href="documentUrl" target="_blank" class="download-link">
                  ⬇️ 下载文档
                </a>
              </div>
            </div>
            
            <!-- 其他格式：提供下载链接 -->
            <div v-else class="document-download">
              <p>此文档格式不支持在线预览，请下载后查看</p>
              <a :href="documentUrl" target="_blank" class="download-link">
                ⬇️ 下载文档
              </a>
            </div>
          </div>
          
          <!-- 无文档：显示上传区域（仅所有者可见） -->
          <div v-else-if="isOwner" class="document-upload-area">
            <div class="upload-placeholder">
              <span class="upload-icon">📄</span>
              <p class="upload-text">暂无项目文档</p>
              <p class="upload-hint">支持 PDF、Word、TXT、Markdown、PPT 格式</p>
              
              <div class="file-select-wrapper">
                <input 
                  type="file" 
                  id="document-file-input"
                  accept=".pdf,.doc,.docx,.txt,.md,.ppt,.pptx"
                  @change="handleDocumentFileSelect"
                  class="file-input"
                />
                <label for="document-file-input" class="select-file-btn">
                  选择文件
                </label>
              </div>
              
              <!-- 显示选中的文件 -->
              <div v-if="selectedDocumentFile" class="selected-file-info">
                <p>已选择：{{ selectedDocumentFile.name }}</p>
                <p class="file-size">{{ (selectedDocumentFile.size / 1024).toFixed(2) }} KB</p>
                <button 
                  class="upload-btn"
                  @click="uploadDocument"
                  :disabled="isUploadingDocument"
                >
                  {{ isUploadingDocument ? '上传中...' : '上传文档' }}
                </button>
                
                <!-- 上传进度条 -->
                <div v-if="isUploadingDocument" class="progress-bar">
                  <div 
                    class="progress-fill" 
                    :style="{ width: documentUploadProgress + '%' }"
                  ></div>
                  <span class="progress-text">{{ documentUploadProgress }}%</span>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 无文档且非所有者 -->
          <div v-else class="empty-document">
            <span class="empty-icon">📄</span>
            <p>该项目暂无文档</p>
          </div>
        </div>

        <!-- 代码内容 -->
        <div v-if="activeTab === 'code'" class="content-section code-content">
          <div class="file-tree">
            <h3>项目文件</h3>
            
            <!-- 加载状态 -->
            <div v-if="isLoadingFiles" class="loading-files">
              <span class="loading-spinner">⏳</span>
              <p>加载文件中...</p>
            </div>
            
            <!-- 空状态 -->
            <div v-else-if="ossFileTree.length === 0" class="empty-files">
              <span class="empty-icon">📁</span>
              <p>暂无文件</p>
              <p class="hint">项目所有者可以在设置中上传文件</p>
            </div>
            
            <!-- 文件列表 -->
            <ul v-else class="file-list">
              <template v-for="file in ossFileTree" :key="file.id">
                <FileTreeItem
                  :file="file"
                  :expanded-folders="ossExpandedFolders"
                  @toggle="toggleOssFolder"
                />
              </template>
            </ul>
          </div>
        </div>

        <!-- 评论内容 -->
        <ProjectCommentsTab v-if="activeTab === 'comments'" :project-id="projectId" />

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

            <!-- 项目类型选择 -->
            <div class="form-group">
              <label class="form-label">项目类型</label>
              <div class="project-type-grid">
                <div
                  v-for="option in projectTypeOptions"
                  :key="option.value"
                  :class="['type-option', { selected: editForm.projectType === option.value }]"
                  @click="isEditing && (editForm.projectType = option.value)"
                >
                  <span class="type-label">{{ option.label }}</span>
                </div>
              </div>
            </div>

            <!-- 课程名称（仅当项目是课程设计时显示） -->
            <div v-if="editForm.projectType === 'COURSE'" class="form-group">
              <label class="form-label">课程名称</label>
              <select 
                v-model="editForm.courseName"
                class="form-select"
                :disabled="!isEditing"
              >
                <option value="">请选择课程</option>
                <option v-for="course in courseList" :key="course" :value="course">
                  {{ course }}
                </option>
              </select>
            </div>

            <!-- 毕设类型（仅当选择毕业设计时显示） -->
            <div v-if="editForm.projectType === 'THESIS'" class="form-group">
              <label class="form-label">毕设类型</label>
              <div class="thesis-type-options">
                <div
                  v-for="option in thesisTypeOptions"
                  :key="option.value"
                  :class="['thesis-option', { selected: editForm.thesisType === option.value }]"
                  @click="isEditing && (editForm.thesisType = option.value)"
                >
                  {{ option.label }}
                </div>
              </div>
            </div>

            <!-- 非编辑模式：只显示项目的标签 -->
            <div v-if="!isEditing && project.tags && project.tags.length > 0" class="form-group">
              <label class="form-label">标签</label>
              <div class="tags-display">
                <span 
                  v-for="tag in project.tags" 
                  :key="tag.id"
                  class="tag-badge"
                >
                  {{ tag.name }}
                </span>
              </div>
            </div>

            <!-- 编辑模式：显示分组标签选择器 -->
            <div v-if="isEditing" class="form-group">
              <label class="form-label">标签 <span class="optional">(可选)</span></label>
              
              <!-- 技术栈标签 -->
              <div v-if="tagsByCategory['技术栈'].length > 0" class="tag-category">
                <h4 class="category-title-small">🔧 技术栈</h4>
                <div class="tags-grid">
                  <div
                    v-for="tag in getDisplayedTags('技术栈')"
                    :key="tag.id"
                    :class="['tag-item', { selected: editForm.tagIds.includes(tag.id) }]"
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
                <h4 class="category-title-small">🎯 领域</h4>
                <div class="tags-grid">
                  <div
                    v-for="tag in getDisplayedTags('领域')"
                    :key="tag.id"
                    :class="['tag-item', { selected: editForm.tagIds.includes(tag.id) }]"
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

            <!-- 文件上传区域（仅编辑模式显示） -->
            <div v-if="isEditing" class="form-group">
              <label class="form-label">
                📁 重传项目文件 <span class="optional">(可选)</span>
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

              <!-- 覆盖模式选项（始终显示） -->
              <div class="overwrite-option">
                <label class="checkbox-label">
                  <input 
                    type="checkbox" 
                    v-model="isOverwriteMode"
                    :disabled="projectFiles.length === 0 || isUploading"
                  />
                  <span class="checkbox-text">
                    🔄 覆盖模式（先删除现有文件再上传）
                    <span v-if="projectFiles.length > 0" class="file-count">
                      （当前有 {{ projectFiles.length }} 个文件将被替换）
                    </span>
                    <span v-else class="hint-text">
                      （新项目，直接上传）
                    </span>
                  </span>
                </label>
                <p class="option-hint">
                  💡 提示：关闭此选项将追加上传，保留现有文件
                </p>
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

            <!-- 操作按钮 -->
            <div class="form-actions">
              <template v-if="!isEditing">
                <button class="btn btn-primary" @click="startEdit">
                  编辑项目
                </button>
              </template>
              <template v-else>
                <button 
                  type="button"
                  class="btn btn-secondary" 
                  @click="uploadProjectFiles"
                  :disabled="isUploading || selectedFiles.length === 0"
                >
                  {{ isUploading ? '上传中...' : '开始上传' }}
                </button>
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
  color: #064e3b;
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
  border-color: #10b981;
  color: #10b981;
}

.action-btn.active {
  background-color: #10b981;
  border-color: #10b981;
  color: #ffffff;
}

.action-btn.primary {
  background-color: #10b981;
  border-color: #10b981;
  color: #ffffff;
}

.action-btn.primary:hover {
  background-color: #059669;
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
  color: #10b981;
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
  color: #10b981;
  border-bottom-color: #10b981;
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
.file-tree {
  background: linear-gradient(135deg, #fafbfc 0%, #f5f7fa 100%);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.file-tree h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #2c3e50;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-tree h3::before {
  content: '';
  display: inline-block;
  width: 20px;
  height: 20px;
  margin-right: 8px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23666'%3E%3Cpath d='M10 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z'/%3E%3C/svg%3E");
  background-size: contain;
  background-repeat: no-repeat;
}

/* 加载状态 */
.loading-files {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  color: #999999;
  background: linear-gradient(135deg, #f0f9ff 0%, #e8f4ff 100%);
  border-radius: 8px;
}

.loading-files .loading-spinner {
  font-size: 40px;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
  filter: drop-shadow(0 2px 4px rgba(0, 89, 179, 0.2));
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.loading-files p {
  font-size: 14px;
  color: #10b981;
  font-weight: 500;
}

/* 空状态 */
.empty-files {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 20px;
  color: #999999;
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
  border-radius: 8px;
  border: 2px dashed #d9d9d9;
}

.empty-files .empty-icon {
  font-size: 72px;
  margin-bottom: 20px;
  opacity: 0.4;
  filter: grayscale(50%);
}

.empty-files p {
  font-size: 15px;
  margin: 6px 0;
  color: #666;
  font-weight: 500;
}

.empty-files .hint {
  font-size: 13px;
  color: #bbb;
  font-weight: 400;
  margin-top: 8px;
}

/* 文件列表项 - 由 FileTreeItem 组件管理 */
.file-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

/* 项目文档区域 */
.document-content {
  padding: 32px;
  min-height: 400px;
}

/* 文档查看器 */
.document-viewer {
  width: 100%;
  position: relative;
}

.document-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #e0e0e0;
}

.document-header h3 {
  font-size: 20px;
  font-weight: 600;
  color: #333333;
  margin: 0;
}

.btn-delete-document {
  padding: 8px 16px;
  background-color: #ff4d4f;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-delete-document:hover {
  background-color: #ff7875;
}

/* 浮动删除按钮（不显示文档名时使用） */
.btn-delete-document-float {
  position: absolute;
  top: 16px;
  right: 16px;
  padding: 8px 16px;
  background-color: rgba(255, 77, 79, 0.9);
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(255, 77, 79, 0.3);
}

.btn-delete-document-float:hover {
  background-color: rgba(255, 120, 117, 1);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.4);
}

.btn-delete-document-float:active {
  transform: translateY(0);
}

/* PDF 预览 */
.pdf-preview {
  width: 100%;
  height: 800px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background-color: #f9f9f9;
}

/* 文档下载区域 */
.document-download {
  text-align: center;
  padding: 60px 20px;
  background-color: #fafafa;
  border-radius: 8px;
  border: 2px dashed #d9d9d9;
}

.document-download p {
  font-size: 16px;
  color: #666666;
  margin-bottom: 24px;
}

.download-link {
  display: inline-block;
  padding: 12px 32px;
  background-color: #10b981;
  color: #ffffff;
  text-decoration: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.2s;
}

.download-link:hover {
  background-color: #059669;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

/* Word 文档预览 */
.word-preview {
  width: 100%;
}

.rendering-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #666666;
}

.rendering-loading .loading-spinner {
  font-size: 48px;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.rendered-content {
  max-height: 700px;
  padding: 32px;
  background-color: #ffffff;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  line-height: 1.8;
  overflow-y: auto;
  overflow-x: auto;
}

/* 自定义滚动条样式 */
.rendered-content::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.rendered-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.rendered-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.rendered-content::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}

.rendered-content h1,
.rendered-content h2,
.rendered-content h3,
.rendered-content h4,
.rendered-content h5,
.rendered-content h6 {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.25;
  color: #333333;
}

.rendered-content h1 {
  font-size: 2em;
  border-bottom: 2px solid #eaecef;
  padding-bottom: 0.3em;
}

.rendered-content h2 {
  font-size: 1.5em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 0.3em;
}

.rendered-content p {
  margin: 16px 0;
  color: #333333;
}

.rendered-content ul,
.rendered-content ol {
  margin: 16px 0;
  padding-left: 2em;
}

.rendered-content li {
  margin: 8px 0;
}

.rendered-content code {
  padding: 0.2em 0.4em;
  background-color: #f6f8fa;
  border-radius: 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 85%;
}

.rendered-content pre {
  padding: 16px;
  background-color: #f6f8fa;
  border-radius: 6px;
  overflow: auto;
  margin: 16px 0;
}

.rendered-content pre code {
  padding: 0;
  background-color: transparent;
}

.rendered-content table {
  border-collapse: collapse;
  width: 100%;
  margin: 16px 0;
}

.rendered-content table th,
.rendered-content table td {
  padding: 8px 12px;
  border: 1px solid #dfe2e5;
}

.rendered-content table tr:nth-child(2n) {
  background-color: #f6f8fa;
}

.rendered-content img {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
}

.rendered-content blockquote {
  padding: 0 1em;
  color: #6a737d;
  border-left: 0.25em solid #dfe2e5;
  margin: 16px 0;
}

.rendered-content a {
  color: #0366d6;
  text-decoration: none;
}

.rendered-content a:hover {
  text-decoration: underline;
}

.render-failed {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  text-align: center;
}

.render-failed p {
  font-size: 16px;
  color: #ff6b6b;
  margin-bottom: 20px;
}

/* Markdown 预览 */
.markdown-preview {
  width: 100%;
}

.markdown-body {
  /* GitHub Markdown 风格 */
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
  font-size: 16px;
  line-height: 1.6;
  word-wrap: break-word;
}

/* 文档上传区域 */
.document-upload-area {
  width: 100%;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 20px;
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
  border-radius: 8px;
  border: 2px dashed #d9d9d9;
}

.upload-placeholder .upload-icon {
  font-size: 72px;
  margin-bottom: 20px;
  opacity: 0.4;
}

.upload-text {
  font-size: 18px;
  color: #333333;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.upload-hint {
  font-size: 14px;
  color: #999999;
  margin: 0 0 32px 0;
}

/* 文件选择包装器 */
.file-select-wrapper {
  position: relative;
  margin-bottom: 24px;
}

.file-input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.select-file-btn {
  display: inline-block;
  padding: 12px 32px;
  background-color: #10b981;
  color: #ffffff;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.select-file-btn:hover {
  background-color: #059669;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

/* 选中文件信息 */
.selected-file-info {
  text-align: center;
  padding: 24px;
  background-color: #ffffff;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  width: 100%;
  max-width: 400px;
}

.selected-file-info p {
  font-size: 14px;
  color: #333333;
  margin: 8px 0;
}

.file-size {
  font-size: 12px;
  color: #999999;
}

.upload-btn {
  margin-top: 16px;
  padding: 12px 32px;
  background-color: #10b981;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  width: 100%;
}

.upload-btn:hover:not(:disabled) {
  background-color: #059669;
}

.upload-btn:disabled {
  background-color: #d9d9d9;
  cursor: not-allowed;
}

/* 进度条 */
.progress-bar {
  margin-top: 16px;
  width: 100%;
  height: 24px;
  background-color: #f0f0f0;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981 0%, #059669 100%);
  transition: width 0.3s ease;
  border-radius: 12px;
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 12px;
  font-weight: 600;
  color: #333333;
}

/* 空文档状态 */
.empty-document {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 20px;
  color: #999999;
}

.empty-document .empty-icon {
  font-size: 72px;
  margin-bottom: 20px;
  opacity: 0.4;
}

.empty-document p {
  font-size: 16px;
  color: #666666;
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
  background-color: #10b981;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.create-issue-btn:hover {
  background-color: #059669;
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
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
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

/* 项目类型选择 */
.project-type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.type-option {
  padding: 16px;
  background-color: #ffffff;
  border: 2px solid #d9d9d9;
  border-radius: 8px;
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
  background-color: rgba(16, 185, 129, 0.02);
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(16, 185, 129, 0.1);
}

.type-option.selected {
  border-color: #10b981;
  background-color: rgba(16, 185, 129, 0.05);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.15);
}

.type-label {
  font-size: 14px;
  font-weight: 600;
  color: #333333;
}

.type-option.selected .type-label {
  color: #059669;
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
  border: 2px solid #d9d9d9;
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
  background-color: rgba(16, 185, 129, 0.02);
}

.thesis-option.selected {
  background-color: #10b981;
  color: #ffffff;
  border-color: #10b981;
  font-weight: 600;
}

/* 非编辑模式下的标签显示 */
.tags-display {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  background-color: #fafafa;
}

.tag-badge {
  padding: 6px 14px;
  background-color: rgba(16, 185, 129, 0.08);
  border-radius: 16px;
  font-size: 13px;
  color: #10b981;
  font-weight: 500;
}

/* 编辑模式下的标签分组 */
.tag-category {
  margin-bottom: 20px;
}

.category-title-small {
  font-size: 14px;
  font-weight: 600;
  color: #064e3b;
  margin: 0 0 10px 0;
  padding-bottom: 6px;
  border-bottom: 1px solid #e8e8e8;
}

/* 标签网格 */
.tags-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 10px;
  margin-bottom: 10px;
}

.tag-item {
  padding: 7px 14px;
  background-color: rgba(16, 185, 129, 0.08);
  border: 2px solid transparent;
  border-radius: 6px;
  font-size: 13px;
  color: #10b981;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
  user-select: none;
}

.tag-item:hover {
  background-color: rgba(16, 185, 129, 0.15);
  transform: translateY(-1px);
}

.tag-item.selected {
  background-color: #10b981;
  color: #ffffff;
  border-color: #10b981;
  font-weight: 600;
}

/* 查看更多按钮 */
.show-more-btn {
  margin-top: 10px;
  padding: 7px 18px;
  background-color: transparent;
  border: 1px dashed #10b981;
  border-radius: 6px;
  font-size: 12px;
  color: #10b981;
  cursor: pointer;
  transition: all 0.2s;
  width: 100%;
}

.show-more-btn:hover {
  background-color: rgba(16, 185, 129, 0.05);
  border-style: solid;
}

.form-hint {
  font-size: 12px;
  color: #999999;
  margin: 8px 0 0 0;
}

/* 文件上传区域样式 */
.file-upload-group {
  padding: 16px;
  background-color: #f9f9f9;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
}

.upload-hint {
  font-size: 13px;
  color: #666666;
  margin: 0 0 12px 0;
  line-height: 1.5;
}

.file-select-area {
  margin-bottom: 12px;
}

.file-input {
  display: none;
}

.file-input-label {
  display: inline-block;
  padding: 8px 16px;
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  color: #333333;
  cursor: pointer;
  transition: all 0.2s;
}

.file-input-label:hover {
  border-color: #10b981;
  color: #10b981;
  background-color: #ecfdf5;
}

.selected-files-list {
  margin: 12px 0;
}

.files-list-title {
  font-size: 13px;
  font-weight: 500;
  color: #333333;
  margin: 0 0 8px 0;
}

.files-list {
  list-style: none;
  padding: 0;
  margin: 0;
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background-color: #ffffff;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s;
}

.file-item:last-child {
  border-bottom: none;
}

.file-item:hover {
  background-color: #f9f9f9;
}

.file-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.file-name {
  flex: 1;
  font-size: 13px;
  color: #333333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 12px;
  color: #999999;
  white-space: nowrap;
  flex-shrink: 0;
}

.remove-file-btn {
  width: 22px;
  height: 22px;
  padding: 0;
  background-color: transparent;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  color: #999999;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.remove-file-btn:hover {
  background-color: #dc3545;
  border-color: #dc3545;
  color: #ffffff;
}

.upload-progress {
  margin: 12px 0;
}

.progress-bar {
  width: 100%;
  height: 6px;
  background-color: #e0e0e0;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 6px;
}

.progress-fill {
  height: 100%;
  background-color: #10b981;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: #666666;
  margin: 0;
  text-align: center;
}

.upload-actions {
  margin-top: 12px;
}

.upload-btn {
  font-size: 13px;
  padding: 8px 16px;
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
  background-color: #10b981;
  color: #ffffff;
}

.btn-primary:hover {
  background-color: #059669;
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

/* 文件上传区域 */
.upload-area {
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: linear-gradient(135deg, #fafbfc 0%, #f5f7fa 100%);
  position: relative;
  overflow: hidden;
}

.upload-area::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at center, rgba(0, 89, 179, 0.03) 0%, transparent 70%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.upload-area:hover {
  border-color: #10b981;
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.1);
}

.upload-area:hover::before {
  opacity: 1;
}

.upload-icon {
  font-size: 56px;
  margin-bottom: 16px;
  display: block;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
  transition: transform 0.3s ease;
}

.upload-area:hover .upload-icon {
  transform: scale(1.1) translateY(-4px);
}

.upload-text {
  font-size: 15px;
  color: #333333;
  margin: 0 0 8px 0;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.upload-hint {
  font-size: 13px;
  color: #999999;
  margin: 0;
  line-height: 1.5;
}

/* 文件列表 */
.file-list {
  margin-top: 16px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  max-height: 300px;
  overflow-y: auto;
  background-color: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.file-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  background: linear-gradient(135deg, #f5f7fa 0%, #eef1f5 100%);
  border-bottom: 1px solid #e8e8e8;
  font-size: 14px;
  font-weight: 600;
  color: #333333;
  border-radius: 8px 8px 0 0;
}

.clear-btn {
  padding: 6px 14px;
  background-color: transparent;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 12px;
  color: #666666;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}

.clear-btn:hover {
  background-color: #ff4d4f;
  border-color: #ff4d4f;
  color: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(255, 77, 79, 0.3);
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.2s ease;
}

.file-item:last-child {
  border-bottom: none;
}

.file-item:hover {
  background-color: #f9fbfd;
  padding-left: 20px;
}

/* 文件夹项样式 */
.folder-item {
  background: linear-gradient(90deg, #fafbfc 0%, #ffffff 100%);
  font-weight: 500;
}

.folder-item .file-name {
  color: #10b981;
  font-weight: 600;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.file-icon {
  font-size: 22px;
  flex-shrink: 0;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.08));
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
  font-weight: 400;
}

.remove-btn {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: transparent;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
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
  transform: scale(1.1);
  box-shadow: 0 2px 6px rgba(255, 77, 79, 0.3);
}

/* 上传进度 */
.upload-progress {
  margin-top: 16px;
  padding: 16px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e8f4ff 100%);
  border: 1px solid #bae7ff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 89, 179, 0.08);
}

.progress-bar {
  width: 100%;
  height: 8px;
  background-color: #e8e8e8;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 10px;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.1);
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981, #34d399, #6ee7b7);
  background-size: 200% 100%;
  animation: progressShine 2s ease-in-out infinite;
  transition: width 0.3s ease;
  border-radius: 4px;
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.3);
}

@keyframes progressShine {
  0%, 100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.progress-text {
  font-size: 13px;
  color: #10b981;
  font-weight: 600;
  text-align: center;
  letter-spacing: 0.3px;
}

/* 覆盖模式选项 */
.overwrite-option {
  margin-top: 16px;
  padding: 12px;
  background-color: #fff8e6;
  border: 1px solid #ffd591;
  border-radius: 6px;
}

.checkbox-label {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.checkbox-label input[type="checkbox"] {
  margin-top: 2px;
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.checkbox-label input[type="checkbox"]:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.checkbox-text {
  font-size: 14px;
  color: #333333;
  line-height: 1.5;
}

.file-count {
  color: #fa8c16;
  font-weight: 500;
}

.hint-text {
  color: #52c41a;
  font-weight: 500;
}

.option-hint {
  margin: 8px 0 0 24px;
  font-size: 12px;
  color: #999999;
  line-height: 1.5;
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
