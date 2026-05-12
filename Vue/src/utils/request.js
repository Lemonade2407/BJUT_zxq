import axios from 'axios'
import tokenManager from './tokenManager'

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api', // Vite 代理配置：/api -> http://localhost:8080/api
  timeout: 60000, // 请求超时时间（1min，文件上传需要更长时间）
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 使用 tokenManager 获取 token
    const token = tokenManager.getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    
    // 如果返回的状态码不是 200，则认为是错误
    if (res.code !== 200) {
      console.error('API 错误:', res.message)
      
      // 401: 未授权，跳转到登录页（排除公开接口）
      if (res.code === 401) {
        // 检查是否是公开接口（不需要登录）
        const publicApis = ['/captcha/', '/auth/login', '/auth/register']
        const isPublicApi = publicApis.some(api => response.config.url.includes(api))
        
        if (!isPublicApi) {
          tokenManager.removeToken()
          window.location.href = '/login'
        }
      }
      
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    
    return res
  },
  error => {
    console.error('响应错误:', error.message)
    
    let errorMessage = '请求失败'
    
    if (error.response) {
      // 服务器返回了错误状态码
      const status = error.response.status
      const data = error.response.data
      
      switch (status) {
        case 401:
          // 检查是否是公开接口
          const publicApis = ['/captcha/', '/auth/login', '/auth/register']
          const isPublicApi = publicApis.some(api => error.config?.url?.includes(api))
          
          if (!isPublicApi) {
            errorMessage = '未授权，请重新登录'
            tokenManager.removeToken()
            window.location.href = '/login'
          } else {
            errorMessage = data?.message || '请求失败'
          }
          break
        case 403:
          errorMessage = data?.message || '拒绝访问'
          break
        case 404:
          errorMessage = '请求的资源不存在'
          break
        case 500:
          // 优先使用后端返回的错误消息
          errorMessage = data?.message || '服务器内部错误'
          console.error('服务器错误详情:', data)
          break
        default:
          errorMessage = data?.message || `请求失败 (${status})`
      }
    } else if (error.code === 'ECONNABORTED') {
      errorMessage = '请求超时，请检查网络连接'
    } else if (error.message.includes('Network Error')) {
      errorMessage = '网络连接失败，请检查后端服务是否启动'
    } else {
      errorMessage = error.message || '网络连接失败，请检查网络'
    }
    
    // 不在这里显示alert，让调用方处理错误提示
    return Promise.reject(new Error(errorMessage))
  }
)

export default request
