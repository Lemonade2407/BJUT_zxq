<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template>
  <main class="not-found-page">
    <div class="not-found-card">
      <span class="not-found-icon">🔍</span>
      <h1 class="not-found-title">404</h1>
      <p class="not-found-desc">页面不存在</p>
      <button @click="router.push('/home')" class="home-btn">返回首页</button>
    </div>
  </main>
</template>

<style scoped>
.not-found-page {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background-color: #f5f7fa;
}
.not-found-card {
  text-align: center;
  background: #fff;
  padding: 60px 40px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.not-found-icon { font-size: 64px; display: block; margin-bottom: 16px; }
.not-found-title { font-size: 72px; font-weight: 700; color: #064e3b; margin: 0 0 8px 0; }
.not-found-desc { font-size: 18px; color: #666; margin: 0 0 24px 0; }
.home-btn {
  padding: 10px 32px; background: #10b981; color: #fff; border: none;
  border-radius: 8px; font-size: 15px; cursor: pointer;
}
.home-btn:hover { background: #059669; }
</style>
