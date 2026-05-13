/**
 * Token 管理工具
 * Token 通过 httpOnly Cookie 存储，前端无法读写，XSS 安全
 * 用户信息（非敏感）存在 sessionStorage，关标签页自动清除
 */
class TokenManager {
  constructor() {
    this.userInfoKey = 'user_info'
  }

  // Token 不再由前端管理（httpOnly Cookie）
  // 保留以下方法用于兼容：登录状态通过请求 /api/auth/me 判断

  /**
   * @deprecated Token 已在 httpOnly Cookie 中，前端无需读取
   */
  getToken() {
    return '' // 后端从 Cookie 读，前端不需要
  }

  /**
   * 清除用户信息
   */
  removeToken() {
    sessionStorage.removeItem(this.userInfoKey)
  }

  /**
   * 保存用户信息（存 sessionStorage，关标签页清空）
   */
  saveUserInfo(userInfo) {
    sessionStorage.setItem(this.userInfoKey, JSON.stringify(userInfo))
  }

  /**
   * 获取用户信息
   */
  getUserInfo() {
    const info = sessionStorage.getItem(this.userInfoKey)
    if (!info || info === 'undefined' || info === 'null') return null
    try {
      return JSON.parse(info)
    } catch {
      sessionStorage.removeItem(this.userInfoKey)
      return null
    }
  }

  /**
   * 检查是否已登录（通过调用 /api/auth/me 验证）
   */
  isLoggedIn() {
    return !!this.getUserInfo()
  }

  /**
   * 处理 Token 过期（收到 401 时调用）
   */
  handleTokenExpired() {
    sessionStorage.removeItem(this.userInfoKey)
    if (window.location.pathname !== '/login') {
      window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname)
    }
  }
}

export default new TokenManager()
