<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="login-title">ChatterAI</h1>
      <p class="login-subtitle">로그인하여 대화를 시작하세요</p>

      <form class="login-form" @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">아이디</label>
          <input
            id="username"
            v-model="form.username"
            type="text"
            placeholder="아이디를 입력하세요"
            autocomplete="username"
            :disabled="isLoading"
          />
        </div>

        <div class="form-group">
          <label for="password">비밀번호</label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            placeholder="비밀번호를 입력하세요"
            autocomplete="current-password"
            :disabled="isLoading"
          />
        </div>

        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

        <button type="submit" class="btn-primary btn-full" :disabled="isLoading">
          {{ isLoading ? '로그인 중...' : '로그인' }}
        </button>
      </form>

      <p class="join-link">
        계정이 없으신가요? <RouterLink to="/join">회원가입</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authApi } from '@/api/auth'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const router    = useRouter()
const route     = useRoute()
const authStore = useAuthStore()

const form         = ref({ username: '', password: '' })
const isLoading    = ref(false)
const errorMessage = ref('')

async function handleLogin() {
  errorMessage.value = ''

  if (!form.value.username || !form.value.password) {
    errorMessage.value = '아이디와 비밀번호를 입력해 주세요.'
    return
  }

  isLoading.value = true
  try {
    const loginRes = await authApi.login({
      username: form.value.username,
      password: form.value.password,
    })

    authStore.setAccessToken(loginRes.data.accessToken)

    const userRes = await userApi.getMe()
    authStore.setAuth({
      userId:      userRes.data.id,
      nickname:    userRes.data.nickname,
      role:        userRes.data.role,
      accessToken: loginRes.data.accessToken,
    })

    const ret = route.query.ret
    router.replace(ret && ret.startsWith('/') ? ret : '/channels')
  } catch (err) {
    const status = err?.response?.status
    if (status === 401) {
      errorMessage.value = '아이디 또는 비밀번호가 올바르지 않습니다.'
    } else if (status === 403) {
      errorMessage.value = '비활성화된 계정입니다. 관리자에게 문의해 주세요.'
    } else {
      errorMessage.value = '로그인 중 오류가 발생했습니다. 다시 시도해 주세요.'
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
  padding: 1rem;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: var(--color-surface);
  border-radius: 12px;
  padding: 2.5rem 2rem;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}

.login-title {
  text-align: center;
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--color-primary);
  margin: 0 0 0.25rem;
}

.login-subtitle {
  text-align: center;
  color: var(--color-text-muted);
  font-size: 0.9rem;
  margin: 0 0 2rem;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.form-group label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.form-group input {
  padding: 0.625rem 0.875rem;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  color: var(--color-text);
  font-size: 0.9375rem;
  transition: border-color 0.15s;
}

.form-group input:focus {
  outline: none;
  border-color: var(--color-primary);
}

.form-group input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-message {
  color: var(--color-error);
  font-size: 0.875rem;
  margin: 0;
  padding: 0.5rem 0.75rem;
  background: rgba(237, 66, 69, 0.1);
  border-radius: 6px;
}

.btn-full {
  width: 100%;
  margin-top: 0.5rem;
}

.join-link {
  text-align: center;
  margin: 1.5rem 0 0;
  font-size: 0.875rem;
  color: var(--color-text-muted);
}

.join-link a {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 500;
}

.join-link a:hover {
  text-decoration: underline;
}
</style>
