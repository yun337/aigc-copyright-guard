<template>
  <div class="app-root">
    <!-- 顶部导航 -->
    <header class="topbar">
      <div class="topbar-inner">
        <div class="topbar-left" @click="$router.push('/')" style="cursor:pointer">
          <div class="logo-icon">
            <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
              <rect width="32" height="32" rx="8" fill="url(#logo-grad)"/>
              <path d="M16 7L25 12.5V19.5L16 25L7 19.5V12.5L16 7Z" stroke="white" stroke-width="1.2" fill="none"/>
              <circle cx="16" cy="16" r="3.5" fill="white" opacity="0.9"/>
              <defs><linearGradient id="logo-grad" x1="0" y1="0" x2="32" y2="32">
                <stop stop-color="#6C5CE7"/><stop offset="1" stop-color="#00CEC9"/>
              </linearGradient></defs>
            </svg>
          </div>
          <div class="logo-text">
            <span class="logo-name">链创守护</span>
            <span class="logo-sub">CopyrightGuard</span>
          </div>
        </div>

        <nav class="topbar-nav">
          <router-link to="/" class="nav-link" active-class="active">首页</router-link>
          <router-link to="/upload" class="nav-link" active-class="active">上传作品</router-link>
          <router-link to="/detection" class="nav-link" active-class="active">侵权检测</router-link>
          <router-link to="/authorization" class="nav-link" active-class="active">授权管理</router-link>
        </nav>

        <div class="topbar-right">
          <WalletConnect />
          <template v-if="store.isLoggedIn">
            <el-dropdown trigger="click">
              <div class="user-badge">
                <div class="user-avatar">{{ store.username?.charAt(0)?.toUpperCase() }}</div>
                <span class="user-name">{{ store.username }}</span>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/my-works')">我的作品</el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/my-nfts')">我的NFT</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <el-button v-else type="primary" size="small" round @click="$router.push('/login')">登录</el-button>
        </div>
      </div>
    </header>

    <!-- 页面内容 -->
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 底部 -->
    <footer class="footer">
      <div class="footer-inner">
        <span>© 2024 链创守护 — AIGC 作品版权保护平台</span>
        <span class="footer-divider">|</span>
        <span>Powered by Ethereum + IPFS + CLIP AI</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { useUserStore } from '@/store/user'
import WalletConnect from '@/components/WalletConnect.vue'

const store = useUserStore()

const handleLogout = () => {
  store.logout()
  window.location.href = '/'
}
</script>

<style>
/* ============================================
   设计系统 — 明亮现代风格
   ============================================ */

:root {
  --bg-body:      #F8F9FC;
  --bg-white:     #FFFFFF;
  --bg-card:      #FFFFFF;
  --bg-hover:     #F0F1F8;
  --border:       #E8E9F0;
  --border-light: #F0F1F6;
  --text-primary:   #1A1A2E;
  --text-secondary: #5A5A7A;
  --text-muted:     #9494B0;
  --brand-purple: #6C5CE7;
  --brand-cyan:   #00CEC9;
  --brand-amber:  #F59E0B;
  --brand-rose:   #E17055;
  --brand-green:  #00B894;
  --gradient-brand: linear-gradient(135deg, #6C5CE7, #00CEC9);
  --gradient-light: linear-gradient(135deg, rgba(108,92,231,0.04), rgba(0,206,201,0.04));
  --shadow-sm:  0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.06);
  --shadow-md:  0 4px 16px rgba(0,0,0,0.06), 0 2px 4px rgba(0,0,0,0.04);
  --shadow-lg:  0 12px 40px rgba(108,92,231,0.10), 0 4px 12px rgba(0,0,0,0.06);
  --radius-sm: 8px;
  --radius-md: 14px;
  --radius-lg: 20px;
  --radius-xl: 28px;
  --font: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

* { margin: 0; padding: 0; box-sizing: border-box; }

body {
  font-family: var(--font);
  background: var(--bg-body);
  color: var(--text-primary);
  -webkit-font-smoothing: antialiased;
  line-height: 1.5;
}

/* ===== 顶部导航 ===== */
.topbar {
  background: var(--bg-white);
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: var(--shadow-sm);
}

.topbar-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 32px;
  height: 64px;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon { display: flex; }

.logo-name {
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 1px;
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.logo-sub {
  font-size: 10px;
  color: var(--text-muted);
  letter-spacing: 2px;
  text-transform: uppercase;
  display: block;
}

.topbar-nav {
  display: flex;
  gap: 4px;
}

.nav-link {
  padding: 8px 18px;
  border-radius: 8px;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  transition: all 0.2s;
}

.nav-link:hover { color: var(--brand-purple); background: var(--bg-hover); }

.nav-link.active {
  color: var(--brand-purple);
  background: rgba(108,92,231,0.08);
  font-weight: 600;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px 4px 4px;
  border-radius: 100px;
  transition: background 0.2s;
}

.user-badge:hover { background: var(--bg-hover); }

.user-avatar {
  width: 32px; height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 13px;
  color: #fff;
  background: var(--gradient-brand);
}

.user-name { font-size: 14px; color: var(--text-primary); font-weight: 500; }

/* ===== 主内容 ===== */
.main-content {
  max-width: 1280px;
  margin: 0 auto;
  padding: 32px;
  min-height: calc(100vh - 120px);
}

/* ===== 底部 ===== */
.footer {
  background: var(--bg-white);
  border-top: 1px solid var(--border);
  padding: 20px 32px;
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
}

.footer-divider { margin: 0 12px; }

/* ===== 页面过渡 ===== */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s, transform 0.2s; }
.fade-enter-from { opacity: 0; transform: translateY(8px); }
.fade-leave-to   { opacity: 0; }

/* ===== Element Plus 覆写 ===== */
.el-card {
  border: 1px solid var(--border) !important;
  border-radius: var(--radius-md) !important;
  box-shadow: var(--shadow-sm) !important;
}

.el-card__header {
  border-bottom: 1px solid var(--border-light) !important;
  padding: 18px 24px !important;
  font-weight: 600 !important;
}

.el-card__body { padding: 24px !important; }

.el-table {
  --el-table-border-color: var(--border-light) !important;
  --el-table-header-bg-color: #FAFAFC !important;
  --el-table-row-hover-bg-color: rgba(108,92,231,0.03) !important;
}

.el-button--primary {
  --el-button-bg-color: #6C5CE7 !important;
  --el-button-border-color: #6C5CE7 !important;
  --el-button-hover-bg-color: #5A4BD1 !important;
  --el-button-hover-border-color: #5A4BD1 !important;
}

.el-tag { border-radius: 6px !important; font-weight: 500 !important; }

.el-pager li.is-active {
  background: #6C5CE7 !important;
}

.el-input__wrapper {
  border-radius: var(--radius-sm) !important;
  box-shadow: 0 0 0 1px var(--border) !important;
}

.el-input__wrapper:hover { box-shadow: 0 0 0 1px var(--brand-purple) !important; }

@media (max-width: 768px) {
  .topbar-nav { display: none; }
  .main-content { padding: 16px; }
  .topbar-inner { padding: 0 16px; }
}
</style>
