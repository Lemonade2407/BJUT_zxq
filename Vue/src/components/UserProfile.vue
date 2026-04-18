<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentUser, updateProfile, changePassword } from '@/api/auth'
import { toast } from '@/utils/toast'
import { error as logError, log } from '@/utils/logger'
import tokenManager from '@/utils/tokenManager'

const router = useRouter()

// 用户信息
const userInfo = ref({
  id: null,
  username: '',
  studentId: '',
  email: '',
  phone: '',
  sex: '',
  bio: '',
  avatar: '',
  role: ''
})

// 编辑模式
const isEditing = ref(false)

// 修改密码对话框
const showPasswordDialog = ref(false)
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 加载用户信息
const loadUserInfo = async () => {
  try {
    const response = await getCurrentUser()
    if (response.code === 200) {
      userInfo.value = response.data
      // 使用 tokenManager 保存用户信息
      tokenManager.saveUserInfo(response.data)
    } else {
      toast.error(response.message || '获取用户信息失败')
    }
  } catch (error) {
    logError('获取用户信息失败:', error)
    toast.error('获取用户信息失败，请重新登录')
    // 如果 token 失效，跳转到登录页
    tokenManager.removeToken()
    router.push('/login')
  }
}

// 切换编辑模式
const toggleEdit = () => {
  isEditing.value = !isEditing.value
  if (!isEditing.value) {
    // 取消编辑时重新加载数据
    loadUserInfo()
  }
}

// 保存用户信息
const handleSave = async () => {
  // TODO: 添加表单验证（手机号、邮箱格式等）
  try {
    const updateData = {
      username: userInfo.value.username,
      avatar: userInfo.value.avatar,
      phone: userInfo.value.phone,
      sex: userInfo.value.sex,
      bio: userInfo.value.bio
    }
    
    const response = await updateProfile(updateData)
    if (response.code === 200) {
      toast.success('保存成功')
      isEditing.value = false
      // 使用 tokenManager 更新本地存储
      tokenManager.saveUserInfo(userInfo.value)
    } else {
      toast.error(response.message || '保存失败')
    }
  } catch (error) {
    logError('保存用户信息失败:', error)
    toast.error('保存失败，请稍后重试')
  }
}

// 打开修改密码对话框
const openPasswordDialog = () => {
  showPasswordDialog.value = true
  passwordForm.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
}

// 关闭修改密码对话框
const closePasswordDialog = () => {
  showPasswordDialog.value = false
  passwordForm.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
}

// 修改密码
const handleChangePassword = async () => {
  // 验证输入
  if (!passwordForm.value.oldPassword) {
    toast.error('请输入原密码')
    return
  }
  if (!passwordForm.value.newPassword) {
    toast.error('请输入新密码')
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    toast.error('新密码长度不能少于6位')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    toast.error('两次输入的密码不一致')
    return
  }
  
  try {
    const response = await changePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    
    if (response.code === 200) {
      toast.success('密码修改成功')
      closePasswordDialog()
    } else {
      toast.error(response.message || '密码修改失败')
    }
  } catch (error) {
    logError('修改密码失败:', error)
    toast.error(error.response?.data?.message || '修改密码失败，请检查原密码是否正确')
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

// 上传头像
const handleAvatarUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    toast.error('请选择图片文件')
    return
  }
  
  // 验证文件大小（5MB）
  if (file.size > 5 * 1024 * 1024) {
    toast.error('图片大小不能超过 5MB')
    return
  }
  
  try {
    // TODO: 调用后端 API 上传头像到 OSS
    // TODO: 添加图片压缩功能，减少上传流量
    // TODO: 添加图片裁剪功能
    // 暂时使用本地预览
    const reader = new FileReader()
    reader.onload = (e) => {
      userInfo.value.avatar = e.target.result
      toast.success('头像更新成功')
    }
    reader.readAsDataURL(file)
  } catch (error) {
    logError('上传头像失败:', error)
    toast.error('上传头像失败，请稍后重试')
  }
}

