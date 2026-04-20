<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProjectDetail, starProject, unstarProject, watchProject, unwatchProject, downloadProject, updateProject, deleteProject, uploadFiles, overwriteUploadFiles, getProjectFiles, getAllProjectFiles } from '@/api/project'
import { getProjectComments, createComment } from '@/api/comment'
import { getTags } from '@/api/tag'
import { getUserById } from '@/api/auth'
import { toast } from '@/utils/toast'
import { log, error as logError, warn } from '@/utils/logger'
import tokenManager from '@/utils/tokenManager'
import FileTreeItem from './FileTreeItem.vue'

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

// 文件上传相关
const selectedFiles = ref([])
const uploadedFiles = ref([])
const isUploading = ref(false)
const uploadProgress = ref(0)
const isOverwriteMode = ref(true) // 默认开启覆盖模式

// 项目文件列表（从 OSS 获取）
const projectFiles = ref([])
const isLoadingFiles = ref(false)

// 文件夹展开状态（存储展开的文件夹 ID）
const expandedFolders = ref(new Set())

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
      
      // 加载项目文件列表
      await loadProjectFiles()
      
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
      
      // 批量加载评论者的用户信息
      await loadCommentUsers()
    }
  } catch (error) {
    logError('加载评论失败:', error)
    // 评论加载失败不影响主流程
  }
}

