<template>
  <header class="channel-header">
    <div class="header-left">
      <span class="channel-hash">#</span>
      <span class="channel-name">{{ channel?.name }}</span>
      <span v-if="channel?.participantCount" class="participant-count">
        {{ channel.participantCount }}명
      </span>
    </div>
    <div class="header-right">
      <button
        v-if="noticeMessage"
        class="notice-btn"
        @click="$emit('scrollToNotice', noticeMessage.id)"
        :title="noticeMessage.content"
      >
        <span class="notice-pin">📌</span>
        <span class="notice-content">{{ truncate(noticeMessage.content, 50) }}</span>
      </button>
      <!-- 검색 (MVP 외) -->
      <button class="icon-btn" title="검색">🔍</button>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useChannelStore } from '@/stores/channel'

const props = defineProps({
  channel: { type: Object, default: null },
})

defineEmits(['scrollToNotice'])

const channelStore = useChannelStore()

const noticeMessage = computed(() => props.channel?.noticeMessage ?? null)

function truncate(text, len) {
  if (!text) return ''
  return text.length > len ? text.slice(0, len) + '...' : text
}
</script>

<style scoped>
.channel-header {
  height: 56px;
  min-height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid var(--color-border);
  background: #fff;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.channel-hash {
  font-size: 1.125rem;
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.channel-name {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.participant-count {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.notice-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
  max-width: 280px;
  transition: background 0.15s;
}

.notice-btn:hover {
  background: var(--color-bg-tertiary);
}

.notice-pin {
  flex-shrink: 0;
}

.notice-content {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.icon-btn {
  padding: 4px 8px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 1rem;
  color: var(--color-text-muted);
  border-radius: 4px;
  transition: background 0.15s;
}

.icon-btn:hover {
  background: var(--color-bg-secondary);
}
</style>
