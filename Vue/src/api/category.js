import request from '@/utils/request'

/**
 * 分类相关 API
 */

// 获取所有分类
export function getCategories(params = {}) {
  return request({
    url: '/categories',
    method: 'get',
    params
  })
}

// 根据 ID 获取分类
export function getCategoryById(id) {
  return request({
    url: `/categories/${id}`,
    method: 'get'
  })
}
