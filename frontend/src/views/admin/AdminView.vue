<template>
  <div class="admin-layout">
    <AppGnb />

    <div class="admin-body">
      <!-- 관리자 사이드 메뉴 -->
      <nav class="admin-nav">
        <p class="admin-nav-title">관리자</p>
        <ul class="admin-nav-list">
          <li>
            <RouterLink to="/admin/dashboard" class="admin-nav-item" active-class="active">
              대시보드
            </RouterLink>
          </li>
          <li>
            <RouterLink to="/admin/channels" class="admin-nav-item" active-class="active">
              채널 관리
            </RouterLink>
          </li>
          <li>
            <RouterLink to="/admin/users" class="admin-nav-item" active-class="active">
              회원 관리
            </RouterLink>
          </li>
        </ul>
      </nav>

      <!-- 관리자 콘텐츠 -->
      <main class="admin-main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup>
import AppGnb from '@/components/layout/AppGnb.vue'
</script>

<style scoped>
.admin-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: var(--color-bg-secondary);
}

.admin-body {
  display: flex;
  flex: 1;
  overflow: hidden;
  margin-top: 64px;
}

/* 관리자 사이드 메뉴 */
.admin-nav {
  width: 180px;
  min-width: 180px;
  background: #fff;
  border-right: 1px solid var(--color-border);
  padding: 20px 12px;
  overflow-y: auto;
}

.admin-nav-title {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0 8px;
  margin: 0 0 8px;
}

.admin-nav-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.admin-nav-item {
  display: block;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 0.9375rem;
  color: var(--color-text-secondary);
  text-decoration: none;
  transition: background 0.1s, color 0.1s;
}

.admin-nav-item:hover {
  background: var(--color-bg-secondary);
  color: var(--color-text);
}

.admin-nav-item.active {
  background: var(--color-primary-light);
  color: var(--color-primary);
  font-weight: 600;
}

/* 관리자 콘텐츠 */
.admin-main {
  flex: 1;
  overflow-y: auto;
  padding: 28px 32px;
}
</style>
