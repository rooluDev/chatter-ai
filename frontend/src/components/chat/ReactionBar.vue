<template>
  <div v-if="reactions.length > 0 || showAdd" class="reaction-bar">
    <button
      v-for="r in reactions"
      :key="r.emoji"
      class="reaction-btn"
      :class="{ mine: r.userIds != null ? r.userIds.includes(currentUserId) : !!r.isMyReaction }"
      :title="reactionTooltip(r)"
      @click="$emit('toggle', r.emoji)"
    >
      <span>{{ r.emoji }}</span>
      <span class="count">{{ r.count }}</span>
    </button>
    <button v-if="showAdd" class="reaction-add-btn" @click="$emit('add')">+</button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  reactions: { type: Array, default: () => [] },
  showAdd:   { type: Boolean, default: true },
})

defineEmits(['toggle', 'add'])

const authStore   = useAuthStore()
const currentUserId = computed(() => authStore.userId)

function reactionTooltip(r) {
  if (!r.nicknames?.length) return r.emoji
  if (r.nicknames.length <= 3) return r.nicknames.join(', ') + '님이 반응했습니다'
  return `${r.nicknames.slice(0, 3).join(', ')} 외 ${r.nicknames.length - 3}명이 반응했습니다`
}
</script>

<style scoped>
.reaction-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 4px;
}

.reaction-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: 9999px;
  cursor: pointer;
  font-size: 0.875rem;
  transition: background 0.1s, border-color 0.1s;
}

.reaction-btn:hover {
  background: var(--color-bg-tertiary);
}

.reaction-btn.mine {
  background: #e8f0ff;
  border-color: var(--color-primary);
}

.count {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.reaction-add-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: transparent;
  border: 1px dashed var(--color-border);
  border-radius: 9999px;
  cursor: pointer;
  font-size: 1rem;
  color: var(--color-text-muted);
  transition: background 0.1s, border-color 0.1s;
}

.reaction-add-btn:hover {
  background: var(--color-bg-secondary);
  border-color: var(--color-text-muted);
}
</style>
