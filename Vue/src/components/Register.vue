<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'
import { getCaptcha, checkPasswordStrength } from '@/api/captcha'

const router = useRouter()

// 表单数据
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  studentId: '',
  email: '',
  phone: '',
  sex: '未知',
  bio: '',
  captchaCode: '',
  captchaSessionId: ''
})

// 表单验证状态
const errors = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  studentId: '',
  email: '',
  phone: '',
  captcha: ''
})

// UI 状态
const isLoading = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const captchaImage = ref('')
const isCaptchaLoading = ref(false)
const passwordStrength = ref(null)

// 获取图形验证码
const fetchCaptcha = async () => {
  try {
    isCaptchaLoading.value = true
    errorMessage.value = '' // 清空之前的错误信息
    
    console.log('开始获取验证码...')
    const res = await getCaptcha()
    console.log('验证码响应:', res)
    
    if (res.code === 200) {
      registerForm.captchaSessionId = res.data.sessionId
      // 如果后端返回的是纯Base64字符串，需要添加Data URI前缀
      let imageData = res.data.image
      console.log('原始图片数据:', imageData ? imageData.substring(0, 50) + '...' : 'null')
      
      if (imageData && !imageData.startsWith('data:')) {
        // 判断图片格式（通常后端返回的是PNG或JPEG）
        imageData = `data:image/png;base64,${imageData}`
      }
      captchaImage.value = imageData
      console.log('验证码加载成功')
    }
  } catch (error) {
    console.error('获取验证码失败:', error)
    console.error('错误详情:', error.response?.status, error.response?.data)
    
    // 显示更详细的错误信息
    if (error.response?.status === 404) {
      errorMessage.value = '验证码接口不存在，请检查后端服务是否正确启动，或联系管理员确认接口路径'
    } else if (error.message.includes('后端服务')) {
      errorMessage.value = '无法连接到服务器，请确保后端服务已启动（http://localhost:8080）'
    } else if (error.message.includes('超时')) {
      errorMessage.value = '请求超时，请检查网络连接'
    } else {
      errorMessage.value = error.message || '获取验证码失败，请点击验证码图片重试'
    }
  } finally {
    isCaptchaLoading.value = false
  }
}

// 检查密码强度
const checkStrength = async () => {
  if (registerForm.password && registerForm.password.length >= 6) {
    try {
      const res = await checkPasswordStrength(registerForm.password)
      if (res.code === 200) {
        passwordStrength.value = res.data
      }
    } catch (error) {
      console.error('检查密码强度失败:', error)
    }
  } else {
    passwordStrength.value = null
  }
}

// 验证用户名
const validateUsername = () => {
  if (!registerForm.username) {
    errors.username = '用户名不能为空'
    return false
  }
  if (registerForm.username.length < 2 || registerForm.username.length > 20) {
    errors.username = '用户名长度应为 2-20 位'
    return false
  }
  errors.username = ''
  return true
}

// 验证密码
const validatePassword = () => {
  if (!registerForm.password) {
    errors.password = '密码不能为空'
    return false
  }
  if (registerForm.password.length < 6 || registerForm.password.length > 20) {
    errors.password = '密码长度应为 6-20 位'
    return false
  }
  // 密码必须包含字母和数字
  if (!/^(?=.*[a-zA-Z])(?=.*\d).+$/.test(registerForm.password)) {
    errors.password = '密码必须包含字母和数字'
    return false
  }
  errors.password = ''
  return true
}

// 验证确认密码
const validateConfirmPassword = () => {
  if (!registerForm.confirmPassword) {
    errors.confirmPassword = '请确认密码'
    return false
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    errors.confirmPassword = '两次输入的密码不一致'
    return false
  }
  errors.confirmPassword = ''
  return true
}

// 验证学号
const validateStudentId = () => {
  if (!registerForm.studentId) {
    errors.studentId = '学号不能为空'
    return false
  }
  // 学号格式：8位数字 或 1位字母+8位数字
  if (!/^([A-Za-z]?\d{8})$/.test(registerForm.studentId)) {
    errors.studentId = '学号格式不正确，应为8位数字或1位字母+8位数字'
    return false
  }
  errors.studentId = ''
  return true
}

// 验证邮箱
const validateEmail = () => {
  if (!registerForm.email) {
    errors.email = '邮箱不能为空'
    return false
  }
  if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/.test(registerForm.email)) {
    errors.email = '邮箱格式不正确'
    return false
  }
  errors.email = ''
  return true
}

