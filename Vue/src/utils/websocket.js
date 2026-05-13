import { log, error as logError, warn } from './logger'

/**
 * WebSocket 客户端工具类
 * 用于实时通知推送
 */
class NotificationWebSocket {
  constructor() {
    this.ws = null
    this.reconnectTimer = null
    this.heartbeatTimer = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 10
    this.reconnectInterval = 3000 // 初始重连间隔 3 秒
    this.heartbeatInterval = 30000 // 心跳间隔 30 秒
    this.messageHandlers = []
    this.isConnected = false
  }

  /**
   * 连接 WebSocket
   */
  connect() {
    // 如果已经连接，先关闭
    if (this.ws && this.isConnected) {
      log('WebSocket 已连接，无需重复连接')
      return
    }

    try {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const wsUrl = `${protocol}//${window.location.host}/api/ws/notifications`
      
      log('正在连接 WebSocket:', wsUrl)
      
      this.ws = new WebSocket(wsUrl)
      
      // 连接成功
      this.ws.onopen = () => {
        this.isConnected = true
        this.reconnectAttempts = 0
        this.reconnectInterval = 3000
        
        // 启动心跳
        this.startHeartbeat()
        
        // 通知所有监听器
        this.emit('connected', { message: 'WebSocket 连接成功' })
      }
      
      // 接收消息
      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          
          // 根据消息类型处理
          if (data.type === 'notification') {
            // 新通知
            this.emit('notification', data)
          } else if (data.type === 'heartbeat' || data.type === 'heartbeat_ack') {
            // 心跳响应（静默处理）
          } else {
            // 其他消息
            this.emit('message', data)
          }
        } catch (err) {
          logError('解析 WebSocket 消息失败:', err)
        }
      }
      
      // 连接关闭
      this.ws.onclose = (event) => {
        this.isConnected = false
        this.stopHeartbeat()
        
        // 通知所有监听器
        this.emit('disconnected', { code: event.code, reason: event.reason })
        
        // 尝试重连
        this.attemptReconnect()
      }
      
      // 连接错误
      this.ws.onerror = (error) => {
        logError('WebSocket 错误:', error)
        this.emit('error', error)
      }
      
    } catch (err) {
      logError('创建 WebSocket 连接失败:', err)
      this.attemptReconnect()
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    // 清除定时器
    this.stopHeartbeat()
    this.clearReconnectTimer()
    
    // 关闭连接
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    
    this.isConnected = false
    this.reconnectAttempts = 0
  }

  /**
   * 发送消息
   */
  send(data) {
    if (this.ws && this.isConnected) {
      this.ws.send(JSON.stringify(data))
      // 不记录日志，避免控制台刷屏
    } else {
      warn('WebSocket 未连接，无法发送消息')
    }
  }

  /**
   * 发送通知确认
   * @param {number} notificationId - 通知 ID
   */
  ackNotification(notificationId) {
    if (!notificationId) {
      warn('通知 ID 不能为空')
      return
    }
    
    this.send({
      type: 'ack',
      notificationId: notificationId,
      timestamp: Date.now()
    })
    
    log('已发送通知确认:', notificationId)
  }

  /**
   * 注册消息监听器
   */
  on(event, handler) {
    // 检查是否已经注册过相同的监听器
    const exists = this.messageHandlers.some(
      h => h.event === event && h.handler === handler
    )
    
    if (!exists) {
      this.messageHandlers.push({ event, handler })
    }
  }

  /**
   * 移除消息监听器
   */
  off(event, handler) {
    this.messageHandlers = this.messageHandlers.filter(
      h => !(h.event === event && h.handler === handler)
    )
  }

  /**
   * 触发事件
   */
  emit(event, data) {
    this.messageHandlers.forEach(({ event: e, handler }) => {
      if (e === event) {
        handler(data)
      }
    })
  }

  /**
   * 尝试重连
   */
  attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      logError('WebSocket 重连次数已达上限，停止重连')
      return
    }

    // 指数退避策略
    const delay = this.reconnectInterval * Math.pow(1.5, this.reconnectAttempts)
    this.reconnectAttempts++
    
    this.reconnectTimer = setTimeout(() => {
      this.connect()
    }, delay)
  }

  /**
   * 清除重连定时器
   */
  clearReconnectTimer() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  /**
   * 启动心跳
   */
  startHeartbeat() {
    this.stopHeartbeat()
    
    this.heartbeatTimer = setInterval(() => {
      if (this.ws && this.isConnected) {
        this.send({ type: 'heartbeat', timestamp: Date.now() })
      }
    }, this.heartbeatInterval)
  }

  /**
   * 停止心跳
   */
  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /**
   * 获取连接状态
   */
  getConnectionStatus() {
    return {
      isConnected: this.isConnected,
      reconnectAttempts: this.reconnectAttempts,
      hasListeners: this.messageHandlers.length > 0
    }
  }
}

// 创建单例实例
const notificationWS = new NotificationWebSocket()

export default notificationWS
