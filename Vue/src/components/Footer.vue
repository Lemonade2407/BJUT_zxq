<script setup>
</script>

<template>
  <footer class="app-footer">
    <div class="footer-container">
      <div class="footer-logo">
        <img src="/北京工业大学-logo.svg" alt="北京工业大学校徽" class="footer-logo" />
      </div>
      <p class="footer-tagline">工大知享圈，分享你的项目</p>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  background: linear-gradient(135deg, #003366 0%, #004080 100%);
  border-top: 2px solid #0059b3;
  padding: 20px 32px;
  color: #ffffff;
  width: 100%;
  min-height: 80px;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  box-sizing: border-box;
}

.footer-container {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 16px;
}

.footer-logo {
  height: 64px;
  width: auto;
  cursor: pointer;
  transition: opacity 0.2s;
}

.footer-tagline {
  margin: 0;
  font-size: 1.1rem;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .app-footer {
    padding: 32px 16px 24px;
  }

  .footer-grid {
    grid-template-columns: 1fr 1fr;
    gap: 24px;
  }

  .footer-bottom {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
}
</style>
