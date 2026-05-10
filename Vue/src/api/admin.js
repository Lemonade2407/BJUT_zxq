import request from '@/utils/request'

// ===== 用户管理 =====
export function getAdminUsers(params) {
  return request({ url: '/admin/users', method: 'get', params })
}
export function searchAdminUsers(keyword, params) {
  return request({ url: '/admin/users/search', method: 'get', params: { keyword, ...params } })
}
export function banUser(id) {
  return request({ url: `/admin/users/${id}/ban`, method: 'put' })
}
export function unbanUser(id) {
  return request({ url: `/admin/users/${id}/unban`, method: 'put' })
}
export function setUserRole(id, roleCode) {
  return request({ url: `/admin/users/${id}/role`, method: 'put', params: { roleCode } })
}
export function updateAdminUser(id, data) {
  return request({ url: `/admin/users/${id}`, method: 'put', data })
}
export function deleteAdminUser(id) {
  return request({ url: `/admin/users/${id}`, method: 'delete' })
}

// ===== 项目管理 =====
export function updateAdminProject(id, data) {
  return request({ url: `/admin/projects/${id}`, method: 'put', data })
}
export function deleteAdminProject(id) {
  return request({ url: `/admin/projects/${id}`, method: 'delete' })
}
