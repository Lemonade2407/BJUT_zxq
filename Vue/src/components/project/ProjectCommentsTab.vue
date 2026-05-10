<script setup>
import { ref, onMounted } from 'vue'
import { getProjectComments, createComment } from '@/api/comment'
import { getUserById } from '@/api/auth'
import { toast } from '@/utils/toast'
import { error as logError } from '@/utils/logger'

const props = defineProps({ projectId: { type: [String, Number], required: true } })

const comments = ref([])
const newComment = ref('')
const isLoading = ref(false)

const formatCommentTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  if (days < 365) return `${Math.floor(days / 30)}个月前`
  return `${Math.floor(days / 365)}年前`
}

const loadCommentUsers = async () => {
  const userIds = [...new Set(comments.value.map(c => c.userId).filter(id => id))]
  if (!userIds.length) return
  try {
    const results = await Promise.all(userIds.map(id => getUserById(id)))
    const userMap = {}
    results.forEach((res, i) => { if (res.code === 200 && res.data) userMap[userIds[i]] = res.data })
    comments.value = comments.value.map(c => {
      const u = userMap[c.userId]
      return { ...c, userName: u ? u.username : '未知用户', userAvatar: u ? u.avatar : '' }
    })
  } catch (e) { /* ignore */ }
}

const loadComments = async () => {
  isLoading.value = true
  try {
    const res = await getProjectComments(props.projectId)
    if (res.code === 200 && res.data && res.data.records) {
      comments.value = res.data.records
      await loadCommentUsers()
    }
  } catch (e) {
    logError('加载评论失败:', e)
  } finally { isLoading.value = false }
}

const submitComment = async () => {
  if (!newComment.value.trim()) { toast.warning('请输入评论内容'); return }
  try {
    const res = await createComment(props.projectId, { content: newComment.value.trim() })
    if (res.code === 200 && res.data) {
      comments.value.unshift({ ...res.data, userName: '我', userAvatar: '' })
      newComment.value = ''
      toast.success('评论成功！')
      loadComments() // reload to get proper user info
    }
  } catch (e) {
    logError('提交评论失败:', e)
    toast.error(e.message || '评论失败，请稍后重试')
  }
}

onMounted(() => loadComments())
</script>

<template>
  <div class="content-section comments-content">
    <div class="comment-input-section">
      <textarea v-model="newComment" placeholder="写下你的评论..." class="comment-textarea" rows="4"></textarea>
      <button class="submit-comment-btn" @click="submitComment">提交评论</button>
    </div>
    <div v-if="isLoading" style="text-align:center;padding:24px;color:#999">加载中...</div>
    <div v-else class="comments-list">
      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <div class="comment-header">
          <img v-if="comment.userAvatar" :src="comment.userAvatar" :alt="comment.userName" class="comment-avatar-img" />
          <span v-else class="comment-avatar-text">{{ (comment.userName || 'U').charAt(0).toUpperCase() }}</span>
          <span class="comment-user">{{ comment.userName || '未知用户' }}</span>
          <span class="comment-time">{{ formatCommentTime(comment.createdAt) }}</span>
        </div>
        <p class="comment-content">{{ comment.content }}</p>
      </div>
      <div v-if="comments.length === 0" style="text-align:center;color:#999;padding:24px">暂无评论，来发表第一条评论吧</div>
    </div>
  </div>
</template>

<style scoped>
.comments-content { padding: 24px 32px; }
.comment-input-section { margin-bottom: 24px; padding-bottom: 24px; border-bottom: 1px solid #e0e0e0; }
.comment-textarea { width: 100%; padding: 12px; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px; font-family: inherit; resize: vertical; margin-bottom: 12px; }
.comment-textarea:focus { outline: none; border-color: #10b981; box-shadow: 0 0 0 3px rgba(16,185,129,0.1); }
.submit-comment-btn { padding: 10px 24px; background: #10b981; color: #fff; border: none; border-radius: 6px; font-size: 14px; cursor: pointer; }
.submit-comment-btn:hover { background: #059669; }
.comments-list { display: flex; flex-direction: column; gap: 16px; }
.comment-item { padding: 16px; background: #f9f9f9; border-radius: 6px; border-left: 3px solid #10b981; }
.comment-header { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.comment-avatar-img { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; border: 2px solid #e0e0e0; }
.comment-avatar-text { width: 36px; height: 36px; border-radius: 50%; background: #10b981; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: 600; flex-shrink: 0; }
.comment-user { font-weight: 600; color: #333; }
.comment-time { font-size: 12px; color: #999; }
.comment-content { font-size: 14px; color: #666; line-height: 1.6; margin: 0; }
</style>