// 获取角色显示文本
const getRoleText = (role) => {
  const roleMap = {
    'USER': '普通用户',
    'TEACHER': '教师',
    'ADMIN': '管理员'
  }
  return roleMap[role] || '未知'
}

// 获取性别显示文本
const getSexText = (sex) => {
  const sexMap = {
    '男': '男',
    '女': '女',
    '未知': '保密'
  }
  return sexMap[sex] || '保密'
}

// 组件挂载时加载用户信息
onMounted(() => {
  loadUserInfo()
})
</script>

<template>
  <div class="profile-container">
    <div class="profile-card">
      <!-- 头部背景 -->
      <div class="profile-header">
        <div class="header-bg"></div>
        <div class="header-content">
          <div class="avatar-wrapper">
            <img
              :src="userInfo.avatar || '/bjut-logo.svg'"
              alt="头像"
              class="profile-avatar"
            />
            <label v-if="isEditing" class="avatar-upload-btn" title="点击更换头像">
              📷
              <input
                type="file"
                accept="image/*"
                @change="handleAvatarUpload"
                style="display: none;"
              />
            </label>
          </div>
          <div class="profile-basic">
            <h1 class="profile-name">{{ userInfo.username }}</h1>
            <p class="profile-role">{{ getRoleText(userInfo.role) }}</p>
            <p class="profile-id">ID: {{ userInfo.id }}</p>
          </div>
        </div>
      </div>

      <!-- 用户信息表单 -->
      <div class="profile-body">
        <div class="section-title">
          <span>基本信息</span>
          <button 
            v-if="!isEditing" 
            @click="toggleEdit" 
            class="edit-btn"
          >
            编辑资料
          </button>
          <div v-else class="edit-actions">
            <button @click="toggleEdit" class="cancel-btn">取消</button>
            <button @click="handleSave" class="save-btn">保存</button>
          </div>
        </div>

        <div class="info-grid">
          <div class="info-item">
            <label>用户ID</label>
            <div class="info-value">{{ userInfo.id }}</div>
          </div>

          <div class="info-item">
            <label>用户名</label>
            <input
              v-if="isEditing"
              v-model="userInfo.username"
              type="text"
              placeholder="请输入用户名"
              class="info-input"
            />
            <div v-else class="info-value">{{ userInfo.username }}</div>
          </div>

          <div class="info-item">
            <label>学号</label>
            <div class="info-value">{{ userInfo.studentId }}</div>
          </div>

          <div class="info-item">
            <label>邮箱</label>
            <div class="info-value">{{ userInfo.email }}</div>
          </div>

          <div class="info-item">
            <label>手机号</label>
            <input
              v-if="isEditing"
              v-model="userInfo.phone"
              type="tel"
              placeholder="请输入手机号"
              class="info-input"
            />
            <div v-else class="info-value">{{ userInfo.phone || '未设置' }}</div>
          </div>

          <div class="info-item">
            <label>性别</label>
            <select
              v-if="isEditing"
              v-model="userInfo.sex"
              class="info-select"
            >
              <option value="男">男</option>
              <option value="女">女</option>
              <option value="未知">保密</option>
            </select>
            <div v-else class="info-value">{{ getSexText(userInfo.sex) }}</div>
          </div>

          <div class="info-item full-width">
            <label>个人简介</label>
            <textarea
              v-if="isEditing"
              v-model="userInfo.bio"
              placeholder="介绍一下自己吧..."
              class="info-textarea"
              rows="3"
            ></textarea>
            <div v-else class="info-value bio-text">{{ userInfo.bio || '暂无简介' }}</div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-section">
          <button @click="openPasswordDialog" class="action-btn password-btn">
            🔑 修改密码
          </button>
        </div>
      </div>
    </div>

    <!-- 修改密码对话框 -->
    <div v-if="showPasswordDialog" class="modal-overlay" @click.self="closePasswordDialog">
      <div class="modal-content">
        <div class="modal-header">
          <h2>修改密码</h2>
          <button @click="closePasswordDialog" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>原密码</label>
            <input
              v-model="passwordForm.oldPassword"
              type="password"
              placeholder="请输入原密码"
              class="form-input"
            />
          </div>
          <div class="form-group">
            <label>新密码</label>
            <input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="请输入新密码（至少6位）"
              class="form-input"
            />
          </div>
          <div class="form-group">
            <label>确认新密码</label>
            <input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              class="form-input"
            />
          </div>
        </div>
        <div class="modal-footer">
          <button @click="closePasswordDialog" class="modal-btn cancel">取消</button>
          <button @click="handleChangePassword" class="modal-btn confirm">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-container {
  max-width: 100%;
  min-height: 100%;
}

