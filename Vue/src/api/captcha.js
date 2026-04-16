import request from '@/utils/request'

/**
 * 验证码和密码强度 API
 */

// 获取图形验证码
export function getCaptcha() {
  return request({
    url: '/captcha/image',
    method: 'get'
  })
}

// 验证图形验证码
export function verifyCaptcha(data) {
  return request({
    url: '/captcha/verify',
    method: 'post',
    data
  })
}

// 评估密码强度
export function checkPasswordStrength(password) {
  return request({
    url: '/captcha/password-strength',
    method: 'post',
    data: { password }
  })
}