// 验证手机号
const validatePhone = () => {
  if (registerForm.phone && !/^1[3-9]\d{9}$/.test(registerForm.phone)) {
    errors.phone = '手机号格式不正确'
    return false
  }
  errors.phone = ''
  return true
}

// 验证验证码
const validateCaptcha = () => {
  if (!registerForm.captchaCode) {
    errors.captcha = '请输入验证码'
    return false
  }
  if (registerForm.captchaCode.length !== 4) {
    errors.captcha = '验证码为4位字符'
    return false
  }
  errors.captcha = ''
  return true
}

// 处理注册
const handleRegister = async () => {
  // TODO: 添加表单提交防抖，防止重复提交
  // TODO: 添加邮箱验证功能（发送验证邮件）
  
  // 验证所有字段
  const validations = [
    validateUsername(),
    validatePassword(),
    validateConfirmPassword(),
    validateStudentId(),
    validateEmail(),
    validatePhone(),
    validateCaptcha()
  ]
  
  if (!validations.every(v => v)) {
    return
  }
  
  isLoading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  
  try {
    // 调用注册 API
    const res = await register({
      username: registerForm.username.trim(),
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      studentId: registerForm.studentId.trim(),
      email: registerForm.email.trim(),
      phone: registerForm.phone?.trim() || null,
      sex: registerForm.sex,
      bio: registerForm.bio?.trim() || null,
      captchaSessionId: registerForm.captchaSessionId,
      captchaCode: registerForm.captchaCode
    })
    
    if (res.code === 200) {
      successMessage.value = '注册成功！即将跳转到登录页面...'
      
      // TODO: 考虑添加邮箱验证步骤，而非直接跳转
      // 2秒后跳转到登录页
      setTimeout(() => {
        router.push('/login')
      }, 2000)
    }
  } catch (error) {
    console.error('注册失败:', error)
    errorMessage.value = error.message || '注册失败，请稍后重试'
    // 刷新验证码
    fetchCaptcha()
    registerForm.captchaCode = ''
  } finally {
    isLoading.value = false
  }
}

// 切换密码显示
const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}

const toggleConfirmPasswordVisibility = () => {
  showConfirmPassword.value = !showConfirmPassword.value
}

// 清空错误信息
const clearError = (field) => {
  if (errors[field]) {
    errors[field] = ''
  }
  errorMessage.value = ''
}

// 组件挂载时加载验证码
onMounted(() => {
  fetchCaptcha()
})

// 切换到登录页面
const goToLogin = () => {
  router.push('/login')
}

// 获取密码强度颜色
const getStrengthColor = (level) => {
  const colors = {
    WEAK: '#d93025',
    MEDIUM: '#f9ab00',
    STRONG: '#34a853',
    VERY_STRONG: '#0059b3'
  }
  return colors[level] || '#999999'
}
</script>

