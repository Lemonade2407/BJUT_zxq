<script setup>
import { ref, computed, onMounted } from 'vue'
import { toast } from '@/utils/toast'
import { error as logError } from '@/utils/logger'
import tokenManager from '@/utils/tokenManager'

// 用户列表
const users = ref([])
const isLoading = ref(false)
const searchKeyword = ref('')

// 分页配置
const PAGE_SIZE = 20
const currentPage = ref(1)
const total = ref(0)

// 编辑对话框
const showEditDialog = ref(false)
const editingUser = ref(null)
const editForm = ref({
  username: '',
  employeeId: '',
  realName: '',
  email: '',
  password: '',
  gender: null,
  bio: '',
  className: '',
  role: 'USER'
})

// 加载用户列表
const loadUsers = async () => {
  isLoading.value = true
  try {
    const response = await fetch(`/api/admin/users?pageNum=${currentPage.value}&pageSize=${PAGE_SIZE}`, {
      headers: {
        'Authorization': `Bearer ${tokenManager.getToken()}`
      }
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.code === 200) {
        users.value = data.data.records || []
        total.value = data.data.total || 0
      }
    }
  } catch (error) {
    logError('加载用户列表失败:', error)
    toast.error('加载用户列表失败')
  } finally {
    isLoading.value = false
  }
}

// 搜索用户
const searchUsers = async () => {
  if (!searchKeyword.value.trim()) {
    loadUsers()
    return
  }
  
  isLoading.value = true
  try {
    const response = await fetch(`/api/admin/users/search?keyword=${encodeURIComponent(searchKeyword.value)}&pageNum=${currentPage.value}&pageSize=${PAGE_SIZE}`, {
      headers: {
        'Authorization': `Bearer ${tokenManager.getToken()}`
      }
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.code === 200) {
        users.value = data.data.records || []
        total.value = data.data.total || 0
      }
    }
  } catch (error) {
    logError('搜索用户失败:', error)
    toast.error('搜索用户失败')
  } finally {
    isLoading.value = false
  }
}

// 切换页码
const changePage = (page) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  if (searchKeyword.value.trim()) {
    searchUsers()
  } else {
    loadUsers()
  }
}

// 计算总页数
const totalPages = computed(() => Math.ceil(total.value / PAGE_SIZE))

// 封禁用户
const banUser = async (userId) => {
  if (!confirm('确定要封禁该用户吗？')) return
  
  try {
    const response = await fetch(`/api/admin/users/${userId}/ban`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${tokenManager.getToken()}`
      }
    })
    
    if (response.ok) {
      toast.success('用户已封禁')
      loadUsers()
    }
  } catch (error) {
    logError('封禁用户失败:', error)
    toast.error('操作失败')
  }
}

// 解封用户
const unbanUser = async (userId) => {
  if (!confirm('确定要解封该用户吗？')) return
  
  try {
    const response = await fetch(`/api/admin/users/${userId}/unban`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${tokenManager.getToken()}`
      }
    })
    
    if (response.ok) {
      toast.success('用户已解封')
      loadUsers()
    }
  } catch (error) {
    logError('解封用户失败:', error)
    toast.error('操作失败')
  }
}

// 删除用户
const deleteUser = async (userId) => {
  if (!confirm('确定要删除该用户吗？此操作不可恢复！')) return
  
  try {
    const response = await fetch(`/api/admin/users/${userId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${tokenManager.getToken()}`
      }
    })
    
    if (response.ok) {
      toast.success('用户已删除')
      loadUsers()
    }
  } catch (error) {
    logError('删除用户失败:', error)
    toast.error('操作失败')
  }
}

// 打开编辑对话框
const openEditDialog = (user) => {
  editingUser.value = user
  // 将后端的 sex 字符串转换为前端的 gender 数字
  let genderValue = null
  if (user.sex === 'MALE') {
    genderValue = 1
  } else if (user.sex === 'FEMALE') {
    genderValue = 0
  }
  
  editForm.value = {
    username: user.username,
    employeeId: user.employeeId || '',
    realName: user.realName || '',
    email: user.email || '',
    password: '',
    gender: genderValue,
    bio: user.bio || '',
    className: user.className || '',
    role: user.role
  }
  showEditDialog.value = true
}

// 关闭编辑对话框
const closeEditDialog = () => {
  showEditDialog.value = false
  editingUser.value = null
  editForm.value = {
    username: '',
    employeeId: '',
    realName: '',
    email: '',
    password: '',
    gender: null,
    bio: '',
    className: '',
    role: 'USER'
  }
}

