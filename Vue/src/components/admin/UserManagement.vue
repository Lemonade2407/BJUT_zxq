<script setup>
import { ref, computed, onMounted } from 'vue'
import { toast } from '@/utils/toast'
import { error as logError } from '@/utils/logger'
import { getAdminUsers, searchAdminUsers, banUser as banUserApi, unbanUser as unbanUserApi, setUserRole, updateAdminUser, deleteAdminUser } from '@/api/admin'

const users = ref([])
const isLoading = ref(false)
const searchKeyword = ref('')
const PAGE_SIZE = 20
const currentPage = ref(1)
const total = ref(0)

const showEditDialog = ref(false)
const editingUser = ref(null)
const editForm = ref({
  username: '', employeeId: '', realName: '', email: '',
  password: '', gender: null, bio: '', className: '', role: 'USER'
})

const totalPages = computed(() => Math.ceil(total.value / PAGE_SIZE))

const loadUsers = async () => {
  isLoading.value = true
  try {
    const res = await getAdminUsers({ pageNum: currentPage.value, pageSize: PAGE_SIZE })
    if (res.code === 200) {
      users.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    logError('加载用户列表失败:', error)
    toast.error('加载用户列表失败')
  } finally { isLoading.value = false }
}

const searchUsers = async () => {
  if (!searchKeyword.value.trim()) { loadUsers(); return }
  isLoading.value = true
  try {
    const res = await searchAdminUsers(searchKeyword.value, { pageNum: currentPage.value, pageSize: PAGE_SIZE })
    if (res.code === 200) {
      users.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    logError('搜索用户失败:', error)
    toast.error('搜索用户失败')
  } finally { isLoading.value = false }
}

const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  searchKeyword.value.trim() ? searchUsers() : loadUsers()
}

const handleBan = async (userId) => {
  if (!confirm('确定要封禁该用户吗？')) return
  try {
    const res = await banUserApi(userId)
    if (res.code === 200) { toast.success('用户已封禁'); loadUsers() }
    else toast.error(res.message || '操作失败')
  } catch (error) { logError('封禁用户失败:', error); toast.error('操作失败') }
}

const handleUnban = async (userId) => {
  if (!confirm('确定要解封该用户吗？')) return
  try {
    const res = await unbanUserApi(userId)
    if (res.code === 200) { toast.success('用户已解封'); loadUsers() }
    else toast.error(res.message || '操作失败')
  } catch (error) { logError('解封用户失败:', error); toast.error('操作失败') }
}

const handleDelete = async (userId) => {
  if (!confirm('确定要删除该用户吗？此操作不可恢复！')) return
  try {
    const res = await deleteAdminUser(userId)
    if (res.code === 200) { toast.success('用户已删除'); loadUsers() }
    else toast.error(res.message || '操作失败')
  } catch (error) { logError('删除用户失败:', error); toast.error('操作失败') }
}

const openEditDialog = (user) => {
  editingUser.value = user
  let genderValue = null
  if (user.sex === 'MALE') genderValue = 1
  else if (user.sex === 'FEMALE') genderValue = 0
  editForm.value = {
    username: user.username, employeeId: user.employeeId || '',
    realName: user.realName || '', email: user.email || '',
    password: '', gender: genderValue, bio: user.bio || '',
    className: user.className || '', role: user.role
  }
  showEditDialog.value = true
}

const closeEditDialog = () => {
  showEditDialog.value = false
  editingUser.value = null
  editForm.value = { username: '', employeeId: '', realName: '', email: '', password: '', gender: null, bio: '', className: '', role: 'USER' }
}

const saveUser = async () => {
  if (!editForm.value.username.trim()) { toast.error('用户名不能为空'); return }
  if (!editForm.value.email.trim()) { toast.error('邮箱不能为空'); return }
  try {
    const res = await updateAdminUser(editingUser.value.id, editForm.value)
    if (res.code === 200) { toast.success('用户信息已更新'); closeEditDialog(); loadUsers() }
    else toast.error(res.message || '更新失败')
  } catch (error) { logError('更新用户失败:', error); toast.error('操作失败') }
}

const getRoleText = (role) => ({ USER: '学生', TEACHER: '教师', ADMIN: '管理员' })[role] || '未知'
const getSexText = (sex) => ({ MALE: '男', FEMALE: '女', UNKNOWN: '未设置' })[sex] || '-'
const getStatusText = (status) => status === 1 ? '正常' : '已封禁'
const formatDate = (d) => d ? new Date(d).toLocaleDateString('zh-CN') : '-'

onMounted(() => loadUsers())
</script>

<template>
  <div class="management-container">
    <div class="section-header">
      <h2 class="section-title">👥 用户管理</h2>
      <div class="search-box">
        <input v-model="searchKeyword" type="text" placeholder="搜索用户名、邮箱或学号..." class="search-input" @keyup.enter="searchUsers" />
        <button @click="searchUsers" class="search-btn">搜索</button>
      </div>
    </div>

    <div v-if="isLoading" class="loading-state"><span class="loading-icon">⏳</span><p>加载中...</p></div>
    <div v-else-if="users.length === 0" class="empty-state"><span class="empty-icon">📭</span><p>暂无用户数据</p></div>

    <div v-else class="table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th><th>用户名</th><th>身份标识号</th><th>真实姓名</th><th>邮箱</th>
            <th>性别</th><th>班级</th><th>角色</th><th>注册时间</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.username }}</td>
            <td>{{ user.employeeId || '-' }}</td>
            <td>{{ user.realName || '-' }}</td>
            <td>{{ user.email || '-' }}</td>
            <td>{{ getSexText(user.sex) }}</td>
            <td>{{ user.className || '-' }}</td>
            <td><span :class="['role-badge', user.role.toLowerCase()]">{{ getRoleText(user.role) }}</span></td>
            <td>{{ formatDate(user.createdAt) }}</td>
            <td>
              <div class="action-buttons">
                <button @click="openEditDialog(user)" class="action-btn edit-btn" title="编辑">✏️</button>
                <button v-if="user.status === 1" @click="handleBan(user.id)" class="action-btn ban-btn" title="封禁">🚫</button>
                <button v-else @click="handleUnban(user.id)" class="action-btn unban-btn" title="解封">✅</button>
                <button @click="handleDelete(user.id)" class="action-btn delete-btn" title="删除">🗑️</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="users.length > 0 && totalPages > 1" class="pagination">
      <button @click="changePage(currentPage - 1)" :disabled="currentPage <= 1" class="pagination-btn">上一页</button>
      <span class="pagination-info">第 {{ currentPage }} / {{ totalPages }} 页，共 {{ total }} 条</span>
      <button @click="changePage(currentPage + 1)" :disabled="currentPage >= totalPages" class="pagination-btn">下一页</button>
    </div>

    <div v-if="showEditDialog" class="modal-overlay" @click="closeEditDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header"><h3 class="modal-title">✏️ 编辑用户信息</h3><button @click="closeEditDialog" class="close-btn">×</button></div>
        <div class="modal-body">
          <div class="form-group"><label class="form-label">用户名 *</label><input v-model="editForm.username" class="form-input" /></div>
          <div class="form-group"><label class="form-label">身份标识号</label><input v-model="editForm.employeeId" class="form-input" /></div>
          <div class="form-group"><label class="form-label">真实姓名</label><input v-model="editForm.realName" class="form-input" /></div>
          <div class="form-group"><label class="form-label">邮箱 *</label><input v-model="editForm.email" type="email" class="form-input" /></div>
          <div class="form-group"><label class="form-label">密码（留空不修改）</label><input v-model="editForm.password" type="password" class="form-input" /></div>
          <div class="form-group"><label class="form-label">性别</label>
            <select v-model="editForm.gender" class="form-select">
              <option :value="null">未设置</option><option :value="1">男</option><option :value="0">女</option>
            </select>
          </div>
          <div class="form-group"><label class="form-label">简介</label><textarea v-model="editForm.bio" class="form-input form-textarea" rows="3"></textarea></div>
          <div class="form-group"><label class="form-label">班级</label><input v-model="editForm.className" class="form-input" /></div>
          <div class="form-group"><label class="form-label">角色</label>
            <select v-model="editForm.role" class="form-select">
              <option value="USER">学生</option><option value="TEACHER">教师</option><option value="ADMIN">管理员</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="closeEditDialog" class="btn btn-cancel">取消</button>
          <button @click="saveUser" class="btn btn-save">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.management-container { width: 100%; max-width: 1400px; min-height: calc(100vh - 144px); background: #fff; border: 1px solid #d9d9d9; border-radius: 8px; padding: 24px; box-shadow: 0 2px 4px rgba(0,51,102,0.05); display: flex; flex-direction: column; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; flex-wrap: wrap; gap: 16px; }
.section-title { font-size: 24px; font-weight: 600; color: #064e3b; margin: 0; }
.search-box { display: flex; gap: 12px; }
.search-input { padding: 8px 16px; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px; width: 300px; }
.search-input:focus { outline: none; border-color: #10b981; }
.search-btn { padding: 8px 20px; background: #10b981; color: #fff; border: none; border-radius: 6px; font-size: 14px; cursor: pointer; }
.search-btn:hover { background: #059669; }
.table-wrapper { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table thead { background: #f5f7fa; }
.data-table th { padding: 12px 16px; text-align: left; font-size: 14px; font-weight: 600; color: #333; border-bottom: 2px solid #e0e0e0; }
.data-table td { padding: 12px 16px; font-size: 14px; color: #666; border-bottom: 1px solid #f0f0f0; }
.data-table tbody tr:hover { background: #f9fafb; }
.role-badge { display: inline-block; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.role-badge.user { background: rgba(59,130,246,0.1); color: #3b82f6; }
.role-badge.teacher { background: rgba(16,185,129,0.1); color: #10b981; }
.role-badge.admin { background: rgba(239,68,68,0.1); color: #ef4444; }
.action-buttons { display: flex; gap: 8px; }
.action-btn { padding: 6px 10px; border: none; border-radius: 4px; font-size: 16px; cursor: pointer; background: transparent; transition: all 0.2s; }
.action-btn:hover { transform: scale(1.1); }
.ban-btn:hover { background: rgba(239,68,68,0.1); }
.unban-btn:hover { background: rgba(16,185,129,0.1); }
.delete-btn:hover { background: rgba(239,68,68,0.1); }
.edit-btn:hover { background: rgba(59,130,246,0.1); }
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-content { background: #fff; border-radius: 12px; width: 90%; max-width: 600px; max-height: 90vh; overflow-y: auto; box-shadow: 0 4px 20px rgba(0,0,0,0.15); }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid #e0e0e0; }
.modal-title { font-size: 20px; font-weight: 600; color: #064e3b; margin: 0; }
.close-btn { background: none; border: none; font-size: 28px; color: #999; cursor: pointer; }
.modal-body { padding: 24px; }
.form-group { margin-bottom: 20px; }
.form-label { display: block; font-size: 14px; font-weight: 500; color: #333; margin-bottom: 8px; }
.form-input, .form-select { width: 100%; padding: 10px 14px; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px; transition: all 0.2s; }
.form-input:focus, .form-select:focus { outline: none; border-color: #10b981; }
.modal-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 16px 24px; border-top: 1px solid #e0e0e0; }
.btn { padding: 10px 24px; border: none; border-radius: 6px; font-size: 14px; font-weight: 500; cursor: pointer; }
.btn-cancel { background: #f5f5f5; color: #666; }
.btn-cancel:hover { background: #e0e0e0; }
.btn-save { background: #10b981; color: #fff; }
.btn-save:hover { background: #059669; }
.loading-state, .empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 20px; color: #999; }
.loading-icon, .empty-icon { font-size: 48px; margin-bottom: 16px; }
.loading-icon { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.pagination { display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 24px; padding: 16px 0; }
.pagination-btn { padding: 8px 16px; background: #fff; color: #10b981; border: 1px solid #10b981; border-radius: 6px; font-size: 14px; cursor: pointer; }
.pagination-btn:hover:not(:disabled) { background: #10b981; color: #fff; }
.pagination-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.pagination-info { font-size: 14px; color: #666; }
</style>
