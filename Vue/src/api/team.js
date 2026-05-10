import request from '@/utils/request'

// 获取组队列表
export function getTeams(params = {}) {
  return request({
    url: '/teams',
    method: 'get',
    params: {
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 12,
      tag: params.tag,
      status: params.status,
      courseName: params.courseName
    }
  })
}

// 获取组队详情
export function getTeamDetail(id) {
  return request({
    url: `/teams/${id}`,
    method: 'get'
  })
}

// 创建组队
export function createTeam(data) {
  return request({
    url: '/teams',
    method: 'post',
    data
  })
}

// 更新组队
export function updateTeam(id, data) {
  return request({
    url: `/teams/${id}`,
    method: 'put',
    data
  })
}

// 删除组队
export function deleteTeam(id) {
  return request({
    url: `/teams/${id}`,
    method: 'delete'
  })
}

// 获取我的组队
export function getMyTeams() {
  return request({
    url: '/teams/mine',
    method: 'get'
  })
}

// ==================== 管理员接口 ====================

// 管理员获取所有组队
export function getAdminTeams(params = {}) {
  return request({
    url: '/admin/teams',
    method: 'get',
    params: {
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 20,
      tag: params.tag,
      status: params.status
    }
  })
}

// 管理员删除组队
export function adminDeleteTeam(id) {
  return request({
    url: `/admin/teams/${id}`,
    method: 'delete'
  })
}

// 管理员更新组队状态
export function updateTeamStatus(id, status) {
  return request({
    url: `/admin/teams/${id}/status`,
    method: 'put',
    params: { status }
  })
}

// ==================== 入队申请 ====================

// 申请入队
export function applyToTeam(teamId, message) {
  return request({
    url: `/teams/${teamId}/apply`,
    method: 'post',
    data: { message: message || '' }
  })
}

// 检查是否已申请
export function hasAppliedToTeam(teamId) {
  return request({
    url: `/teams/${teamId}/applied`,
    method: 'get'
  })
}

// 获取组队的申请列表（组长）
export function getTeamApplications(teamId) {
  return request({
    url: `/teams/${teamId}/applications`,
    method: 'get'
  })
}

// 通过申请
export function approveApplication(appId) {
  return request({
    url: `/teams/applications/${appId}/approve`,
    method: 'put'
  })
}

// 拒绝申请
export function rejectApplication(appId) {
  return request({
    url: `/teams/applications/${appId}/reject`,
    method: 'put'
  })
}
