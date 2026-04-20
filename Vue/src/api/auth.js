import request from '@/utils/request'

/**
 * 认证相关 API
 */

// TODO: 添加 API 请求缓存，减少重复请求
// TODO: 添加 API 错误重试机制

// 用户登录
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

// 用户注册
export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

// 获取当前用户信息
export function getCurrentUser() {
  return request({
    url: '/auth/me',
    method: 'get'
  })
}

// 退出登录
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

// 更新用户信息
export function updateProfile(data) {
  return request({
    url: '/auth/user/profile',
    method: 'put',
    data
  })
}

// 修改密码
export function changePassword(data) {
  return request({
    url: '/auth/user/password',
    method: 'put',
    data
  })
}

// 上传头像
export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  
  return request({
    url: '/auth/avatar/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 根据 ID 获取用户信息
export function getUserById(userId) {
  return request({
    url: `/auth/user/${userId}`,
    method: 'get'
  })
}
