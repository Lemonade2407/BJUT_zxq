# 前端代码优化建议

## 🔴 高优先级问题

### 1. **API 路径重复问题** ⚠️ 严重
**位置**: `src/utils/request.js` + `src/api/*.js`

**问题描述**:
- `request.js` 中 baseURL 设置为 `/api`
- API 文件中 URL 又添加了 `/api` 前缀
- 导致实际请求路径变成 `/api/api/xxx`（双重前缀）

**影响文件**:
- `src/api/auth.js` - 所有接口都有 `/api` 前缀
- `src/api/project.js` - 所有接口都有 `/api` 前缀
- `src/api/captcha.js` - 所有接口都有 `/api` 前缀

**修复方案**:
```javascript
// request.js
const request = axios.create({
  baseURL: '/api', // ✅ 保留
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

// auth.js - 移除 /api 前缀
export function login(data) {
  return request({
    url: '/auth/login',  // ❌ 原来是 '/api/auth/login'
    method: 'post',
    data
  })
}
```

---

### 2. **Vite 代理配置冲突** ⚠️ 严重
**位置**: `vite.config.js`

**问题描述**:
```javascript
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
    rewrite: (path) => path.replace(/^\/api/, '') // ❌ 移除了 /api
  }
}
```
- 代理会将 `/api/xxx` 重写为 `/xxx`
- 但后端接口可能期望 `/api/xxx` 路径
- 需要确认后端接口的实际路径

**修复方案**:
```javascript
// 方案1: 如果后端接口是 /api/xxx，移除 rewrite
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
    // 不重写，保持 /api 前缀
  }
}

// 方案2: 如果后端接口是 /xxx，保持当前配置
// 但需要修改 API 文件，移除 /api 前缀
```

---

### 3. **路由系统缺失** ⚠️ 中等
**位置**: 整个项目

**问题描述**:
- 使用简单的 `currentPage` 状态管理页面切换
- 没有真正的路由系统（vue-router）
- 无法直接通过 URL 访问特定页面
- 浏览器前进/后退按钮无效
- 无法分享特定页面的链接

**修复方案**:
```bash
npm install vue-router@4
```

创建 `src/router/index.js`:
```javascript
import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/components/Login.vue'
import Register from '@/components/Register.vue'
import Main from '@/components/Main.vue'
import UserRepository from '@/components/UserRepository.vue'
import ProjectSquare from '@/components/ProjectSquare.vue'
import ProjectDetail from '@/components/ProjectDetail.vue'

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/login', component: Login },
  { path: '/register', component: Register },
  { path: '/home', component: Main, meta: { requiresAuth: true } },
  { path: '/repository', component: UserRepository, meta: { requiresAuth: true } },
  { path: '/projects', component: ProjectSquare, meta: { requiresAuth: true } },
  { path: '/project/:id', component: ProjectDetail, meta: { requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
```

---

## 🟡 中优先级问题

### 4. **缺少统一的错误提示组件**
**位置**: 所有组件

**问题描述**:
- 使用 `alert()` 显示错误信息（体验差）
- 没有 Toast/Message 通知组件
- 错误提示样式不统一

**修复方案**:
创建 `src/components/Toast.vue`:
```vue
<script setup>
import { ref } from 'vue'

const visible = ref(false)
const message = ref('')
const type = ref('info') // success, error, warning, info

const show = (msg, toastType = 'info') => {
  message.value = msg
  type.value = toastType
  visible.value = true
  setTimeout(() => {
    visible.value = false
  }, 3000)
}

defineExpose({ show })
</script>

<template>
  <Transition name="toast">
    <div v-if="visible" :class="['toast', type]">
      {{ message }}
    </div>
  </Transition>
</template>
```

---

### 5. **硬编码的模拟数据**
**位置**: 
- `UserRepository.vue` - 3个模拟项目
- `ProjectDetail.vue` - 模拟项目详情和评论
- `ProjectSquare.vue` - 空项目列表

**问题描述**:
- 应该从后端 API 获取真实数据
- 目前所有页面都是静态数据

**修复方案**:
实现真实的 API 调用，移除模拟数据

---

### 6. **Console.log 调试代码未清理**
**位置**: 多个组件

**发现的 console 语句**:
- `App.vue`: 2处
- `Header.vue`: 3处
- `Login.vue`: 1处
- `Register.vue`: 7处
- `UserRepository.vue`: 4处
- `ProjectDetail.vue`: 3处

**修复方案**:
- 生产环境应移除或使用环境变量控制
```javascript
if (import.meta.env.DEV) {
  console.log('调试信息')
}
```

---

### 7. **TODO 标记未完成的功能**
**位置**: 多个组件

