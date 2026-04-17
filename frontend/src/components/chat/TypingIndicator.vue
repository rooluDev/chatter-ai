<template>
  <Transition name="fade">
    <p v-if="label" class="typing-indicator">{{ label }}</p>
  </Transition>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'

// typingUsers: { [userId]: { nickname, timer } }
const typingUsers = ref({})

const label = computed(() => {
  const users = Object.values(typingUsers.value)
  if (users.length === 0) return ''
  if (users.length === 1) return `${users[0].nickname}님이 입력 중...`
  if (users.length === 2) return `${users[0].nickname}, ${users[1].nickname}님이 입력 중...`
  return '여러 사람이 입력 중...'
})

const TIMEOUT = 3000

function addUser(userId, nickname) {
  // 기존 타이머 초기화
  if (typingUsers.value[userId]?.timer) {
    clearTimeout(typingUsers.value[userId].timer)
  }
  const timer = setTimeout(() => removeUser(userId), TIMEOUT)
  typingUsers.value = {
    ...typingUsers.value,
    [userId]: { nickname, timer },
  }
}

function removeUser(userId) {
  const copy = { ...typingUsers.value }
  if (copy[userId]?.timer) clearTimeout(copy[userId].timer)
  delete copy[userId]
  typingUsers.value = copy
}

function clear() {
  Object.values(typingUsers.value).forEach(u => clearTimeout(u.timer))
  typingUsers.value = {}
}

onUnmounted(clear)

defineExpose({ addUser, removeUser, clear })
</script>

<style scoped>
.typing-indicator {
  font-size: 0.8125rem;
  font-style: italic;
  color: var(--color-text-muted);
  padding: 2px 16px;
  margin: 0;
  min-height: 20px;
}

.fade-enter-active,
.fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from,
.fade-leave-to     { opacity: 0; }
</style>
