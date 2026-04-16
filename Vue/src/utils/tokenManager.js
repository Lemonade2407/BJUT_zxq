/**
 * Token 管理工具
 */
class TokenManager {
  constructor() {
    this.tokenKey = 'auth_token'
    this.userInfoKey = 'user_info'
    this.refreshTimer = null
  }

  /**
   * 保存 Token
   */
  // TODO: 考虑使用 sessionStorage 或 cookie，提高安全性
  saveToken(token) {
    localStorage.setItem(this.tokenKey, token)
  }

  /**
   * 获取 Token
   */
  getToken() {
    return localStorage.getItem(this.tokenKey)
  }

  /**
   * 删除 Token
   */
  removeToken() {
    localStorage.removeItem(this.tokenKey)
    localStorage.removeItem(this.userInfoKey)
    this.stopAutoRefresh()
  }

  /**
   * 保存用户信息
   */
  saveUserInfo(userInfo) {
    localStorage.setItem(this.userInfoKey, JSON.stringify(userInfo))
  }

  /**
   * 获取用户信息
   */
  getUserInfo() {
    const info = localStorage.getItem(this.userInfoKey)
    return info ? JSON.parse(info) : null
  }

  /**
   * 检查是否已登录
   */
  isLoggedIn() {
    return !!this.getToken()
  }

  /**
   * 更新 Token（从响应头获取新 Token）
   */
  updateTokenFromResponse(response) {
    const newToken = response.headers.get('X-New-Token')
    if (newToken) {
      console.log('Token 已自动刷新')
      this.saveToken(newToken)
      return true
    }
    return false
  }

  /**
   * 手动刷新 Token
   */
  async refreshToken() {
    const token = this.getToken()
    if (!token) {
      console.error('没有 Token，无法刷新')
      return false
    }

    try {
      const response = await fetch('/api/auth/refresh', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        }
      })

      const result = await response.json()
      
      if (result.code === 200) {
        this.saveToken(result.data.token)
        console.log('Token 刷新成功')
        return true
      } else {
        console.error('Token 刷新失败:', result.message)
        this.handleTokenExpired()
        return false
      }
    } catch (error) {
      console.error('Token 刷新异常:', error)
      return false
    }
  }

  /**
   * 启动自动刷新定时器
   * 每 30 分钟检查一次，如果 Token 即将过期则刷新
   */
  // TODO: 优化刷新策略，根据 Token 剩余时间动态调整检查频率
  startAutoRefresh() {
    this.stopAutoRefresh()
    
    // 每 30 分钟检查一次
    this.refreshTimer = setInterval(async () => {
      const token = this.getToken()
      if (!token) {
        this.stopAutoRefresh()
        return
      }

      try {
        // TODO: 添加请求超时处理
        // 尝试请求一个需要认证的接口，检查是否需要刷新
        const response = await fetch('/api/auth/me', {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })

        // 如果返回 401，说明 Token 已过期
        if (response.status === 401) {
          console.warn('Token 已过期，请重新登录')
          this.handleTokenExpired()
          return
        }

        // 检查响应头是否有新 Token
        this.updateTokenFromResponse(response)
      } catch (error) {
        console.error('自动刷新检查失败:', error)
      }
    }, 30 * 60 * 1000) // 30 分钟

    console.log('Token 自动刷新已启动')
  }

  /**
   * 停止自动刷新
   */
  stopAutoRefresh() {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer)
      this.refreshTimer = null
      console.log('Token 自动刷新已停止')
    }
  }

  /**
   * 处理 Token 过期
   */
  handleTokenExpired() {
    console.log('处理 Token 过期')
    this.removeToken()
    
    // 跳转到登录页
    if (window.location.pathname !== '/login') {
      window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname)
    }
  }

  /**
   * 创建带有认证信息的 fetch 请求
   */
  authenticatedFetch(url, options = {}) {
    const token = this.getToken()
    
    const headers = {
      'Content-Type': 'application/json',
      ...options.headers
    }

    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    return fetch(url, {
      ...options,
      headers
    }).then(async response => {
      // 检查是否有新 Token
      this.updateTokenFromResponse(response)

      // 如果返回 401，处理 Token 过期
      if (response.status === 401) {
        this.handleTokenExpired()
        throw new Error('Token 已过期，请重新登录')
      }

      return response
    })
  }
}

// 导出单例
export default new TokenManager()
