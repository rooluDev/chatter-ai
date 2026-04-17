import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', () => {
  const userId = ref(null)
  const nickname = ref(null)
  const role = ref(null)
  const accessToken = ref(null)
  const isInitialized = ref(false)

  const isLoggedIn = computed(() => !!accessToken.value)
  const isAdmin = computed(() => role.value === 'ADMIN')

  /**
   * 로그인 성공 시 전체 정보 저장.
   * @param {{ userId, nickname, role, accessToken }} data
   */
  function setAuth(data) {
    userId.value = data.userId
    nickname.value = data.nickname
    role.value = data.role
    accessToken.value = data.accessToken
  }

  /** 토큰 갱신 시 accessToken만 업데이트. */
  function setAccessToken(token) {
    accessToken.value = token
  }

  /** 로그아웃 시 모든 상태 초기화. */
  function clearAuth() {
    userId.value = null
    nickname.value = null
    role.value = null
    accessToken.value = null
  }

  /**
   * 앱 최초 로드 시 Refresh Token으로 Access Token 복구 시도.
   * 라우터 가드에서 await하여 초기화 완료를 보장한다.
   */
  async function initialize() {
    if (isInitialized.value) return

    try {
      // 순환 의존성 방지: 동적 import
      const { authApi } = await import('@/api/auth')
      const refreshRes = await authApi.refresh()
      accessToken.value = refreshRes.data.accessToken

      const { userApi } = await import('@/api/user')
      const userRes = await userApi.getMe()
      userId.value = userRes.data.id
      nickname.value = userRes.data.nickname
      role.value = userRes.data.role
    } catch {
      clearAuth()
    } finally {
      isInitialized.value = true
    }
  }

  return {
    userId,
    nickname,
    role,
    accessToken,
    isInitialized,
    isLoggedIn,
    isAdmin,
    setAuth,
    setAccessToken,
    clearAuth,
    initialize,
  }
})
