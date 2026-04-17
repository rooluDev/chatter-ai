import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 루트 → 채널 리다이렉트
    { path: '/', redirect: '/channels' },

    // 비로그인 전용
    {
      path: '/login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/join',
      component: () => import('@/views/JoinView.vue'),
      meta: { guestOnly: true },
    },

    // 인증 필요
    {
      path: '/channels',
      component: () => import('@/views/ChannelView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/channels/:channelId',
      component: () => import('@/views/ChannelView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/dm/:dmRoomId',
      component: () => import('@/views/DmView.vue'),
      meta: { requiresAuth: true },
    },

    // 관리자 전용 (중첩 라우트)
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        { path: '',         redirect: 'dashboard' },
        { path: 'dashboard', component: () => import('@/views/admin/AdminDashboardView.vue') },
        { path: 'channels',  component: () => import('@/views/admin/AdminChannelsView.vue') },
        { path: 'users',     component: () => import('@/views/admin/AdminUsersView.vue') },
      ],
    },

    // 404 fallback
    { path: '/:pathMatch(.*)*', redirect: '/channels' },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  // 최초 로드 시 Refresh Token으로 인증 상태 복구
  if (!authStore.isInitialized) {
    await authStore.initialize()
  }

  // 인증 필요 페이지: 비로그인이면 /login?ret=현재경로
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return `/login?ret=${encodeURIComponent(to.fullPath)}`
  }

  // 관리자 전용 페이지: ADMIN 아니면 채널로
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    return '/channels'
  }

  // 비로그인 전용 페이지: 이미 로그인이면 채널로
  if (to.meta.guestOnly && authStore.isLoggedIn) {
    return '/channels'
  }
})

export default router
