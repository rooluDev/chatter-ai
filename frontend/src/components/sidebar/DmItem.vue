<template>
  <li
    class="dm-item"
    :class="{ active: isActive }"
    @click="$emit('select', room.id)"
  >
    <div class="avatar-wrapper">
      <UserAvatar :nickname="room.opponent?.nickname || '?'" size="sm" />
      <PresenceDot
        :status="room.opponent?.isOnline ? 'ONLINE' : 'OFFLINE'"
        class="presence-overlay"
      />
    </div>
    <div class="dm-info">
      <span class="dm-nickname">{{ room.opponent?.nickname }}</span>
      <span v-if="room.lastMessage" class="dm-last">{{ truncate(room.lastMessage.content, 20) }}</span>
    </div>
    <span v-if="room.unreadCount > 0" class="unread-badge">
      {{ room.unreadCount > 99 ? '99+' : room.unreadCount }}
    </span>
  </li>
</template>

<script setup>
import UserAvatar from '@/components/common/UserAvatar.vue'
import PresenceDot from '@/components/common/PresenceDot.vue'

defineProps({
  room:     { type: Object,  required: true },
  isActive: { type: Boolean, default: false },
})
defineEmits(['select'])

function truncate(text, len) {
  if (!text) return ''
  return text.length > len ? text.slice(0, len) + '...' : text
}
</script>

<style scoped>
.dm-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 12px;
  border-radius: 4px;
  cursor: pointer;
  color: var(--color-text-on-sidebar);
  transition: background 0.1s;
  user-select: none;
}

.dm-item:hover {
  background: var(--color-sidebar-hover);
}

.dm-item.active {
  background: var(--color-sidebar-active);
  color: #fff;
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

.dm-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.dm-nickname {
  font-size: 0.9375rem;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dm-item.active .dm-nickname {
  color: #fff;
}

.dm-last {
  font-size: 0.75rem;
  color: var(--color-text-on-sidebar);
  opacity: 0.6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-badge {
  background: var(--color-primary);
  color: #fff;
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 9999px;
  min-width: 18px;
  text-align: center;
  flex-shrink: 0;
}

.dm-item.active .unread-badge {
  background: rgba(255, 255, 255, 0.3);
}
</style>