// 保存用户信息
const saveUser = async () => {
  if (!editForm.value.username.trim()) {
    toast.error('用户名不能为空')
    return
  }
  
  if (!editForm.value.email.trim()) {
    toast.error('邮箱不能为空')
    return
  }
  
  // 准备提交的数据，将 gender 转换为 sex
  const submitData = {
    ...editForm.value,
    sex: editForm.value.gender === 1 ? 'MALE' : (editForm.value.gender === 0 ? 'FEMALE' : 'UNKNOWN')
  }
  // 删除 gender 字段，后端不需要
  delete submitData.gender
  
  try {
    const response = await fetch(`/api/admin/users/${editingUser.value.id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${tokenManager.getToken()}`
      },
      body: JSON.stringify(submitData)
    })
    
    if (response.ok) {
      toast.success('用户信息已更新')
      closeEditDialog()
      loadUsers()
    } else {
      const data = await response.json()
      toast.error(data.message || '更新失败')
    }
  } catch (error) {
    logError('更新用户失败:', error)
    toast.error('操作失败')
  }
}

// 获取角色文本
const getRoleText = (role) => {
  const roleMap = {
    'USER': '学生',
    'TEACHER': '教师',
    'ADMIN': '管理员'
  }
  return roleMap[role] || '未知'
}

// 获取性别文本
const getSexText = (sex) => {
  const sexMap = {
    'MALE': '男',
    'FEMALE': '女',
    'UNKNOWN': '未设置'
  }
  return sexMap[sex] || '-'
}

// 获取状态文本
const getStatusText = (status) => {
  return status === 1 ? '正常' : '已封禁'
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadUsers()
})
</script>