**待完成功能**:
1. `Header.vue:L54` - 搜索功能
2. `ProjectDetail.vue:L41` - 加载项目详情 API
3. `ProjectDetail.vue:L144` - 提交评论 API
4. `ProjectDetail.vue:L171` - 下载项目 API
5. `UserRepository.vue:L71` - 加载用户项目 API

---

### 8. **缺少加载状态管理**
**位置**: 多个组件

**问题描述**:
- `ProjectSquare.vue` 没有加载状态
- 部分组件有 isLoading 但未在模板中使用

**修复方案**:
统一添加骨架屏或加载动画

---

## 🟢 低优先级问题

### 9. **CSS 重复代码**
**位置**: 多个组件

**问题描述**:
- 相同的样式在多个组件中重复定义
- 例如：`.loading-state`, `.empty-state`, `.pagination-container`

**修复方案**:
提取公共样式到 `main.css` 或创建 `styles/common.css`

---

### 10. **响应式设计不完善**
**位置**: 多个组件

**问题描述**:
- 部分组件只有基础的移动端适配
- 缺少平板设备的适配（768px - 1024px）

---

### 11. **缺少图片懒加载**
**位置**: 所有显示图片的地方

**问题描述**:
- 用户头像、项目封面等没有懒加载
- 影响首屏加载性能

**修复方案**:
使用 `loading="lazy"` 属性或 Intersection Observer

---

### 12. **缺少错误边界处理**
**位置**: 根组件

**问题描述**:
- 没有全局错误边界
- 单个组件崩溃会影响整个应用

**修复方案**:
```javascript
// App.vue
import { onErrorCaptured } from 'vue'

onErrorCaptured((err, instance, info) => {
  console.error('捕获到错误:', err, info)
  // 显示错误提示
  return false // 阻止错误继续传播
})
```

---

### 13. **localStorage 操作分散**
**位置**: 多个组件

**问题描述**:
- Token 和用户信息在多处读写
- 没有统一的存储管理

**修复方案**:
创建 `src/utils/storage.js`:
```javascript
export const storage = {
  getToken() {
    return localStorage.getItem('token')
  },
  setToken(token) {
    localStorage.setItem('token', token)
  },
  removeToken() {
    localStorage.removeItem('token')
  },
  getUser() {
    const user = localStorage.getItem('user')
    return user ? JSON.parse(user) : null
  },
  setUser(user) {
    localStorage.setItem('user', JSON.stringify(user))
  },
  removeUser() {
    localStorage.removeItem('user')
  },
  clear() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }
}
```

---

### 14. **缺少表单防抖**
**位置**: `Register.vue` - 密码强度检查

**问题描述**:
- 每次输入都触发 API 请求
- 应该使用防抖减少请求次数

**修复方案**:
```javascript
import { debounce } from 'lodash-es'

const checkPasswordStrength = debounce(async (password) => {
  // API 调用
}, 500)
```

---

### 15. **魔法数字和字符串**
**位置**: 多个组件

**问题描述**:
- `PAGE_SIZE = 5` 或 `6` 硬编码
- 颜色值、间距等魔法数字

**修复方案**:
创建 `src/constants/index.js`:
```javascript
export const PAGE_SIZE = 10
export const MAX_UPLOAD_SIZE = 10 * 1024 * 1024 // 10MB
export const COLORS = {
  primary: '#0059b3',
  secondary: '#003366'
}
```

---

## 📋 优化优先级建议

### 第一阶段（立即修复）
1. ✅ 修复 API 路径重复问题
2. ✅ 确认并修复 Vite 代理配置
3. ✅ 清理 console.log 调试代码

### 第二阶段（近期优化）
4. 实现真实的后端 API 对接
5. 添加统一的错误提示组件
6. 引入 vue-router 路由系统

### 第三阶段（长期改进）
7. 提取公共 CSS 样式
8. 完善响应式设计
9. 添加性能优化（懒加载、防抖等）
10. 建立统一的工具函数库

---

## 🎯 快速修复清单

```bash
# 1. 安装依赖
npm install vue-router@4
npm install lodash-es  # 用于防抖

# 2. 修复 API 路径（批量替换）
# 在所有 api/*.js 文件中，将 '/api/' 替换为 '/'

# 3. 清理 console.log
# 搜索并删除或条件化所有 console 语句

# 4. 创建统一的工具函数
mkdir src/utils
touch src/utils/storage.js
touch src/utils/constants.js
```

---

## 💡 最佳实践建议

1. **代码规范**: 使用 ESLint + Prettier
2. **类型安全**: 考虑迁移到 TypeScript
3. **状态管理**: 复杂状态使用 Pinia
4. **组件库**: 考虑引入 Element Plus 或 Ant Design Vue
5. **单元测试**: 添加 Vitest 测试框架
6. **CI/CD**: 配置自动化部署流程

---

**最后更新**: 2026-03-31
