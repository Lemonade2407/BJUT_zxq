<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { login } from '@/api/auth'
import { toast } from '@/utils/toast'
import { error as logError } from '@/utils/logger'
import tokenManager from '@/utils/tokenManager'

const router = useRouter()
const route = useRoute()

// 表单数据
const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false
})

// 表单验证状态
const errors = reactive({
  username: '',
  password: ''
})

// UI 状态
const isLoading = ref(false)
const showPassword = ref(false)
const errorMessage = ref('')
const isLocked = ref(false)
const lockMessage = ref('')

// 验证用户名
const validateUsername = () => {
  if (!loginForm.username) {
    errors.username = '请输入用户名/邮箱/身份标识号'
    return false
  }
  errors.username = ''
  return true
}

// 验证密码
const validatePassword = () => {
  if (!loginForm.password) {
    errors.password = '请输入密码'
    return false
  }
  errors.password = ''
  return true
}

// 处理登录
const handleLogin = async () => {
  
  // 验证表单
  const isUsernameValid = validateUsername()
  const isPasswordValid = validatePassword()
  
  if (!isUsernameValid || !isPasswordValid) {
    return
  }
  
  isLoading.value = true
  errorMessage.value = ''
  
  try {
    // 调用登录 API
    const res = await login({
      username: loginForm.username.trim(),
      password: loginForm.password
    })
    
    if (res.code === 200) {
      // 使用 tokenManager 保存用户信息（Token 由后端通过 httpOnly Cookie 管理）
      const loginData = res.data
      
      // 构建用户信息对象（从 LoginResponse 中提取）
      const userInfo = {
        id: loginData.id,
        username: loginData.username,
        email: loginData.email,
        avatar: loginData.avatar,
        employeeId: loginData.employeeId,
        role: loginData.role
      }
      
      tokenManager.saveUserInfo(userInfo)

      // 弱密码强制跳转改密
      if (loginData.mustChangePassword) {
        toast.warning('您的密码强度不足，为了账户安全，请修改密码')
        await router.replace('/profile?mustChangePassword=true')
        return
      }

      // 登录成功,根据角色跳转到不同页面
      const userRole = loginData.role
      let redirect = route.query.redirect

      // 如果没有指定重定向路径，根据角色决定跳转目标
      if (!redirect) {
        if (userRole === 'ADMIN') {
          redirect = '/admin'
        } else {
          redirect = '/home'
        }
      }

      await router.replace(redirect)
    }
  } catch (err) {
    logError('登录失败:', err)
    const msg = err.message || '登录失败，请检查用户名和密码'
    // 暴力破解锁定：显示红色警告，禁用按钮
    if (msg.includes('登录失败次数过多')) {
      isLocked.value = true
      lockMessage.value = msg
      toast.error('账户已被临时锁定')
    } else {
      toast.error(msg)
    }
    errorMessage.value = msg
  } finally {
    isLoading.value = false
  }
}

// 切换密码显示
const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}

// 清空错误信息
const clearError = (field) => {
  if (field === 'username') {
    errors.username = ''
  } else if (field === 'password') {
    errors.password = ''
  }
  errorMessage.value = ''
  isLocked.value = false
  lockMessage.value = ''
}

// 组件挂载时加载记住的用户名
onMounted(() => {
  // 加载记住的用户名
  const rememberedUser = localStorage.getItem('rememberedUser')
  if (rememberedUser) {
    loginForm.username = rememberedUser
    loginForm.rememberMe = true
  }
})

// 切换到注册页面
const goToRegister = () => {
  router.push('/register')
}
</script>

