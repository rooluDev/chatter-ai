<template>
  <div class="message-list-wrapper" ref="wrapperRef" @scroll="handleScroll">
    <!-- 위쪽 로딩 -->
    <div v-if="messageStore.isLoadingMore" class="loading-more">
      <span>이전 메시지 불러오는 중...</span>
    </div>

    <!-- 메시지 목록 -->
    <MessageItem
      v-for="(msg, idx) in decoratedMessages"
      :key="msg._loadingTempId ?? msg._tempId ?? msg.id ?? idx"
      :message="msg"
      :is-first-in-group="msg._isFirstInGroup"
      :show-date-divider="msg._showDateDivider"
      @toggle-reaction="(msgId, emoji) => $emit('toggleReaction', msgId, emoji)"
      @open-emoji-picker="(msgId, evt) => $emit('openEmojiPicker', msgId, evt)"
      @delete="(msgId) => $emit('deleteMessage', msgId)"
      @edit="(msgId, content) => $emit('editMessage', msgId, content)"
      @set-notice="(msgId) => $emit('setNotice', msgId)"
    />

    <!-- 새 메시지 뱃지 -->
    <NewMessageBadge :count="newMessageCount" @click="scrollToBottom(true)" />
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { useMessageStore } from '@/stores/message'
import MessageItem from './MessageItem.vue'
import NewMessageBadge from './NewMessageBadge.vue'

const emit = defineEmits([
  'loadMore',
  'toggleReaction',
  'openEmojiPicker',
  'deleteMessage',
  'editMessage',
  'setNotice',
])

const messageStore = useMessageStore()
const wrapperRef   = ref(null)

const isAtBottom      = ref(true)
const newMessageCount = ref(0)

// ── 메시지 장식 (isFirstInGroup, showDateDivider 계산) ──────────
const decoratedMessages = computed(() => {
  const msgs = messageStore.messages
  return msgs.map((msg, idx) => {
    const prev = idx > 0 ? msgs[idx - 1] : null

    // 날짜 구분선: 이전 메시지와 날짜가 다를 때
    const _showDateDivider = !prev || !isSameDay(
      new Date(prev.createdAt),
      new Date(msg.createdAt),
    )

    // 메시지 묶음: 같은 사용자, 5분 이내
    let _isFirstInGroup = true
    if (prev && !_showDateDivider) {
      const sameUser = (prev.userId != null && prev.userId === msg.userId) ||
        (prev.isAiMessage && msg.isAiMessage)
      const within5min = (new Date(msg.createdAt) - new Date(prev.createdAt)) < 5 * 60 * 1000
      if (sameUser && within5min) _isFirstInGroup = false
    }

    return { ...msg, _isFirstInGroup, _showDateDivider }
  })
})

function isSameDay(a, b) {
  return a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
}

// ── 스크롤 처리 ─────────────────────────────────────────────────
function handleScroll() {
  const el = wrapperRef.value
  if (!el) return

  const threshold = 80
  isAtBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight < threshold

  if (isAtBottom.value) newMessageCount.value = 0

  // 최상단 근처 → 이전 메시지 로드
  if (el.scrollTop < 100 && messageStore.hasNext && !messageStore.isLoadingMore) {
    emit('loadMore')
  }
}

// ── 스크롤 최하단 고정 ──────────────────────────────────────────
async function scrollToBottom(force = false) {
  await nextTick()
  const el = wrapperRef.value
  if (!el) return
  if (force || isAtBottom.value) {
    el.scrollTop = el.scrollHeight
    newMessageCount.value = 0
  }
}

// ── 이전 메시지 로드 후 스크롤 위치 유지 ───────────────────────
async function preserveScrollOnPrepend(prevScrollHeight) {
  await nextTick()
  const el = wrapperRef.value
  if (!el) return
  el.scrollTop = el.scrollHeight - prevScrollHeight
}

// ── 새 메시지 수신 시 처리 ─────────────────────────────────────
watch(() => messageStore.messages.length, (newLen, oldLen) => {
  if (newLen <= oldLen) return
  if (isAtBottom.value) {
    scrollToBottom()
  } else {
    newMessageCount.value += newLen - oldLen
  }
})

onMounted(() => scrollToBottom())

defineExpose({ scrollToBottom, preserveScrollOnPrepend })
</script>

<style scoped>
.message-list-wrapper {
  flex: 1;
  overflow-y: auto;
  position: relative;
  padding-bottom: 8px;
}

.loading-more {
  text-align: center;
  padding: 12px;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}
</style>