<template>
  <div class="register-container">
    <div class="register-box">
      <!-- Logo 和标题 -->
      <div class="register-header">
        <img src="/logo.svg" alt="logo" class="register-logo" />
        <h1 class="register-title">用户注册</h1>
        <p class="register-subtitle">加入工大项目协作平台</p>
      </div>

      <!-- 注册表单 -->
      <form @submit.prevent="handleRegister" class="register-form">
        <!-- 用户名 -->
        <div class="form-group">
          <label for="username" class="form-label">用户名 *</label>
          <input
            id="username"
            v-model="registerForm.username"
            type="text"
            class="form-input"
            :class="{ error: errors.username }"
            placeholder="请输入用户名（2-20位）"
            @blur="validateUsername"
            @input="clearError('username')"
          />
          <span v-if="errors.username" class="error-message">{{ errors.username }}</span>
        </div>

        <!-- 学号 -->
        <div class="form-group">
          <label for="studentId" class="form-label">学号 *</label>
          <input
            id="studentId"
            v-model="registerForm.studentId"
            type="text"
            class="form-input"
            :class="{ error: errors.studentId }"
            placeholder="例如：20230101 或 B20230101"
            @blur="validateStudentId"
            @input="clearError('studentId')"
          />
          <span v-if="errors.studentId" class="error-message">{{ errors.studentId }}</span>
        </div>

        <!-- 邮箱 -->
        <div class="form-group">
          <label for="email" class="form-label">邮箱 *</label>
          <input
            id="email"
            v-model="registerForm.email"
            type="email"
            class="form-input"
            :class="{ error: errors.email }"
            placeholder="请输入邮箱地址"
            @blur="validateEmail"
            @input="clearError('email')"
          />
          <span v-if="errors.email" class="error-message">{{ errors.email }}</span>
        </div>

        <!-- 密码 -->
        <div class="form-group">
          <label for="password" class="form-label">密码 *</label>
          <div class="password-input-wrapper">
            <input
              id="password"
              v-model="registerForm.password"
              :type="showPassword ? 'text' : 'password'"
              class="form-input"
              :class="{ error: errors.password }"
              placeholder="请输入密码（6-20位，含字母和数字）"
              @blur="validatePassword"
              @input="clearError('password'); checkStrength()"
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
          
          <!-- 密码强度提示 -->
          <div v-if="passwordStrength" class="password-strength">
            <div class="strength-bar">
              <div 
                class="strength-fill" 
                :style="{ 
                  width: (passwordStrength.score / 100 * 100) + '%',
                  backgroundColor: getStrengthColor(passwordStrength.level)
                }"
              ></div>
            </div>
            <div class="strength-info">
              <span :style="{ color: getStrengthColor(passwordStrength.level) }">
                {{ passwordStrength.levelDescription }}：
              </span>
              <span v-if="passwordStrength.suggestion" class="strength-suggestion">
                {{ passwordStrength.suggestion }}
              </span>
            </div>
          </div>
        </div>

        <!-- 确认密码 -->
        <div class="form-group">
          <label for="confirmPassword" class="form-label">确认密码 *</label>
          <div class="password-input-wrapper">
            <input
              id="confirmPassword"
              v-model="registerForm.confirmPassword"
              :type="showConfirmPassword ? 'text' : 'password'"
              class="form-input"
              :class="{ error: errors.confirmPassword }"
              placeholder="请再次输入密码"
              @blur="validateConfirmPassword"
              @input="clearError('confirmPassword')"
            />
            <button
              type="button"
              class="password-toggle"
              @click="toggleConfirmPasswordVisibility"
              tabindex="-1"
            >
              <svg v-if="!showConfirmPassword" viewBox="0 0 16 16" width="16" height="16">
                <path d="M8 3C5 3 2.5 5 1 8c1.5 3 4 5 7 5s5.5-2 7-5c-1.5-3-4-5-7-5zm0 8a3 3 0 110-6 3 3 0 010 6zm0-1.5a1.5 1.5 0 100-3 1.5 1.5 0 000 3z"/>
              </svg>
              <svg v-else viewBox="0 0 16 16" width="16" height="16">
                <path d="M13.86 2.75a1 1 0 011.41 0l.03.03a1 1 0 010 1.41L3.7 15.8a1 1 0 01-1.41 0l-.03-.03a1 1 0 010-1.41L13.86 2.75zM8 3C5 3 2.5 5 1 8c.5 1 1.2 1.9 2 2.6L1.7 12a1 1 0 001.4 1.4L15.3 2.7a1 1 0 00-1.4-1.4l-1.4 1.4C11.2 2.3 9.6 3 8 3zm0 10c-1.3 0-2.5-.4-3.5-1.1l1.4-1.4c.6.3 1.3.5 2.1.5 2.8 0 5-2.2 5-5 0-.8-.2-1.5-.5-2.1l1.4-1.4c.7 1 1.1 2.2 1.1 3.5 0 3-2.5 7-7 7z"/>
              </svg>
            </button>
          </div>
          <span v-if="errors.confirmPassword" class="error-message">{{ errors.confirmPassword }}</span>
        </div>

        <!-- 手机号（可选） -->
        <div class="form-group">
          <label for="phone" class="form-label">手机号（可选）</label>
          <input
            id="phone"
            v-model="registerForm.phone"
            type="tel"
            class="form-input"
            :class="{ error: errors.phone }"
            placeholder="请输入手机号"
            @blur="validatePhone"
            @input="clearError('phone')"
          />
          <span v-if="errors.phone" class="error-message">{{ errors.phone }}</span>
        </div>

        <!-- 图形验证码 -->
        <div class="form-group">
          <label for="captcha" class="form-label">验证码 *</label>
          <div class="captcha-wrapper">
            <input
              id="captcha"
              v-model="registerForm.captchaCode"
              type="text"
              class="form-input captcha-input"
              :class="{ error: errors.captcha }"
              placeholder="请输入验证码"
              maxlength="4"
              @blur="validateCaptcha"
              @input="clearError('captcha')"
            />
            <div 
              class="captcha-image" 
              @click="fetchCaptcha"
              title="点击刷新验证码"
            >
              <img 
                v-if="captchaImage && !isCaptchaLoading" 
                :src="captchaImage" 
                alt="验证码"
              />
              <div v-else class="captcha-loading">
                <span class="loading-spinner">⟳</span>
              </div>
            </div>
          </div>
          <span v-if="errors.captcha" class="error-message">{{ errors.captcha }}</span>
        </div>

        <!-- 错误提示 -->
        <div v-if="errorMessage" class="global-error">
          <svg class="error-icon" viewBox="0 0 16 16" width="16" height="16">
            <path d="M8 16A8 8 0 108 0a8 8 0 000 8zm1-4.5V6H7v5.5h2zm0-7V3H7v1.5h2z"/>
          </svg>
          <span>{{ errorMessage }}</span>
        </div>

        <!-- 成功提示 -->
        <div v-if="successMessage" class="global-success">
          <svg class="success-icon" viewBox="0 0 16 16" width="16" height="16">
            <path d="M8 16A8 8 0 108 0a8 8 0 000 8zm3.78-9.72a.75.75 0 00-1.06-1.06L6.5 9.44 5.28 8.22a.75.75 0 00-1.06 1.06l1.75 1.75a.75.75 0 001.06 0l4.75-4.75z"/>
          </svg>
          <span>{{ successMessage }}</span>
        </div>

        <!-- 注册按钮 -->
        <button type="submit" class="register-button" :disabled="isLoading">
          <span v-if="!isLoading">注 册</span>
          <span v-else class="loading-spinner">
            <svg class="spinner" viewBox="0 0 16 16" width="16" height="16">
              <circle cx="8" cy="8" r="7" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>
            </svg>
            注册中...
          </span>
        </button>
      </form>

      <!-- 底部链接 -->
      <div class="register-footer">
        <p>已有账号？<a href="#" @click.prevent="goToLogin" class="login-link">立即登录</a></p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #003366 0%, #004080 50%, #0059b3 100%);
  padding: 20px;
}

