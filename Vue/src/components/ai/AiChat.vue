<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { streamChat, getConversations, getConversationMessages, deleteConversation } from '@/api/ai'
import { toast } from '@/utils/toast'

const conversations = ref([])
const currentId = ref(null)
const messages = ref([])
const input = ref('')
const loading = ref(false)
const listRef = ref(null)
const inputRef = ref(null)

let abortCtrl = null
let markedFn = null
let mdTimer = null

const suggestions = [
  { icon: '🎯', text: '帮我推荐一个 Java 课设选题' },
  { icon: '👥', text: '我想参加竞赛，帮我找适合我的队伍' },
  { icon: '🐍', text: '帮我选个适合新手的 Python 选题' },
  { icon: '⏰', text: '课设快截止了，推荐工作量小的题目' }
]

// ==================== markdown ====================

async function toHtml(text) {
  if (!markedFn) {
    const mod = await import('marked')
    markedFn = mod.marked
  }
  try {
    return markedFn(text || '')
  } catch {
    return escapeHtml(text || '')
  }
}

function escapeHtml(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

async function renderAssistant(m, immediate) {
  if (mdTimer && !immediate) return
  mdTimer = setTimeout(async () => {
    mdTimer = null
    m.html = await toHtml(m.content)
    await scrollBottom()
  }, immediate ? 0 : 80)
}

// ==================== 会话管理 ====================

async function loadConversations() {
  try {
    const res = await getConversations()
    conversations.value = res.data || []
  } catch (e) {
    toast.error(e.message)
  }
}

async function openConversation(id) {
  abortCurrent()
  currentId.value = id
  loading.value = true
  try {
    const res = await getConversationMessages(id)
    messages.value = (res.data || []).map(m => reactive({ role: m.role, content: m.content || '', html: '' }))
    for (const m of messages.value) {
      if (m.role === 'assistant') m.html = await toHtml(m.content)
    }
    await scrollBottom()
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

function newChat() {
  abortCurrent()
  currentId.value = null
  messages.value = []
  input.value = ''
  nextTick(() => inputRef.value?.focus())
}

async function removeConversation(id, ev) {
  ev.stopPropagation()
  try {
    await deleteConversation(id)
    if (currentId.value === id) newChat()
    await loadConversations()
  } catch (e) {
    toast.error(e.message)
  }
}

function useSuggestion(text) {
  input.value = text
  send()
}

// ==================== 发送 ====================

function send() {
  const text = input.value.trim()
  if (!text || loading.value) return
  input.value = ''

  messages.value.push({ role: 'user', content: text, html: '' })
  // 必须用 reactive：push 进响应式数组后局部变量拿到的仍是原始对象，
  // 直接改 content 不会触发更新 → 导致整段一次性渲染（非流式）。
  const ai = reactive({ role: 'assistant', content: '', html: '' })
  messages.value.push(ai)
  loading.value = true
  scrollBottom()

  abortCtrl = new AbortController()
  streamChat({
    conversationId: currentId.value,
    message: text,
    signal: abortCtrl.signal,
    onDelta: (delta) => {
      ai.content += delta
      renderAssistant(ai, false)
      scrollBottom()
    },
    onDone: (conversationId) => {
      if (conversationId && !currentId.value) currentId.value = conversationId
      finish(ai, null)
    },
    onError: (msg) => {
      finish(ai, msg)
    }
  })
}

function stopGenerating() {
  // 中止 fetch → 后端检测到断连即停止 DeepSeek 拉流
  if (abortCtrl) {
    abortCtrl.abort()
    abortCtrl = null
  }
  if (mdTimer) {
    clearTimeout(mdTimer)
    mdTimer = null
  }
  loading.value = false
  // 保留已生成的部分内容并渲染
  const last = messages.value[messages.value.length - 1]
  if (last && last.role === 'assistant' && last.content) {
    renderAssistant(last, true)
  }
  nextTick(() => inputRef.value?.focus())
}

function finish(ai, errMsg) {
  if (mdTimer) {
    clearTimeout(mdTimer)
    mdTimer = null
  }
  if (errMsg) {
    ai.html = `<div class="ai-error">${escapeHtml(errMsg)}</div>`
  } else {
    ai.html = ai.html || ''
  }
  loading.value = false
  abortCtrl = null
  loadConversations()
  nextTick(() => {
    renderAssistant(ai, true)
    inputRef.value?.focus()
  })
}

function abortCurrent() {
  if (abortCtrl) {
    abortCtrl.abort()
    abortCtrl = null
  }
  if (mdTimer) {
    clearTimeout(mdTimer)
    mdTimer = null
  }
  loading.value = false
}

function scrollBottom() {
  nextTick(() => {
    if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight
  })
}

function handleEnter(e) {
  if (!e.shiftKey) {
    e.preventDefault()
    send()
  }
}

function autoGrow(e) {
  const el = e.target
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 160) + 'px'
}

onMounted(() => {
  loadConversations()
  nextTick(() => inputRef.value?.focus())
})

onBeforeUnmount(() => {
  abortCurrent()
})
</script>

<template>
  <div class="ai-page">
    <!-- ===== 会话侧栏 ===== -->
    <aside class="ai-sidebar">
      <div class="sidebar-top">
        <button class="new-chat-btn" @click="newChat">
          <svg viewBox="0 0 20 20" width="16" height="16" fill="currentColor">
            <path d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z"/>
          </svg>
          新对话
        </button>
      </div>
      <div class="conv-label">历史对话</div>
      <div class="conv-list">
        <div
          v-for="c in conversations"
          :key="c.id"
          class="conv-item"
          :class="{ active: c.id === currentId }"
          @click="openConversation(c.id)"
        >
          <svg class="conv-ico" viewBox="0 0 20 20" width="14" height="14" fill="currentColor">
            <path d="M2 5a2 2 0 012-2h12a2 2 0 012 2v8a2 2 0 01-2 2H6l-3 3V5z"/>
          </svg>
          <span class="conv-title">{{ c.title || '新对话' }}</span>
          <button class="conv-del" title="删除会话" @click="removeConversation(c.id, $event)">×</button>
        </div>
        <div v-if="conversations.length === 0" class="conv-empty">暂无历史会话</div>
      </div>
      <div class="sidebar-foot">
        <span>ProjecTree AI</span>
      </div>
    </aside>

    <!-- ===== 主聊天区 ===== -->
    <main class="ai-main">
      <header class="chat-header">
        <div class="chat-title">
          <span class="chat-logo">🤖</span>
          <span>AI 选题与组队助手</span>
        </div>
        <span class="chat-status" :class="{ busy: loading }">
          <i class="status-dot"></i>{{ loading ? '思考中…' : '在线' }}
        </span>
      </header>

      <div ref="listRef" class="msg-list">
        <!-- 空态 -->
        <div v-if="messages.length === 0" class="empty-state">
          <div class="empty-logo">🤖</div>
          <h2>你好，我是知享圈 AI 助手</h2>
          <p>帮你确定课设 / 竞赛选题，找到适合你的招人队伍</p>
          <div class="suggestions">
            <button
              v-for="s in suggestions"
              :key="s.text"
              class="suggestion-btn"
              @click="useSuggestion(s.text)"
            >
              <span class="sg-icon">{{ s.icon }}</span>
              <span>{{ s.text }}</span>
            </button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div v-else class="msg-wrap">
          <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role">
            <div class="msg-avatar" :class="m.role">{{ m.role === 'user' ? '我' : 'AI' }}</div>
            <div class="msg-bubble">
              <div v-if="m.role === 'assistant' && m.html" class="md-body" v-html="m.html"></div>
              <div
                v-else-if="m.role === 'assistant' && loading && i === messages.length - 1"
                class="thinking"
              >
                <span class="tdot"></span><span class="tdot"></span><span class="tdot"></span>
              </div>
              <div v-else class="plain-body">{{ m.content }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <footer class="input-bar">
        <div class="input-box">
          <textarea
            ref="inputRef"
            v-model="input"
            rows="1"
            class="ai-input"
            placeholder="描述你的需求，例如：我是计算机专业，想找适合我的竞赛队伍…"
            @keydown="handleEnter($event)"
            @input="autoGrow($event)"
          ></textarea>
          <button
            v-if="loading"
            class="send-btn stop-btn"
            title="停止生成"
            @click="stopGenerating"
          >
            <svg viewBox="0 0 20 20" width="16" height="16" fill="currentColor">
              <rect x="4.5" y="4.5" width="11" height="11" rx="2.5"/>
            </svg>
          </button>
          <button v-else class="send-btn" :disabled="!input.trim()" @click="send">
            <svg viewBox="0 0 20 20" width="18" height="18" fill="currentColor">
              <path d="M2.72 1.79l15.04 7.5a.75.75 0 010 1.34L2.72 18.13a.75.75 0 01-1.08-.86l1.76-5.9L9 10 3.4 8.63 1.64 2.73a.75.75 0 011.08-.86z"/>
            </svg>
          </button>
        </div>
        <div class="input-hint">Enter 发送 · Shift + Enter 换行 · 基于平台真实数据回答</div>
      </footer>
    </main>
  </div>
</template>

<style scoped>
.ai-page {
  /* 钉死为视口高度（60px = 固定头部偏移），内部滚动，不随 AI 输出变高 */
  height: calc(100vh - 60px);
  min-height: 0;
  display: grid;
  grid-template-columns: 280px 1fr;
  background: #ffffff;
  overflow: hidden;
}

/* ============ 侧栏 ============ */
.ai-sidebar {
  background: #f7f9f8;
  border-right: 1px solid #e6e8e7;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.sidebar-top {
  padding: 16px 14px 8px;
}

.new-chat-btn {
  width: 100%;
  padding: 11px 12px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.25);
}
.new-chat-btn:hover {
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.4);
  transform: translateY(-1px);
}
.new-chat-btn:active {
  transform: translateY(0);
}

.conv-label {
  padding: 10px 16px 6px;
  font-size: 12px;
  color: #9ca3af;
  font-weight: 500;
}

.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 10px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 0;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  color: #374151;
  border: 1px solid transparent;
}
.conv-item:hover {
  background: #eef2f0;
}
.conv-item.active {
  background: #ffffff;
  border-color: #d1fae5;
  box-shadow: 0 1px 4px rgba(16, 185, 129, 0.12);
  color: #047857;
}
.conv-ico {
  color: #9ca3af;
  flex-shrink: 0;
}
.conv-item.active .conv-ico {
  color: #10b981;
}
.conv-title {
  flex: 1;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.conv-del {
  border: none;
  background: transparent;
  color: #c0c4c3;
  font-size: 16px;
  cursor: pointer;
  line-height: 1;
  padding: 2px 5px;
  border-radius: 5px;
  opacity: 0;
  transition: all 0.15s;
}
.conv-item:hover .conv-del {
  opacity: 1;
}
.conv-del:hover {
  color: #ef4444;
  background: #fee2e2;
}
.conv-empty {
  color: #9ca3af;
  font-size: 13px;
  text-align: center;
  padding-top: 24px;
}

.sidebar-foot {
  padding: 10px 16px;
  border-top: 1px solid #eef0ef;
  font-size: 12px;
  color: #c0c4c3;
}

/* ============ 主区 ============ */
.ai-main {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #ffffff;
}

.chat-header {
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid #f0f1f0;
}
.chat-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}
.chat-logo {
  font-size: 20px;
}
.chat-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #6b7280;
}
.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #10b981;
}
.chat-status.busy .status-dot {
  background: #f59e0b;
  animation: pulse 1s infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

/* 消息区 */
.msg-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px 24px 8px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.msg-wrap {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 16px;
}

/* 空态 */
.empty-state {
  margin: auto;
  text-align: center;
  max-width: 520px;
  padding: 20px 0 40px;
}
.empty-logo {
  width: 72px;
  height: 72px;
  margin: 0 auto 20px;
  border-radius: 24px;
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  box-shadow: 0 8px 24px rgba(16, 185, 129, 0.15);
}
.empty-state h2 {
  font-size: 20px;
  color: #111827;
  margin: 0 0 8px;
}
.empty-state p {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 28px;
}
.suggestions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.suggestion-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #ffffff;
  color: #374151;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}
.suggestion-btn:hover {
  border-color: #10b981;
  background: #f0fdf4;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.12);
  transform: translateY(-1px);
}
.sg-icon {
  font-size: 18px;
  flex-shrink: 0;
}

/* 消息行 */
.msg-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.msg-row.user {
  flex-direction: row-reverse;
}
.msg-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
}
.msg-avatar.assistant {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #fff;
}
.msg-avatar.user {
  background: #f3f4f6;
  color: #4b5563;
  border: 1px solid #e5e7eb;
}
.msg-bubble {
  max-width: 78%;
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}
.msg-row.user .msg-bubble {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #fff;
  border-top-right-radius: 4px;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.2);
}
.msg-row.assistant .msg-bubble {
  background: #f9fafb;
  border: 1px solid #eef0ef;
  border-top-left-radius: 4px;
  color: #1f2937;
}