<template>
  <div class="management-container">
    <div class="section-header">
      <h2 class="section-title">👥 用户管理</h2>
      <div class="search-box">
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索用户名、邮箱或学号..."
          class="search-input"
          @keyup.enter="searchUsers"
        />
        <button @click="searchUsers" class="search-btn">搜索</button>
      </div>
    </div>

    <!-- 用户列表 -->
    <div v-if="isLoading" class="loading-state">
      <span class="loading-icon">⏳</span>
      <p>加载中...</p>
    </div>

    <div v-else-if="users.length === 0" class="empty-state">
      <span class="empty-icon">📭</span>
      <p>暂无用户数据</p>
    </div>

    <div v-else class="table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>身份标识号</th>
            <th>真实姓名</th>
            <th>邮箱</th>
            <th>性别</th>
            <th>班级</th>
            <th>角色</th>
            <th>注册时间</th>
            <th>操作</th>
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
            <td>
              <span :class="['role-badge', user.role.toLowerCase()]">
                {{ getRoleText(user.role) }}
              </span>
            </td>
            <td>{{ formatDate(user.createdAt) }}</td>
            <td>
              <div class="action-buttons">
                <button
                  @click="openEditDialog(user)"
                  class="action-btn edit-btn"
                  title="编辑用户"
                >
                  ✏️
                </button>
                <button
                  v-if="user.status === 1"
                  @click="banUser(user.id)"
                  class="action-btn ban-btn"
                  title="封禁用户"
                >
                  🚫
                </button>
                <button
                  v-else
                  @click="unbanUser(user.id)"
                  class="action-btn unban-btn"
                  title="解封用户"
                >
                  ✅
                </button>
                <button
                  @click="deleteUser(user.id)"
                  class="action-btn delete-btn"
                  title="删除用户"
                >
                  🗑️
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <!-- 分页组件 -->
    <div v-if="users.length > 0 && totalPages > 1" class="pagination">
      <button 
        @click="changePage(currentPage - 1)" 
        :disabled="currentPage <= 1"
        class="pagination-btn">
        上一页
      </button>
      
      <span class="pagination-info">
        第 {{ currentPage }} / {{ totalPages }} 页，
        共 {{ total }} 条记录
      </span>
      
      <button 
        @click="changePage(currentPage + 1)" 
        :disabled="currentPage >= totalPages"
        class="pagination-btn">
        下一页
      </button>
    </div>

    <!-- 编辑用户对话框 -->
    <div v-if="showEditDialog" class="modal-overlay" @click="closeEditDialog">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">✏️ 编辑用户信息</h3>
          <button @click="closeEditDialog" class="close-btn">×</button>
        </div>
        
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">用户名 *</label>
            <input
              v-model="editForm.username"
              type="text"
              class="form-input"
              placeholder="请输入用户名"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">身份标识号</label>
            <input
              v-model="editForm.employeeId"
              type="text"
              class="form-input"
              placeholder="学号或职工号"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">真实姓名</label>
            <input
              v-model="editForm.realName"
              type="text"
              class="form-input"
              placeholder="请输入真实姓名"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">邮箱 *</label>
            <input
              v-model="editForm.email"
              type="email"
              class="form-input"
              placeholder="请输入邮箱"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">密码（留空不修改）</label>
            <input
              v-model="editForm.password"
              type="password"
              class="form-input"
              placeholder="输入新密码"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">性别</label>
            <select v-model="editForm.gender" class="form-select">
              <option :value="null">未设置</option>
              <option :value="1">男</option>
              <option :value="0">女</option>
            </select>
          </div>
          
          <div class="form-group">
            <label class="form-label">简介</label>
            <textarea
              v-model="editForm.bio"
              class="form-input form-textarea"
              placeholder="请输入个人简介"
              rows="3"
            ></textarea>
          </div>
          
          <div class="form-group">
            <label class="form-label">班级</label>
            <input
              v-model="editForm.className"
              type="text"
              class="form-input"
              placeholder="请输入班级名称"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">角色</label>
            <select v-model="editForm.role" class="form-select">
              <option value="USER">学生</option>
              <option value="TEACHER">教师</option>
              <option value="ADMIN">管理员</option>
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
.management-container {
  width: 100%;
  max-width: 1400px;
  min-height: calc(100vh - 144px);
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 4px rgba(0, 51, 102, 0.05);
  display: flex;
  flex-direction: column;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.section-title {
  font-size: 24px;
  font-weight: 600;
  color: #064e3b;
  margin: 0;
}

.search-box {
  display: flex;
  gap: 12px;
}

.search-input {
  padding: 8px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  width: 300px;
  transition: all 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.search-btn {
  padding: 8px 20px;
  background-color: #10b981;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.search-btn:hover {
  background-color: #059669;
}

.table-wrapper {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table thead {
  background-color: #f5f7fa;
}

.data-table th {
  padding: 12px 16px;
  text-align: left;
  font-size: 14px;
  font-weight: 600;
  color: #333333;
  border-bottom: 2px solid #e0e0e0;
}

.data-table td {
  padding: 12px 16px;
  font-size: 14px;
  color: #666666;
  border-bottom: 1px solid #f0f0f0;
}

.data-table tbody tr:hover {
  background-color: #f9fafb;
}

.role-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.role-badge.user {
  background-color: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.role-badge.teacher {
  background-color: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.role-badge.admin {
  background-color: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.active {
  background-color: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.status-badge.banned {
  background-color: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px 10px;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
  background-color: transparent;
}

.action-btn:hover {
  transform: scale(1.1);
}

.ban-btn:hover {
  background-color: rgba(239, 68, 68, 0.1);
}

.unban-btn:hover {
  background-color: rgba(16, 185, 129, 0.1);
}

.delete-btn:hover {
  background-color: rgba(239, 68, 68, 0.1);
}

.edit-btn:hover {
  background-color: rgba(59, 130, 246, 0.1);
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background-color: #ffffff;
  border-radius: 12px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e0e0e0;
}

.modal-title {
  font-size: 20px;
  font-weight: 600;
  color: #064e3b;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #999999;
  cursor: pointer;
  transition: color 0.2s;
  line-height: 1;
}

.close-btn:hover {
  color: #333333;
}

.modal-body {
  padding: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333333;
  margin-bottom: 8px;
}

.form-input,
.form-select {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s;
}

.form-input:focus,
.form-select:focus {
  outline: none;
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #e0e0e0;
}

.btn {
  padding: 10px 24px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel {
  background-color: #f5f5f5;
  color: #666666;
}

.btn-cancel:hover {
  background-color: #e0e0e0;
}

.btn-save {
  background-color: #10b981;
  color: #ffffff;
}

.btn-save:hover {
  background-color: #059669;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #999999;
}

.loading-icon,
.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 分页样式 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
  padding: 16px 0;
}

.pagination-btn {
  padding: 8px 16px;
  background-color: #ffffff;
  color: #10b981;
  border: 1px solid #10b981;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.pagination-btn:hover:not(:disabled) {
  background-color: #10b981;
  color: #ffffff;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-info {
  font-size: 14px;
  color: #666666;
}
</style>
