<template>
  <header class="gnb">
    <div class="gnb-left">
      <span class="gnb-logo">ChatterAI</span>
    </div>

    <div class="gnb-right">
      <div class="profile-wrapper" ref="profileRef">
        <button class="profile-btn" @click="toggleDropdown">
          <UserAvatar :nickname="authStore.nickname || '?'" size="sm" />
          <span class="profile-nickname">{{ authStore.nickname }}</span>
          <svg class="chevron" :class="{ open: isDropdownOpen }" width="12" height="12" viewBox="0 0 12 12" fill="none">
            <path d="M2 4L6 8L10 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>

        <Transition name="dropdown">
          <div v-if="isDropdownOpen" class="dropdown-menu">
            <div class="dropdown-info">
              <p class="dropdown-nickname">{{ authStore.nickname }}</p>
              <p class="dropdown-role">{{ roleLabel }}</p>
            </div>
            <div class="dropdown-divider" />
            <button class="dropdown-item danger" @click="handleLogout">
              로그아웃
            </button>
          </div>
        </Transition>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import stompClient from '@/socket/stompClient'
import UserAvatar from '@/components/common/UserAvatar.vue'

const router    = useRouter()
const authStore = useAuthStore()
const uiStore   = useUiStore()

const isDropdownOpen = ref(false)
const profileRef     = ref(null)

const roleLabel = computed(() => {
  return authStore.isAdmin ? '관리자' : '일반 회원'
})

function toggleDropdown() {
  isDropdownOpen.value = !isDropdownOpen.value
}

function closeDropdown(e) {
  if (profileRef.value && !profileRef.value.contains(e.target)) {
    isDropdownOpen.value = false
  }
}

onMounted(() => document.addEventListener('click', closeDropdown))
onUnmounted(() => document.removeEventListener('click', closeDropdown))

async function handleLogout() {
  isDropdownOpen.value = false
  try {
    await authApi.logout()
  } catch {
    // 서버 오류여도 클라이언트 로그아웃 진행
  } finally {
    stompClient.disconnect()
    authStore.clearAuth()
    router.replace('/login')
  }
}
</script>

<style scoped>
.gnb {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 64px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1.5rem;
  z-index: 100;
}

.gnb-logo {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: -0.01em;
}

.gnb-right {
  display: flex;
  align-items: center;
}

.profile-wrapper {
  position: relative;
}

.profile-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.375rem 0.625rem;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  color: var(--color-text);
  transition: background 0.15s;
}

.profile-btn:hover {
  background: var(--color-hover);
}

.profile-nickname {
  font-size: 0.9375rem;
  font-weight: 500;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chevron {
  color: var(--color-text-muted);
  transition: transform 0.2s;
  flex-shrink: 0;
}

.chevron.open {
  transform: rotate(180deg);
}

/* 드롭다운 */
.dropdown-menu {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  min-width: 180px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  z-index: 200;
}

.dropdown-info {
  padding: 0.75rem 1rem;
}

.dropdown-nickname {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 0.125rem;
}

.dropdown-role {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin: 0;
}

.dropdown-divider {
  height: 1px;
  background: var(--color-border);
  margin: 0;
}

.dropdown-item {
  display: block;
  width: 100%;
  padding: 0.625rem 1rem;
  background: transparent;
  border: none;
  text-align: left;
  font-size: 0.9375rem;
  color: var(--color-text);
  cursor: pointer;
  transition: background 0.15s;
}

.dropdown-item:hover {
  background: var(--color-hover);
}

.dropdown-item.danger {
  color: var(--color-error);
}

.dropdown-item.danger:hover {
  background: rgba(237, 66, 69, 0.1);
}

/* 드롭다운 트랜지션 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.15s, transform 0.15s;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
