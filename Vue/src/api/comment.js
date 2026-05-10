import request from '@/utils/request'

/**
 * 评论相关 API
 */

// 获取项目评论列表
export function getProjectComments(projectId, params = {}) {
  return request({
    url: `/projects/${projectId}/comments`,
    method: 'get',
    params: {
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 20
    }
  })
}

// 发表评论
export function createComment(projectId, data) {
  return request({
    url: `/projects/${projectId}/comments`,
    method: 'post',
    data
  })
}

// 删除评论
export function deleteComment(commentId) {
  return request({
    url: `/comments/${commentId}`,
    method: 'delete'
  })
}

// ==================== 管理员接口 ====================

// 获取所有评论（管理员，分页）
export function getAdminComments(params = {}) {
  return request({
    url: '/admin/comments',
    method: 'get',
    params: {
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 20,
      status: params.status
    }
  })
}

// 搜索评论（管理员）
export function searchAdminComments(keyword, params = {}) {
  return request({
    url: '/admin/comments/search',
    method: 'get',
    params: {
      keyword,
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 20
    }
  })
}

// 管理员删除评论（物理删除）
export function adminDeleteComment(commentId) {
  return request({
    url: `/admin/comments/${commentId}`,
    method: 'delete'
  })
}
