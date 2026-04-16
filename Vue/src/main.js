import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { initToast } from '@/utils/toast'

const app = createApp(App)

// 使用路由
app.use(router)

// 初始化 Toast
initToast(app)

app.mount('#app')
