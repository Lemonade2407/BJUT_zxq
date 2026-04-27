import request from '@/utils/request'

/**
 * 标签相关 API
 */

// 获取所有标签
export function getTags() {
  return request({
    url: '/tags',
    method: 'get'
  })
}

// 根据 ID 获取标签
export function getTagById(id) {
  return request({
    url: `/tags/${id}`,
    method: 'get'
  })
}

// 搜索标签
export function searchTags(name) {
  return request({
    url: '/tags/search/name',
    method: 'get',
    params: { name }
  })
}

// 获取热门标签
export function getHotTags(limit = 10) {
  return request({
    url: '/tags/hot',
    method: 'get',
    params: { limit }
  })
}

// 根据分组获取标签
export function getTagsByCategory(category) {
  return request({
    url: `/tags/category/${category}`,
    method: 'get'
  })
}
