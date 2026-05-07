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

