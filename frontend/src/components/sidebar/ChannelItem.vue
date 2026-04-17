<template>
  <li
    class="channel-item"
    :class="{ active: isActive, unread: channel.unreadCount > 0 }"
    @click="$emit('select', channel.id)"
  >
    <span class="channel-hash">#</span>
    <span class="channel-name">{{ channel.name }}</span>
    <span v-if="channel.unreadCount > 0" class="unread-badge">
      {{ channel.unreadCount > 99 ? '99+' : channel.unreadCount }}
    </span>
  </li>
</template>

<script setup>
defineProps({
  channel:  { type: Object,  required: true },
  isActive: { type: Boolean, default: false },
})
defineEmits(['select'])
</script>

<style scoped>
.channel-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 4px;
  cursor: pointer;
  color: var(--color-text-on-sidebar);
  font-size: 0.9375rem;
  transition: background 0.1s;
  user-select: none;
}

.channel-item:hover {
  background: var(--color-sidebar-hover);
}

.channel-item.active {
  background: var(--color-sidebar-active);
  color: #fff;
}

.channel-item.unread .channel-name {
  font-weight: 700;
  color: #fff;
}

.channel-hash {
  font-size: 1rem;
  opacity: 0.7;
  flex-shrink: 0;
}

.channel-name {
  flex: 1;
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

.channel-item.active .unread-badge {
  background: rgba(255, 255, 255, 0.3);
}
</style>