// 加载评论者的用户信息
const loadCommentUsers = async () => {
  try {
    // 收集所有唯一的 userId
    const userIds = [...new Set(comments.value.map(c => c.userId).filter(id => id))]
    
    if (userIds.length === 0) return
    
    // 批量查询用户信息
    const userPromises = userIds.map(userId => getUserById(userId))
    const userResults = await Promise.all(userPromises)
    
    // 创建 userId -> userInfo 的映射
    const userMap = {}
    userResults.forEach((res, index) => {
      if (res.code === 200 && res.data) {
        userMap[userIds[index]] = res.data
      }
    })
    
    // 为每条评论添加用户信息
    comments.value = comments.value.map(comment => {
      const user = userMap[comment.userId]
      return {
        ...comment,
        userName: user ? user.username : '未知用户',
        userAvatar: user ? user.avatar : ''
      }
    })
    
    log('评论用户信息加载成功')
  } catch (error) {
    logError('加载评论用户信息失败:', error)
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
      log('项目文件加载成功，数量:', projectFiles.value.length)
      
      // 打印前3个文件的详细信息用于调试
      if (projectFiles.value.length > 0) {
        log('文件数据示例（前3个）:', JSON.stringify(projectFiles.value.slice(0, 3), null, 2))
      }
    }
  } catch (error) {
    logError('加载项目文件失败:', error)
    // 文件加载失败不影响主流程
  } finally {
    isLoadingFiles.value = false
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

// 按文件夹结构组织文件
const organizedFiles = computed(() => {
  const fileTree = {}
  
  selectedFiles.value.forEach((file, index) => {
    const path = file.relativePath || file.webkitRelativePath || file.name
    const parts = path.split('/')
    
    // 如果没有路径，直接放在根目录
    if (parts.length === 1) {
      if (!fileTree['__root__']) {
        fileTree['__root__'] = { type: 'folder', name: '根目录', children: [] }
      }
      fileTree['__root__'].children.push({ 
        type: 'file', 
        fileIndex: index,
        name: file.name,
        size: file.size
      })
    } else {
      // 有路径，构建文件夹树
      let currentLevel = fileTree
      
      parts.forEach((part, idx) => {
        if (idx === parts.length - 1) {
          // 最后一个部分是文件
          if (!currentLevel['__files__']) {
            currentLevel['__files__'] = []
          }
          currentLevel['__files__'].push({ 
            type: 'file',
            fileIndex: index,
            name: part,
            size: file.size
          })
        } else {
          // 中间部分是文件夹
          if (!currentLevel[part]) {
            currentLevel[part] = { type: 'folder', name: part, children: {} }
          }
          currentLevel = currentLevel[part].children
        }
      })
    }
  })
  
  return fileTree
})

// 递归渲染文件树
const renderFileTree = (tree, level = 0, parentKey = '') => {
  const result = []
  
  // 先显示当前层的文件
  if (tree['__files__']) {
    tree['__files__'].forEach(file => {
      result.push({ ...file, level })
    })
  }
  
  // 再递归显示子文件夹
  Object.keys(tree).forEach(key => {
    if (key !== '__files__' && key !== '__root__') {
      const folder = tree[key]
      const folderKey = parentKey ? `${parentKey}/${key}` : key
      const isExpanded = expandedFolders.value.has(folderKey)
      
      result.push({ 
        type: 'folder', 
        name: folder.name, 
        level, 
        isFolder: true,
        folderKey,
        isExpanded
      })
      
      // 如果文件夹已展开，递归显示子内容
      if (isExpanded) {
        result.push(...renderFileTree(folder.children, level + 1, folderKey))
      }
    }
  })
  
  // 处理根目录的文件
  if (tree['__root__']) {
    tree['__root__'].children.forEach(file => {
      result.push({ ...file, level })
    })
  }
  
  return result
}

// 展平的文件列表（用于显示）
const displayFiles = computed(() => {
  return renderFileTree(organizedFiles.value)
})

// OSS 文件树形结构（用于代码展示）
const ossFileTree = computed(() => {
  if (!projectFiles.value || projectFiles.value.length === 0) {
    return []
  }
  
  log('构建文件树，总文件数:', projectFiles.value.length)
  
  // 构建树形结构
  const tree = []
  const fileMap = new Map()
  
  // 第一遍：创建所有节点
  projectFiles.value.forEach(file => {
    fileMap.set(file.id, {
      ...file,
      children: [],
      level: 0
    })
  })
  
  // 第二遍：建立父子关系
  projectFiles.value.forEach(file => {
    const node = fileMap.get(file.id)
    if (file.parentId !== null && fileMap.has(file.parentId)) {
      // 有父节点，添加到父节点的children
      const parent = fileMap.get(file.parentId)
      node.level = parent.level + 1
      parent.children.push(node)
      log(`文件 "${file.fileName}" 是 "${parent.fileName}" 的子节点`)
    } else {
      // 根节点
      tree.push(node)
      log(`文件 "${file.fileName}" 是根节点`)
    }
  })
  
  log('文件树构建完成，根节点数量:', tree.length)
  return tree
})

// 将文件树展平为列表（递归）
const flattenFileTree = (tree) => {
  const result = []
  
  const traverse = (nodes) => {
    nodes.forEach(node => {
      result.push(node)
      if (node.children && node.children.length > 0) {
        traverse(node.children)
      }
    })
  }
  
  traverse(tree)
  return result
}

// 切换文件夹展开/折叠状态
const toggleFolder = (fileId) => {
  const newSet = new Set(expandedFolders.value)
  if (newSet.has(fileId)) {
    newSet.delete(fileId)
    log('折叠文件夹 ID:', fileId)
  } else {
    newSet.add(fileId)
    log('展开文件夹 ID:', fileId)
  }
  expandedFolders.value = newSet
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

// 上传项目文件（分批上传）
const uploadProjectFiles = async () => {
  if (selectedFiles.value.length === 0) {
    toast.warning('请先选择要上传的文件')
    return
  }
  
  // 计算总文件大小
  const totalSize = selectedFiles.value.reduce((sum, file) => sum + file.size, 0)
  const totalSizeMB = (totalSize / 1024 / 1024).toFixed(2)
  
  // 如果是覆盖模式，给出提示
  if (isOverwriteMode.value && projectFiles.value.length > 0) {
    const confirmUpload = await new Promise((resolve) => {
      toast.confirm(
        `即将删除现有的 ${projectFiles.value.length} 个文件并上传新文件，是否继续？`,
        '确认覆盖上传',
        resolve
      )
    })
    
    if (!confirmUpload) {
      log('用户取消覆盖上传')
      return
    }
  }
  
  // 如果文件数量多或体积大，给出提示
  if (selectedFiles.value.length > 100 || totalSize > 50 * 1024 * 1024) {
    toast.info(`正在上传 ${selectedFiles.value.length} 个文件（${totalSizeMB} MB），请耐心等待...`)
  }
  
  isUploading.value = true
  uploadProgress.value = 0
  
  try {
    log('开始上传文件，数量:', selectedFiles.value.length, '总大小:', totalSizeMB, 'MB', '模式:', isOverwriteMode.value ? '覆盖' : '追加')
    
    // 分批上传，每批50个文件
    const batchSize = 50
    const totalBatches = Math.ceil(selectedFiles.value.length / batchSize)
    let uploadedCount = 0
    let res
    
    for (let i = 0; i < totalBatches; i++) {
      const start = i * batchSize
      const end = Math.min(start + batchSize, selectedFiles.value.length)
      const batch = selectedFiles.value.slice(start, end)
      
      log(`上传第 ${i + 1}/${totalBatches} 批，文件数: ${batch.length}`)
      
      if (isOverwriteMode.value && i === 0) {
        // 覆盖模式：第一批使用覆盖上传接口（会先删除旧文件）
        res = await overwriteUploadFiles(
          projectId.value,
          batch,
          null,
          (progress) => {
            // 计算总体进度
            const batchProgress = progress / 100
            const overallProgress = ((uploadedCount + batch.length * batchProgress) / selectedFiles.value.length) * 100
            uploadProgress.value = Math.round(overallProgress)
            
            // 每10%记录一次日志
            if (Math.round(overallProgress) % 10 === 0 && Math.round(overallProgress) > 0) {
              log('总体进度:', Math.round(overallProgress) + '%')
            }
          }
        )
      } else {
        // 其他批次使用普通上传接口
        res = await uploadFiles(
          projectId.value,
          batch,
          null,
          (progress) => {
            // 计算总体进度
            const batchProgress = progress / 100
            const overallProgress = ((uploadedCount + batch.length * batchProgress) / selectedFiles.value.length) * 100
            uploadProgress.value = Math.round(overallProgress)
            
            // 每10%记录一次日志
            if (Math.round(overallProgress) % 10 === 0 && Math.round(overallProgress) > 0) {
              log('总体进度:', Math.round(overallProgress) + '%')
            }
          }
        )
      }
      
      uploadedCount += batch.length
      log(`第 ${i + 1} 批上传完成，已上传: ${uploadedCount}/${selectedFiles.value.length}`)
    }
    
    uploadProgress.value = 100
    
    // 显示成功消息
    if (isOverwriteMode.value && res.message) {
      toast.success(res.message || `成功上传 ${selectedFiles.value.length} 个文件`)
    } else {
      toast.success(`成功上传 ${selectedFiles.value.length} 个文件`)
    }
    
    log('文件上传成功')
    
    // 清空已选择的文件
    selectedFiles.value = []
    
    // 延迟后重新加载项目详情
    setTimeout(() => {
      loadProjectDetail()
      isUploading.value = false
      uploadProgress.value = 0
    }, 1500)
  } catch (error) {
    logError('文件上传失败:', error)
    
    // 更友好的错误提示
    let errorMessage = '文件上传失败'
    if (error.message?.includes('timeout')) {
      errorMessage = '上传超时，文件较多时可能需要较长时间，请检查网络连接后重试'
    } else if (error.message?.includes('network')) {
      errorMessage = '网络连接异常，请检查网络后重试'
    } else if (error.message) {
      errorMessage = error.message
    }
    
    toast.error(errorMessage)
    isUploading.value = false
    uploadProgress.value = 0
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
            
            <!-- 加载状态 -->
            <div v-if="isLoadingFiles" class="loading-files">
              <span class="loading-spinner">⟳</span>
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
                  :expanded-folders="expandedFolders"
                  @toggle="toggleFolder"
                />
              </template>
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
                <img 
                  v-if="comment.userAvatar" 
                  :src="comment.userAvatar" 
                  :alt="comment.userName"
                  class="comment-avatar-img"
                />
                <span v-else class="comment-avatar-text">
                  {{ (comment.userName || 'U').charAt(0).toUpperCase() }}
                </span>
                <span class="comment-user">{{ comment.userName || '未知用户' }}</span>
                <span class="comment-time">{{ formatCommentTime(comment.createdAt) }}</span>
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
                <p class="upload-hint">支持批量上传文件和整个文件夹，单个文件不超过 50MB</p>
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
                    <span class="file-icon">{{ item.isFolder ? '📁' : '📄' }}</span>
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
  content: '📂';
  font-size: 20px;
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
  color: #0059b3;
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
  gap: 12px;
  margin-bottom: 8px;
}

.comment-avatar-img {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #e0e0e0;
}

.comment-avatar-text {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #0059b3;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;
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
  border-color: #0059b3;
  color: #0059b3;
  background-color: #f0f7ff;
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
  background-color: #0059b3;
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
  border-color: #0059b3;
  background: linear-gradient(135deg, #f0f7ff 0%, #e8f4ff 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 89, 179, 0.1);
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
  color: #0059b3;
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
  background: linear-gradient(90deg, #0059b3, #0077cc, #0088ff);
  background-size: 200% 100%;
  animation: progressShine 2s ease-in-out infinite;
  transition: width 0.3s ease;
  border-radius: 4px;
  box-shadow: 0 0 10px rgba(0, 89, 179, 0.3);
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
  color: #0059b3;
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