<template>
  <div class="login-container">
    <div class="login-box">
      <!-- Logo 和标题 -->
      <div class="login-header">
        <img src="/logo.svg" alt="logo" class="login-logo" />
        <h1 class="login-title">欢迎登录</h1>
        <p class="login-subtitle">工大项目分享平台</p>
      </div>

      <!-- 登录表单 -->
      <form @submit.prevent="handleLogin" class="login-form">
        <!-- 用户名输入框 -->
        <div class="form-group">
          <label for="username" class="form-label">
            <svg class="form-icon" viewBox="0 0 16 16" width="16" height="16">
              <path d="M8 8a3 3 0 100-6 3 3 0 000 6zm0 1c-2 0-6 1-6 3v1h12v-1c0-2-4-3-6-3z"/>
            </svg>
            用户名
          </label>
          <input
            id="username"
            v-model="loginForm.username"
            type="text"
            class="form-input"
            :class="{ error: errors.username }"
            placeholder="请输入用户名/邮箱/身份标识号"
            @blur="validateUsername"
            @input="clearError('username')"
          />
          <span v-if="errors.username" class="error-message">{{ errors.username }}</span>
        </div>

        <!-- 密码输入框 -->
        <div class="form-group">
          <label for="password" class="form-label">
            <svg class="form-icon" viewBox="0 0 16 16" width="16" height="16">
              <path d="M8 2a2 2 0 012 2v2H6V4a2 2 0 012-2zm3 6V4a3 3 0 10-6 0v4H2v6h12V8h-3z"/>
            </svg>
            密码
          </label>
          <div class="password-input-wrapper">
            <input
              id="password"
              v-model="loginForm.password"
              :type="showPassword ? 'text' : 'password'"
              class="form-input"
              :class="{ error: errors.password }"
              placeholder="请输入密码"
              @blur="validatePassword"
              @input="clearError('password')"
            />
            <button
              type="button"
              class="password-toggle"
              @click="togglePasswordVisibility"
              tabindex="-1"
            >
              <svg v-if="!showPassword" viewBox="0 0 16 16" width="16" height="16">
                <path d="M8 3C5 3 2.5 5 1 8c1.5 3 4 5 7 5s5.5-2 7-5c-1.5-3-4-5-7-5zm0 8a3 3 0 110-6 3 3 0 010 6zm0-1.5a1.5 1.5 0 100-3 1.5 1.5 0 000 3z"/>
              </svg>
              <svg v-else viewBox="0 0 16 16" width="16" height="16">
                <path d="M13.86 2.75a1 1 0 011.41 0l.03.03a1 1 0 010 1.41L3.7 15.8a1 1 0 01-1.41 0l-.03-.03a1 1 0 010-1.41L13.86 2.75zM8 3C5 3 2.5 5 1 8c.5 1 1.2 1.9 2 2.6L1.7 12a1 1 0 001.4 1.4L15.3 2.7a1 1 0 00-1.4-1.4l-1.4 1.4C11.2 2.3 9.6 3 8 3zm0 10c-1.3 0-2.5-.4-3.5-1.1l1.4-1.4c.6.3 1.3.5 2.1.5 2.8 0 5-2.2 5-5 0-.8-.2-1.5-.5-2.1l1.4-1.4c.7 1 1.1 2.2 1.1 3.5 0 3-2.5 7-7 7z"/>
              </svg>
            </button>
          </div>
          <span v-if="errors.password" class="error-message">{{ errors.password }}</span>
        </div>

        <!-- 记住我 -->
        <div class="form-options">
          <label class="checkbox-label">
            <input
              v-model="loginForm.rememberMe"
              type="checkbox"
              class="checkbox-input"
            />
            <span class="checkbox-text">记住我</span>
          </label>
          <a href="#" class="forgot-link">忘记密码？</a>
        </div>

        <!-- 暴力破解锁定提示 -->
        <div v-if="isLocked" class="lockout-banner">
          <svg class="lockout-icon" viewBox="0 0 16 16" width="18" height="18">
            <path d="M8 1a3.5 3.5 0 00-3.5 3.5V7H3a1 1 0 00-1 1v5a1 1 0 001 1h10a1 1 0 001-1V8a1 1 0 00-1-1h-1.5V4.5A3.5 3.5 0 008 1zm2.5 6h-5V4.5a2.5 2.5 0 015 0V7z"/>
          </svg>
          <div class="lockout-text">
            <strong>账户已被临时锁定</strong>
            <span>{{ lockMessage }}</span>
          </div>
        </div>

        <!-- 错误提示 -->
        <div v-else-if="errorMessage" class="global-error">
          <svg class="error-icon" viewBox="0 0 16 16" width="16" height="16">
            <path d="M8 16A8 8 0 108 0a8 8 0 000 8zm1-4.5V6H7v5.5h2zm0-7V3H7v1.5h2z"/>
          </svg>
          <span>{{ errorMessage }}</span>
        </div>

        <!-- 登录按钮 -->
        <button type="submit" class="login-button" :disabled="isLoading || isLocked">
          <span v-if="!isLoading">登 录</span>
          <span v-else class="loading-spinner">
            <svg class="spinner" viewBox="0 0 16 16" width="16" height="16">
              <circle cx="8" cy="8" r="7" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>
            </svg>
            登录中...
          </span>
        </button>
      </form>

      <!-- 底部链接 -->
      <div class="login-footer">
        <p>还没有账号？<a href="#" @click.prevent="goToRegister" class="register-link">立即注册</a></p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  height: 100%;
  background: linear-gradient(135deg, #064e3b 0%, #065f46 50%, #047857 100%);
  padding: 20px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow-y: auto;
}

