<template>
  <div class="join-page">
    <div class="join-card">
      <h1 class="join-title">회원가입</h1>
      <p class="join-subtitle">ChatterAI에 오신 것을 환영합니다</p>

      <form class="join-form" @submit.prevent="handleJoin">
        <!-- 아이디 -->
        <div class="form-group">
          <label for="username">아이디</label>
          <input
            id="username"
            v-model="form.username"
            type="text"
            placeholder="영문·숫자 4~20자"
            autocomplete="username"
            :disabled="isLoading"
            @blur="touchField('username')"
          />
          <p v-if="errors.username" class="field-error">{{ errors.username }}</p>
        </div>

        <!-- 비밀번호 -->
        <div class="form-group">
          <label for="password">비밀번호</label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            placeholder="영문·숫자·특수문자 포함 8자 이상"
            autocomplete="new-password"
            :disabled="isLoading"
            @blur="touchField('password')"
          />
          <p v-if="errors.password" class="field-error">{{ errors.password }}</p>
        </div>

        <!-- 비밀번호 확인 -->
        <div class="form-group">
          <label for="passwordConfirm">비밀번호 확인</label>
          <input
            id="passwordConfirm"
            v-model="form.passwordConfirm"
            type="password"
            placeholder="비밀번호를 다시 입력하세요"
            autocomplete="new-password"
            :disabled="isLoading"
            @blur="touchField('passwordConfirm')"
          />
          <p v-if="errors.passwordConfirm" class="field-error">{{ errors.passwordConfirm }}</p>
        </div>

        <!-- 닉네임 -->
        <div class="form-group">
          <label for="nickname">닉네임</label>
          <input
            id="nickname"
            v-model="form.nickname"
            type="text"
            placeholder="2~20자"
            :disabled="isLoading"
            @blur="touchField('nickname')"
          />
          <p v-if="errors.nickname" class="field-error">{{ errors.nickname }}</p>
        </div>

        <button type="submit" class="btn-primary btn-full" :disabled="isLoading || !isFormValid">
          {{ isLoading ? '가입 중...' : '회원가입' }}
        </button>
      </form>

      <p class="login-link">
        이미 계정이 있으신가요? <RouterLink to="/login">로그인</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import { useUiStore } from '@/stores/ui'

const router  = useRouter()
const uiStore = useUiStore()

const form = ref({
  username:        '',
  password:        '',
  passwordConfirm: '',
  nickname:        '',
})

const touched  = ref({ username: false, password: false, passwordConfirm: false, nickname: false })
const errors   = ref({ username: '',    password: '',    passwordConfirm: '',    nickname: '' })
const isLoading = ref(false)

const USERNAME_REGEX = /^[a-zA-Z0-9]{4,20}$/
const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]).{8,}$/

function validateField(field) {
  const v = form.value
  switch (field) {
    case 'username':
      if (!v.username) {
        errors.value.username = '아이디를 입력해 주세요.'
      } else if (!USERNAME_REGEX.test(v.username)) {
        errors.value.username = '영문·숫자만 사용하여 4~20자로 입력해 주세요.'
      } else {
        errors.value.username = ''
      }
      break

    case 'password':
      if (!v.password) {
        errors.value.password = '비밀번호를 입력해 주세요.'
      } else if (!PASSWORD_REGEX.test(v.password)) {
        errors.value.password = '영문·숫자·특수문자를 포함하여 8자 이상으로 입력해 주세요.'
      } else {
        errors.value.password = ''
      }
      // 비밀번호 변경 시 확인 필드도 재검증
      if (touched.value.passwordConfirm) validateField('passwordConfirm')
      break

    case 'passwordConfirm':
      if (!v.passwordConfirm) {
        errors.value.passwordConfirm = '비밀번호 확인을 입력해 주세요.'
      } else if (v.password !== v.passwordConfirm) {
        errors.value.passwordConfirm = '비밀번호가 일치하지 않습니다.'
      } else {
        errors.value.passwordConfirm = ''
      }
      break

    case 'nickname':
      if (!v.nickname) {
        errors.value.nickname = '닉네임을 입력해 주세요.'
      } else if (v.nickname.length < 2 || v.nickname.length > 20) {
        errors.value.nickname = '닉네임은 2~20자로 입력해 주세요.'
      } else {
        errors.value.nickname = ''
      }
      break
  }
}

function touchField(field) {
  touched.value[field] = true
  validateField(field)
}

// 실시간 유효성 검사 (touched된 필드만)
watch(() => form.value.username,        () => { if (touched.value.username)        validateField('username') })
watch(() => form.value.password,        () => { if (touched.value.password)        validateField('password') })
watch(() => form.value.passwordConfirm, () => { if (touched.value.passwordConfirm) validateField('passwordConfirm') })
watch(() => form.value.nickname,        () => { if (touched.value.nickname)        validateField('nickname') })

const isFormValid = computed(() => {
  const v = form.value
  return (
    USERNAME_REGEX.test(v.username) &&
    PASSWORD_REGEX.test(v.password) &&
    v.password === v.passwordConfirm &&
    v.nickname.length >= 2 &&
    v.nickname.length <= 20
  )
})

async function handleJoin() {
  // 모든 필드 touch & 검증
  Object.keys(touched.value).forEach((field) => {
    touched.value[field] = true
    validateField(field)
  })

  if (!isFormValid.value) return

  isLoading.value = true
  try {
    await authApi.join({
      username: form.value.username,
      password: form.value.password,
      nickname: form.value.nickname,
    })

    uiStore.showToast('success', '회원가입이 완료되었습니다. 로그인해 주세요.')
    router.replace('/login')
  } catch (err) {
    const status = err?.response?.status
    const code   = err?.response?.data?.message

    if (status === 409) {
      if (code && code.includes('아이디')) {
        errors.value.username = '이미 사용 중인 아이디입니다.'
      } else if (code && code.includes('닉네임')) {
        errors.value.nickname = '이미 사용 중인 닉네임입니다.'
      } else {
        // errorCode 기반 fallback
        const errorCode = err?.response?.data?.errorCode
        if (errorCode === 'DUPLICATE_USERNAME') {
          errors.value.username = '이미 사용 중인 아이디입니다.'
        } else if (errorCode === 'DUPLICATE_NICKNAME') {
          errors.value.nickname = '이미 사용 중인 닉네임입니다.'
        } else {
          uiStore.showToast('error', '이미 사용 중인 정보입니다.')
        }
      }
    } else {
      uiStore.showToast('error', '회원가입 중 오류가 발생했습니다. 다시 시도해 주세요.')
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.join-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
  padding: 1rem;
}

.join-card {
  width: 100%;
  max-width: 400px;
  background: var(--color-surface);
  border-radius: 12px;
  padding: 2.5rem 2rem;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}

.join-title {
  text-align: center;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 0.25rem;
}

.join-subtitle {
  text-align: center;
  color: var(--color-text-muted);
  font-size: 0.875rem;
  margin: 0 0 2rem;
}

.join-form {
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

.field-error {
  color: var(--color-error);
  font-size: 0.8125rem;
  margin: 0;
}

.btn-full {
  width: 100%;
  margin-top: 0.25rem;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.login-link {
  text-align: center;
  margin: 1.5rem 0 0;
  font-size: 0.875rem;
  color: var(--color-text-muted);
}

.login-link a {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 500;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>