.plain-body {
  white-space: pre-wrap;
}

/* 思考动画 */
.thinking {
  display: flex;
  gap: 4px;
  padding: 4px 2px;
}
.tdot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #9ca3af;
  animation: blink 1.2s infinite;
}
.tdot:nth-child(2) { animation-delay: 0.2s; }
.tdot:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
  0%, 80%, 100% { opacity: 0.2; transform: translateY(0); }
  40% { opacity: 1; transform: translateY(-2px); }
}

.ai-error {
  color: #dc2626;
}

/* markdown 正文 */
.md-body :deep(p) { margin: 0 0 8px; }
.md-body :deep(ul), .md-body :deep(ol) { margin: 4px 0 8px; padding-left: 22px; }
.md-body :deep(h1), .md-body :deep(h2), .md-body :deep(h3), .md-body :deep(h4) {
  margin: 12px 0 6px;
  color: #065f46;
}
.md-body :deep(h1:first-child), .md-body :deep(h2:first-child), .md-body :deep(h3:first-child) { margin-top: 0; }
.md-body :deep(code) {
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}
.md-body :deep(pre) {
  background: #0f172a;
  color: #e2e8f0;
  padding: 12px 14px;
  border-radius: 10px;
  overflow-x: auto;
  margin: 8px 0;
}
.md-body :deep(pre code) {
  background: transparent;
  color: inherit;
  padding: 0;
}
.md-body :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
  width: 100%;
  font-size: 13px;
}
.md-body :deep(th), .md-body :deep(td) {
  border: 1px solid #e5e7eb;
  padding: 7px 10px;
  text-align: left;
}
.md-body :deep(th) { background: #f9fafb; }
.md-body :deep(a) { color: #059669; text-decoration: none; }
.md-body :deep(a):hover { text-decoration: underline; }
.md-body :deep(blockquote) {
  margin: 8px 0;
  padding: 6px 12px;
  border-left: 3px solid #10b981;
  background: #f0fdf4;
  border-radius: 0 8px 8px 0;
  color: #047857;
}
.md-body :deep(hr) { border: none; border-top: 1px solid #e5e7eb; margin: 12px 0; }
.md-body :deep(strong) { color: #111827; }

/* 输入区 */
.input-bar {
  flex-shrink: 0;
  padding: 12px 24px 16px;
  background: linear-gradient(to top, #ffffff 70%, rgba(255,255,255,0));
}
.input-box {
  width: 100%;
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 10px 10px 10px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #fff;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.input-box:focus-within {
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.12);
}
.ai-input {
  flex: 1;
  resize: none;
  border: none;
  outline: none;
  font-size: 14px;
  line-height: 1.6;
  font-family: inherit;
  max-height: 160px;
  padding: 6px 0;
  background: transparent;
}
.ai-input::placeholder {
  color: #9ca3af;
}
.send-btn {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.send-btn:hover:not(:disabled) {
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.35);
  transform: translateY(-1px);
}
.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.send-btn.stop-btn {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
}
.send-btn.stop-btn:hover {
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.35);
  transform: translateY(-1px);
}
.input-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #c0c4c3;
}

@media (max-width: 860px) {
  .ai-page {
    grid-template-columns: 1fr;
  }
  .ai-sidebar {
    display: none;
  }
  .suggestions {
    grid-template-columns: 1fr;
  }
}
</style>