.login-box {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(6, 78, 59, 0.3);
  padding: 40px;
  width: 100%;
  max-width: 420px;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  height: 80px;
  width: auto;
  margin-bottom: 16px;
}

.login-title {
  font-size: 28px;
  font-weight: 600;
  color: #064e3b;
  margin: 0 0 8px 0;
}

.login-subtitle {
  font-size: 14px;
  color: #666666;
  margin: 0;
}

/* 表单样式 */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #333333;
}

.form-icon {
  fill: #666666;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 14px;
  color: #333333;
  transition: all 0.2s;
  outline: none;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.form-input.error {
  border-color: #d93025;
}

.form-input::placeholder {
  color: #999999;
}

.password-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.password-toggle {
  position: absolute;
  right: 12px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.password-toggle svg {
  fill: #999999;
  transition: fill 0.2s;
}

.password-toggle:hover svg {
  fill: #10b981;
}

.error-message {
  font-size: 12px;
  color: #d93025;
  margin-top: 4px;
}

/* 表单选项 */
.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #666666;
  user-select: none;
}

.checkbox-input {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #10b981;
}

.checkbox-text {
  cursor: pointer;
}

.forgot-link {
  font-size: 13px;
  color: #10b981;
  text-decoration: none;
  transition: color 0.2s;
}

.forgot-link:hover {
  color: #059669;
  text-decoration: underline;
}

/* 全局错误提示 */
.global-error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background-color: rgba(217, 48, 37, 0.1);
  border: 1px solid #d93025;
  border-radius: 6px;
  color: #d93025;
  font-size: 13px;
}

.error-icon {
  fill: #d93025;
  flex-shrink: 0;
}

/* 暴力破解锁定横幅 */
.lockout-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  background: #fef2f2;
  border: 2px solid #ef4444;
  border-radius: 8px;
  color: #991b1b;
  font-size: 13px;
}

.lockout-icon {
  fill: #ef4444;
  flex-shrink: 0;
}

.lockout-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.lockout-text strong {
  font-size: 14px;
}

/* 登录按钮 */
.login-button {
  padding: 14px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

.login-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
  transform: translateY(-1px);
}

.login-button:active:not(:disabled) {
  transform: translateY(0);
}

.login-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-spinner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 底部区域 */
.login-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e0e0e0;
}

.login-footer p {
  font-size: 14px;
  color: #666666;
  margin: 0;
}

.register-link {
  color: #10b981;
  font-weight: 500;
  text-decoration: none;
  transition: color 0.2s;
}

.register-link:hover {
  color: #059669;
  text-decoration: underline;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-box {
    padding: 32px 24px;
  }
  
  .login-title {
    font-size: 24px;
  }
  
  .form-options {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
