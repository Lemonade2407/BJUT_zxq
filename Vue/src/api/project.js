import request from '@/utils/request'

/**
 * 项目相关 API
 */

// TODO: 添加项目列表缓存策略（避免频繁请求）
// TODO: 添加搜索防抖功能

// 获取项目列表（所有公开项目）
export function getProjectList(params) {
  return request({
    url: '/projects/list',
    method: 'get',
    params
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

// 根据分类查询项目
export function getProjectsByCategory(categoryId, params = {}) {
  return request({
    url: `/projects/category/${categoryId}`,
    method: 'get',
    params: {
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
    url: `/projects/${projectId}/watch`,
    method: 'post',
    data: {
      notificationType
    }
  })
}

// 取消收藏（取消关注）
export function unwatchProject(projectId) {
  return request({
    url: `/projects/${projectId}/watch`,
    method: 'delete'
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
export function downloadProject(projectId) {
  // 使用原生 fetch 处理文件下载
  const token = localStorage.getItem('token')
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
  
  return fetch(`${baseUrl}/projects/${projectId}/download`, {
    method: 'GET',
    headers: {
      'Authorization': token ? `Bearer ${token}` : ''
    }
  }).then(response => {
    if (!response.ok) {
      throw new Error('下载失败')
    }
    return response.blob()
  }).then(blob => {
    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `project_${projectId}.zip`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  })
}

// 上传单个文件
export function uploadFile(projectId, file, parentId = null) {
  const formData = new FormData()
  formData.append('file', file)
  if (parentId !== null) {
    formData.append('parentId', parentId)
  }
  
  return request({
    url: `/projects/${projectId}/files/upload`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 批量上传文件
export function uploadFiles(projectId, files, parentId = null) {
  const formData = new FormData()
  // 添加多个文件
  files.forEach(file => {
    formData.append('files', file)
  })
  if (parentId !== null) {
    formData.append('parentId', parentId)
  }
  
  return request({
    url: `/projects/${projectId}/files/upload-batch`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