.register-box {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 51, 102, 0.3);
  padding: 40px;
  width: 100%;
  max-width: 520px;
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

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.register-logo {
  height: 80px;
  width: auto;
  margin-bottom: 16px;
}

.register-title {
  font-size: 28px;
  font-weight: 600;
  color: #003366;
  margin: 0 0 8px 0;
}

.register-subtitle {
  font-size: 14px;
  color: #666666;
  margin: 0;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: #333333;
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
  border-color: #0059b3;
  box-shadow: 0 0 0 3px rgba(0, 89, 179, 0.1);
}

.form-input.error {
  border-color: #d93025;
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
  fill: #0059b3;
}

/* 密码强度 */
.password-strength {
  margin-top: 8px;
}

.strength-bar {
  height: 4px;
  background-color: #e0e0e0;
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 4px;
}

.strength-fill {
  height: 100%;
  transition: all 0.3s;
  border-radius: 2px;
}

.strength-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.strength-suggestion {
  color: #666666;
}

/* 验证码 */
.captcha-wrapper {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-image {
  width: 120px;
  height: 48px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
}

.captcha-image:hover {
  border-color: #0059b3;
  box-shadow: 0 0 0 3px rgba(0, 89, 179, 0.1);
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #0059b3;
  animation: spin 1s linear infinite;
}

.error-message {
  font-size: 12px;
  color: #d93025;
  margin-top: 4px;
}

.global-error,
.global-success {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border-radius: 6px;
  font-size: 13px;
}

.global-error {
  background-color: rgba(217, 48, 37, 0.1);
  border: 1px solid #d93025;
  color: #d93025;
}

.global-success {
  background-color: rgba(52, 168, 83, 0.1);
  border: 1px solid #34a853;
  color: #34a853;
}

.error-icon,
.success-icon {
  flex-shrink: 0;
}

.error-icon {
  fill: #d93025;
}

.success-icon {
  fill: #34a853;
}

.register-button {
  padding: 14px;
  background: linear-gradient(135deg, #0059b3 0%, #003366 100%);
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 89, 179, 0.3);
  margin-top: 8px;
}

.register-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #004080 0%, #003366 100%);
  box-shadow: 0 4px 12px rgba(0, 89, 179, 0.4);
  transform: translateY(-1px);
}

.register-button:active:not(:disabled) {
  transform: translateY(0);
}

.register-button:disabled {
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

.register-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e0e0e0;
}

.register-footer p {
  font-size: 14px;
  color: #666666;
  margin: 0;
}

.login-link {
  color: #0059b3;
  font-weight: 500;
  text-decoration: none;
  transition: color 0.2s;
}

.login-link:hover {
  color: #003366;
  text-decoration: underline;
}

@media (max-width: 480px) {
  .register-box {
    padding: 32px 24px;
  }
  
  .register-title {
    font-size: 24px;
  }
}
</style>
