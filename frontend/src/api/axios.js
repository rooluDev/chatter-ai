import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true, // Refresh Token Cookie 전송을 위해 필수
})

// ── 요청 인터셉터 ──────────────────────────────────────────
api.interceptors.request.use(config => {
  const authStore = useAuthStore()
  if (authStore.accessToken) {
    config.headers.Authorization = `Bearer ${authStore.accessToken}`
  }
  return config
})

// ── 응답 인터셉터 ──────────────────────────────────────────
let isRefreshing = false
let refreshQueue = []

api.interceptors.response.use(
  res => res.data,  // 성공: { success, message, data } 전체 반환

  async err => {
    const authStore = useAuthStore()
    const uiStore = useUiStore()
    const status = err.response?.status
    const originalRequest = err.config

    // 401 처리: Refresh Token으로 재시도
    if (status === 401 && !originalRequest._retry && !originalRequest._isRefresh) {
      if (isRefreshing) {
        // 갱신 진행 중: 큐에 대기
        return new Promise((resolve, reject) => {
          refreshQueue.push({ resolve, reject })
        }).then(newToken => {
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          return api(originalRequest)
        }).catch(e => Promise.reject(e))
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        // Refresh Token Cookie로 새 Access Token 발급
        const res = await api.post('/auth/refresh', null, { _isRefresh: true })
        const newToken = res.data.accessToken
        authStore.setAccessToken(newToken)

        refreshQueue.forEach(cb => cb.resolve(newToken))
        refreshQueue = []

        originalRequest.headers.Authorization = `Bearer ${newToken}`
        return api(originalRequest)
      } catch (refreshErr) {
        // Refresh 실패 → 로그아웃 + 로그인 페이지로
        refreshQueue.forEach(cb => cb.reject(refreshErr))
        refreshQueue = []
        authStore.clearAuth()
        const ret = encodeURIComponent(window.location.pathname + window.location.search)
        window.location.replace(`/login?ret=${ret}`)
        return Promise.reject(refreshErr)
      } finally {
        isRefreshing = false
      }
    }

    // 에러별 토스트 처리 (409는 컴포넌트에서 직접 처리)
    if (status !== 409) {
      if (status === 403) {
        uiStore.showToast('error', '접근 권한이 없습니다.')
      } else if (status === 404) {
        uiStore.showToast('error', '요청한 리소스를 찾을 수 없습니다.')
      } else if (status === 429) {
        uiStore.showToast('info', '오늘의 AI 사용 횟수를 모두 사용했습니다.')
      } else if (status >= 500) {
        uiStore.showToast('error', '서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.')
      } else if (!err.response) {
        uiStore.showToast('error', '네트워크 연결을 확인해 주세요.')
      }
    }

    return Promise.reject(err)
  }
)

export default api