.profile-card {
  background: #ffffff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

/* 头部样式 */
.profile-header {
  position: relative;
  height: 200px;
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #003366 0%, #004080 100%);
}

.header-content {
  position: relative;
  height: 100%;
  display: flex;
  align-items: flex-end;
  padding: 0 40px 20px;
  gap: 20px;
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
}

.profile-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 4px solid #ffffff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  background: #ffffff;
}

.avatar-upload-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 36px;
  height: 36px;
  background: #003366;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  transition: all 0.2s;
  border: 3px solid #ffffff;
}

.avatar-upload-btn:hover {
  background: #004080;
  transform: scale(1.1);
}

.profile-basic {
  color: #ffffff;
  padding-bottom: 10px;
}

.profile-name {
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.profile-role {
  font-size: 14px;
  opacity: 0.9;
  margin: 0 0 4px 0;
}

.profile-id {
  font-size: 12px;
  opacity: 0.8;
  margin: 0;
  font-family: 'Courier New', monospace;
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-block;
}

/* 主体内容 */
.profile-body {
  padding: 30px 40px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
}

.section-title span {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.edit-btn,
.save-btn,
.cancel-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.edit-btn {
  background: #003366;
  color: #ffffff;
}

.edit-btn:hover {
  background: #004080;
}

.save-btn {
  background: #52c41a;
  color: #ffffff;
  margin-left: 8px;
}

.save-btn:hover {
  background: #73d13d;
}

.cancel-btn {
  background: #f0f0f0;
  color: #666;
}

.cancel-btn:hover {
  background: #e0e0e0;
}

.edit-actions {
  display: flex;
  gap: 8px;
}

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item.full-width {
  grid-column: 1 / -1;
}

.info-item label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.info-value {
  font-size: 15px;
  color: #333;
  padding: 8px 0;
}

.bio-text {
  line-height: 1.6;
  white-space: pre-wrap;
}

.info-input,
.info-select,
.info-textarea {
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s;
}

.info-input:focus,
.info-select:focus,
.info-textarea:focus {
  border-color: #003366;
  outline: none;
  box-shadow: 0 0 0 2px rgba(0, 51, 102, 0.1);
}

.info-textarea {
  resize: vertical;
  font-family: inherit;
}

/* 操作区域 */
.action-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 2px solid #f0f0f0;
  display: flex;
  gap: 12px;
}

.action-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.password-btn {
  background: #fff7e6;
  color: #fa8c16;
  border: 1px solid #ffd591;
}

.password-btn:hover {
  background: #ffe7ba;
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: #ffffff;
  border-radius: 12px;
  width: 90%;
  max-width: 450px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-header h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #999;
  cursor: pointer;
  line-height: 1;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #f0f0f0;
  color: #333;
}

.modal-body {
  padding: 24px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s;
}

.form-input:focus {
  border-color: #003366;
  outline: none;
  box-shadow: 0 0 0 2px rgba(0, 51, 102, 0.1);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
}

.modal-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.modal-btn.cancel {
  background: #f0f0f0;
  color: #666;
}

.modal-btn.cancel:hover {
  background: #e0e0e0;
}

.modal-btn.confirm {
  background: #003366;
  color: #ffffff;
}

.modal-btn.confirm:hover {
  background: #004080;
}

@media (max-width: 768px) {
  .profile-container {
    padding: 20px 10px;
  }

  .header-content {
    padding: 0 20px 15px;
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .profile-avatar {
    width: 100px;
    height: 100px;
  }

  .profile-name {
    font-size: 22px;
  }

  .profile-body {
    padding: 20px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .action-section {
    flex-direction: column;
  }

  .action-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
