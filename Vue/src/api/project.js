import request from '@/utils/request'
import tokenManager from '@/utils/tokenManager'

/**
 * 项目相关 API
 */
// 获取项目列表（所有公开项目）
export function getProjectList(params) {
  return request({
    url: '/projects/list',
    method: 'get',
    params
  })
}

// 获取所有项目类型（字典）
export function getProjectTypes() {
  return request({
    url: '/projects/types',
    method: 'get'
  })
}

// 筛选项目（按班级、课程等，用于教学管理）
export function filterProjects(params = {}) {
  return request({
    url: '/projects/filter',
    method: 'get',
    params: {
      className: params.className || '',
      courseName: params.courseName || '',
      projectType: params.projectType || '',
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 10
    }
  })
}

// 获取筛选条件下的所有项目ID（用于批量下载）
export function getFilteredProjectIds(params = {}) {
  return request({
    url: '/projects/filter/ids',
    method: 'get',
    params: {
      className: params.className || '',
      courseName: params.courseName || '',
      projectType: params.projectType || ''
    }
  })
}

// 获取项目详情
export function getProjectDetail(id) {
  return request({
    url: `/projects/${id}`,
    method: 'get'
  })
}

// 创建项目
export function createProject(data) {
  return request({
    url: '/projects/create',
    method: 'post',
    data
  })
}

// 更新项目
export function updateProject(id, data) {
  return request({
    url: `/projects/${id}`,
    method: 'put',
    data
  })
}

// 删除项目
export function deleteProject(id) {
  return request({
    url: `/projects/${id}`,
    method: 'delete'
  })
}

// 获取当前用户的项目（支持分页）
export function getMyProjects(params = {}) {
  return request({
    url: '/projects/my',
    method: 'get',
    params: {
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 10
    }
  })
}

// 搜索项目（按名称）
export function searchProjects(name, params = {}) {
  return request({
    url: '/projects/search/name',
    method: 'get',
    params: {
      name,
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 10
    }
  })
}

// 根据标签查询项目
export function getProjectsByTag(tagId, params = {}) {
  return request({
    url: `/projects/tag/${tagId}`,
    method: 'get',
    params: {
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 10
    }
  })
}

// 点赞项目
export function starProject(projectId) {
  return request({
    url: `/projects/${projectId}/star`,
    method: 'post'
  })
}

// 取消点赞
export function unstarProject(projectId) {
  return request({
    url: `/projects/${projectId}/star`,
    method: 'delete'
  })
}

// 收藏项目（关注）
export function watchProject(projectId, notificationType = 1) {
  return request({
    url: `/watch/${projectId}`,
    method: 'post',
    data: {
      notificationType
    }
  })
}

// 取消收藏（取消关注）
export function unwatchProject(projectId) {
  return request({
    url: `/watch/${projectId}`,
    method: 'delete'
  })
}

// 获取用户收藏的项目列表
export function getMyWatchedProjects() {
  return request({
    url: '/watch/my',
    method: 'get'
  })
}

// 获取热门项目（按浏览量排序）
export function getTrendingProjects(params = {}) {
  return request({
    url: '/projects/trending',
    method: 'get',
    params: {
      limit: params.limit || 10
    }
  })
}

// 下载项目（打包为 ZIP）
export async function downloadProject(projectId) {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
  const response = await fetch(`${baseUrl}/projects/${projectId}/download`, {
    credentials: 'include'
  })
  if (!response.ok) throw new Error(`下载失败: ${response.status}`)
  const blob = await response.blob()
  const downloadUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = downloadUrl
  link.download = `project_${projectId}.zip`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(downloadUrl)
}

// 批量下载学生项目（教师专用）
export async function batchDownloadProjects(data) {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
  const response = await fetch(`${baseUrl}/projects/batch-download`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  if (!response.ok) throw new Error(`下载失败: ${response.status}`)
  const contentDisposition = response.headers.get('Content-Disposition')
  let fileName = `${data.className}_${data.courseName}.zip`
  if (contentDisposition) {
    const m = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
    if (m && m[1]) fileName = decodeURIComponent(m[1])
  }
  const blob = await response.blob()
  const downloadUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = downloadUrl
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(downloadUrl)
}

// 获取项目的所有文件（用于构建完整的树形结构）
export function getAllProjectFiles(projectId) {
  return request({
    url: `/projects/${projectId}/files/all`,
    method: 'get'
  })
}

// ==================== 项目文档相关 API ====================

// 上传项目文档
export function uploadProjectDocument(projectId, file, onProgress = null) {
  const formData = new FormData()
  formData.append('file', file)
  
  return request({
    url: `/projects/${projectId}/files/document/upload`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 120000, // 文档上传超时时间设置为2分钟（120秒）
    onUploadProgress: onProgress ? (progressEvent) => {
      const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
      onProgress(percentCompleted)
    } : undefined
  })
}

// 删除项目文档
export function deleteProjectDocument(projectId) {
  return request({
    url: `/projects/${projectId}/files/document`,
    method: 'delete'
  })
}

// 获取项目文档 URL
export function getProjectDocument(projectId) {
  return request({
    url: `/projects/${projectId}/files/document`,
    method: 'get'
  })
}

// OSS 直传 — 获取上传签名
export function getUploadSignatures(files) {
  return request({
    url: '/oss/upload-signatures',
    method: 'post',
    data: { files }
  })
}

// OSS 直传 — 通过 POST 表单上传到 OSS
// onProgress 回调参数为 (loadedBytes, totalBytes)
export function uploadToPresignedUrl(sig, file, onProgress) {
  return new Promise((resolve, reject) => {
    const formData = new FormData()
    formData.append('OSSAccessKeyId', sig.accessKeyId)
    formData.append('policy', sig.policy)
    formData.append('signature', sig.signature)
    formData.append('key', sig.objectKey)
    formData.append('success_action_status', '200')
    formData.append('file', file, file.name)

    const xhr = new XMLHttpRequest()
    xhr.open('POST', sig.host)
    if (onProgress) {
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) onProgress(e.loaded, e.total)
      }
    }
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) resolve()
      else reject(new Error(`Upload failed: ${xhr.status}`))
    }
    xhr.onerror = () => reject(new Error('网络错误'))
    xhr.send(formData)
  })
}

// OSS 直传 — 确认上传完成
export function confirmUpload(projectId, files) {
  return request({
    url: `/projects/${projectId}/files/confirm`,
    method: 'post',
    data: files
  })
}

// OSS 直传 — 删除项目所有文件
export function deleteAllProjectFiles(projectId) {
  return request({
    url: `/projects/${projectId}/files/all`,
    method: 'delete'
  })
}
