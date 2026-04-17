<template>
  <div class="user-profile" ref="profileRef">
    <button class="profile-btn" @click="toggleDropdown">
      <div class="avatar-wrapper">
        <UserAvatar :nickname="authStore.nickname || '?'" size="sm" />
        <PresenceDot status="ONLINE" class="presence-overlay" />
      </div>
      <span class="profile-nickname">{{ authStore.nickname }}</span>
      <span class="settings-icon">⚙</span>
    </button>

    <Transition name="dropdown-up">
      <div v-if="isOpen" class="dropdown-menu">
        <div class="dropdown-user">
          <p class="dropdown-nickname">{{ authStore.nickname }}</p>
          <p class="dropdown-role">{{ authStore.isAdmin ? '관리자' : '일반 회원' }}</p>
        </div>
        <div class="dropdown-divider" />
        <button class="dropdown-item danger" @click="handleLogout">로그아웃</button>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from '@/components/common/UserAvatar.vue'
import PresenceDot from '@/components/common/PresenceDot.vue'
import stompClient from '@/socket/stompClient'

const router    = useRouter()
const authStore = useAuthStore()

const isOpen     = ref(false)
const profileRef = ref(null)

function toggleDropdown() { isOpen.value = !isOpen.value }

function handleClickOutside(e) {
  if (profileRef.value && !profileRef.value.contains(e.target)) {
    isOpen.value = false
  }
}

onMounted(() => document.addEventListener('click', handleClickOutside))
onUnmounted(() => document.removeEventListener('click', handleClickOutside))

async function handleLogout() {
  isOpen.value = false
  try { await authApi.logout() } catch { /* ignore */ }
  stompClient.disconnect()
  authStore.clearAuth()
  router.replace('/login')
}
</script>

<style scoped>
.user-profile {
  position: relative;
  padding: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.profile-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 6px 8px;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  color: var(--color-text-on-sidebar);
  transition: background 0.1s;
}

.profile-btn:hover {
  background: var(--color-sidebar-hover);
}

.avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.presence-overlay {
  position: absolute;
  bottom: -1px;
  right: -1px;
}

.profile-nickname {
  flex: 1;
  font-size: 0.8125rem;
  font-weight: 600;
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 110px;
}

.settings-icon {
  font-size: 0.875rem;
  opacity: 0;
  transition: opacity 0.15s;
  flex-shrink: 0;
}

.profile-btn:hover .settings-icon {
  opacity: 0.7;
}

/* 드롭다운 (위쪽으로 열림) */
.dropdown-menu {
  position: absolute;
  bottom: calc(100% + 4px);
  left: 8px;
  right: 8px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  z-index: 200;
}

.dropdown-user {
  padding: 10px 12px;
}

.dropdown-nickname {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 2px;
}

.dropdown-role {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin: 0;
}

.dropdown-divider {
  height: 1px;
  background: var(--color-border);
}

.dropdown-item {
  display: block;
  width: 100%;
  padding: 8px 12px;
  text-align: left;
  font-size: 0.9375rem;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: background 0.1s;
}

.dropdown-item.danger {
  color: var(--color-error);
}

.dropdown-item.danger:hover {
  background: rgba(239, 68, 68, 0.08);
}

/* 트랜지션 */
.dropdown-up-enter-active,
.dropdown-up-leave-active {
  transition: opacity 0.15s, transform 0.15s;
}

.dropdown-up-enter-from,
.dropdown-up-leave-to {
  opacity: 0;
  transform: translateY(6px);
}
</style>
