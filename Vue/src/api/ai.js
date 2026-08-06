import request from '@/utils/request'

// 获取会话列表
export function getConversations() {
  return request({
    url: '/ai/conversations',
    method: 'get'
  })
}

// 获取会话历史（仅 user/assistant）
export function getConversationMessages(id) {
  return request({
    url: `/ai/conversations/${id}/messages`,
    method: 'get'
  })
}

// 删除会话
export function deleteConversation(id) {
  return request({
    url: `/ai/conversations/${id}`,
    method: 'delete'
  })
}

/**
 * 流式对话（SSE）。
 * axios 有 60s 超时不适用于长对话，这里用原生 fetch + ReadableStream。
 *
 * @param {object} opts
 * @param {number|null} opts.conversationId
 * @param {string} opts.message
 * @param {(delta:string)=>void} opts.onDelta  内容增量
 * @param {(conversationId:number|null)=>void} opts.onDone 回答结束
 * @param {(msg:string)=>void} opts.onError    错误
 * @param {AbortSignal} [opts.signal]          取消信号
 */
export async function streamChat({ conversationId, message, onDelta, onDone, onError, signal }) {
  try {
    const res = await fetch('/api/ai/chat', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ conversationId: conversationId ?? null, message }),
      signal
    })

    if (!res.ok) {
      let msg = `请求失败 (${res.status})`
      try {
        const data = await res.json()
        msg = data?.message || msg
      } catch { /* ignore */ }
      onError(msg)
      return
    }

    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    const handleLine = (line) => {
      const trimmed = line.trim()
      if (!trimmed.startsWith('data:')) return
      const payload = trimmed.slice(5).trim()
      if (!payload) return
      try {
        const evt = JSON.parse(payload)
        if (evt.type === 'delta') onDelta(evt.content || '')
        else if (evt.type === 'done') onDone(evt.conversationId ?? null)
        else if (evt.type === 'error') onError(evt.message || '服务异常')
      } catch { /* 忽略无法解析的碎片 */ }
    }

    for (;;) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      let idx
      while ((idx = buffer.indexOf('\n')) >= 0) {
        handleLine(buffer.slice(0, idx))
        buffer = buffer.slice(idx + 1)
      }
    }
    if (buffer.trim()) handleLine(buffer)
  } catch (e) {
    if (e?.name === 'AbortError') {
      onDone(null)
    } else {
      onError('网络错误，请稍后重试')
    }
  }
}
