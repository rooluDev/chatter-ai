<template>
  <div class="channel-layout">
    <!-- GNB -->
    <AppGnb />

    <div class="channel-body">
      <!-- 사이드바 -->
      <AppSidebar @open-admin="router.push('/admin')" />

      <!-- 메인 영역 -->
      <main class="channel-main">
        <template v-if="channelStore.currentChannel">
          <!-- 채널 헤더 -->
          <ChannelHeader
            :channel="channelStore.currentChannel"
            @scroll-to-notice="handleScrollToNotice"
          />

          <!-- 메시지 목록 -->
          <MessageList
            ref="messageListRef"
            @load-more="handleLoadMore"
            @toggle-reaction="handleToggleReaction"
            @open-emoji-picker="handleOpenEmojiPicker"
            @delete-message="handleDeleteMessage"
            @edit-message="handleEditMessage"
            @set-notice="handleSetNotice"
          />

          <!-- 입력 영역 -->
          <MessageInput
            ref="messageInputRef"
            :channel-name="channelStore.currentChannel?.name"
            :placeholder="'메시지를 입력하세요...'"
            @send="handleSend"
            @typing="handleTyping"
          />
        </template>

        <div v-else class="channel-empty">
          <p>채널을 선택해 주세요.</p>
        </div>
      </main>
    </div>

    <!-- 이모지 피커 (메시지 호버용) -->
    <Teleport to="body">
      <div
        v-if="emojiPickerState.open"
        class="floating-emoji"
        :style="{ top: emojiPickerState.y + 'px', left: emojiPickerState.x + 'px' }"
      >
        <EmojiPicker
          @select="(e) => handleReactionEmoji(emojiPickerState.messageId, e)"
          @close="emojiPickerState.open = false"
        />
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { channelApi } from '@/api/channel'
import { messageApi } from '@/api/message'
import { reactionApi } from '@/api/reaction'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useChannelStore } from '@/stores/channel'
import { useMessageStore } from '@/stores/message'
import { useDmStore } from '@/stores/dm'
import { useUiStore } from '@/stores/ui'
import stompClient from '@/socket/stompClient'
import AppGnb from '@/components/layout/AppGnb.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import ChannelHeader from '@/components/layout/ChannelHeader.vue'
import MessageList from '@/components/chat/MessageList.vue'
import MessageInput from '@/components/chat/MessageInput.vue'
import EmojiPicker from '@/components/chat/EmojiPicker.vue'

const route        = useRoute()
const router       = useRouter()
const authStore    = useAuthStore()
const channelStore = useChannelStore()
const messageStore = useMessageStore()
const dmStore      = useDmStore()
const uiStore      = useUiStore()

const messageListRef  = ref(null)
const messageInputRef = ref(null)

const emojiPickerState = ref({ open: false, messageId: null, x: 0, y: 0 })

// ── WebSocket 연결 ──────────────────────────────────────────
onMounted(async () => {
  // 연결
  if (!stompClient.isConnected()) {
    stompClient.connect(authStore.accessToken)
  }

  // 알림 구독 (채널 비의존)
  stompClient.subscribeNotification(handleNotificationEvent)

  // 채널 초기 로드
  if (channelStore.channels.length === 0) {
    await loadChannels()
  }

  // 라우트 파라미터 처리
  const channelId = route.params.channelId
    ? Number(route.params.channelId)
    : channelStore.channels[0]?.id

  if (channelId) {
    await enterChannel(channelId)
  }
})

onUnmounted(() => {
  if (channelStore.currentChannelId) {
    stompClient.unsubscribeChannel(channelStore.currentChannelId)
  }
  stompClient.unsubscribeNotification()
})

// ── 라우트 변경 감지 ────────────────────────────────────────
watch(() => route.params.channelId, async (newId) => {
  if (newId) await enterChannel(Number(newId))
})

// ── 채널 목록 로드 ──────────────────────────────────────────
async function loadChannels() {
  try {
    const res = await channelApi.getChannels()
    channelStore.setChannels(res.data ?? [])
  } catch { /* 인터셉터에서 처리 */ }
}

// ── 채널 입장 ───────────────────────────────────────────────
async function enterChannel(channelId) {
  const prevChannelId = channelStore.currentChannelId

  // 이전 채널 구독 해제
  if (prevChannelId && prevChannelId !== channelId) {
    stompClient.unsubscribeChannel(prevChannelId)
  }

  channelStore.setCurrentChannel(channelId)
  messageStore.clearMessages()

  // 메시지 로드
  await loadMessages(channelId)

  // 안읽음 초기화
  channelStore.resetUnread(channelId)
  try { await channelApi.markAsRead(channelId) } catch { /* ignore */ }

  // 새 채널 구독
  stompClient.subscribeChannel(channelId, handleChannelEvent)

  // 스크롤 최하단
  nextTick(() => messageListRef.value?.scrollToBottom(true))
}

// ── 메시지 로드 ─────────────────────────────────────────────
async function loadMessages(channelId, beforeId = null) {
  if (beforeId) {
    messageStore.isLoadingMore = true
  } else {
    messageStore.isLoading = true
  }

  try {
    const params = { size: 20, ...(beforeId ? { before: beforeId } : {}) }
    const res = await channelApi.getChannelMessages(channelId, params)
    const { content, hasNext, nextCursorId } = res.data

    if (beforeId) {
      const prevScrollHeight = messageListRef.value?.$el?.scrollHeight ?? 0
      messageStore.prependMessages(content, { hasNext, nextCursorId })
      messageListRef.value?.preserveScrollOnPrepend(prevScrollHeight)
    } else {
      messageStore.setMessages(content, { hasNext, nextCursorId })
    }
  } catch { /* 인터셉터에서 처리 */ } finally {
    messageStore.isLoading = false
    messageStore.isLoadingMore = false
  }
}

// ── 무한 스크롤 ─────────────────────────────────────────────
async function handleLoadMore() {
  if (!messageStore.hasNext || messageStore.isLoadingMore) return
  await loadMessages(channelStore.currentChannelId, messageStore.nextCursorId)
}

// ── 메시지 전송 ─────────────────────────────────────────────
async function handleSend(payload) {
  const channelId = channelStore.currentChannelId
  if (!channelId) return

  // AI 사용량 1차 검증
  if (payload.content?.includes('@AI')) {
    try {
      const usageRes = await userApi.getAiUsage()
      if (usageRes.data.remainCount <= 0) {
        uiStore.showToast('info', 'AI 일일 사용량을 초과했습니다.')
        return
      }
    } catch { /* 무시 — 서버에서 2차 검증 */ }
  }

  // Optimistic UI
  const tempId = `temp-${Date.now()}`
  messageStore.addOptimisticMessage({
    _tempId: tempId,
    userId:  authStore.userId,
    nickname: authStore.nickname,
    content: payload.content,
    type:    payload.type,
    attachments: payload.attachments,
    reactions: [],
    isAiMessage: false,
    isDeleted: false,
    createdAt: new Date().toISOString(),
  })

  try {
    stompClient.sendChannelMessage(channelId, { ...payload, tempId })
  } catch {
    messageStore.removeOptimisticMessage(tempId)
    uiStore.showToast('error', '메시지 전송에 실패했습니다.')
  }
}

// ── 타이핑 전송 ─────────────────────────────────────────────
function handleTyping() {
  const channelId = channelStore.currentChannelId
  if (channelId) stompClient.sendTyping(channelId)
}

// ── WebSocket 채널 이벤트 처리 ──────────────────────────────
function handleChannelEvent(event) {
  switch (event.eventType) {
    case 'CHAT':
      messageStore.addMessage(event.message, event.tempId)
      break

    case 'AI_LOADING':
      messageStore.addLoadingMessage(event)
      break

    case 'AI_RESPONSE':
    case 'AI_ERROR':
      messageStore.replaceLoadingMessage(event)
      break

    case 'MESSAGE_UPDATED':
      messageStore.updateMessage(event.message)
      break

    case 'MESSAGE_DELETED':
      messageStore.softDeleteMessage(event.messageId)
      break

    case 'REACTION_UPDATED':
      messageStore.updateReactions(event)
      break

    case 'NOTICE_UPDATED':
      channelStore.updateNotice(event)
      break

    case 'CHANNEL_DELETED':
      handleChannelDeleted(event.channelId)
      break

    case 'TYPING':
      messageInputRef.value?.addTypingUser(event.userId, event.nickname)
      break
  }
}

// ── WebSocket 알림 이벤트 ───────────────────────────────────
function handleNotificationEvent(event) {
  if (event.eventType === 'UNREAD_COUNT') {
    const { channelId, dmRoomId, unreadCount } = event
    if (channelId) {
      if (channelId !== channelStore.currentChannelId) {
        const ch = channelStore.channels.find(c => c.id === channelId)
        if (ch) ch.unreadCount = unreadCount
      }
    } else if (dmRoomId) {
      if (dmRoomId !== dmStore.currentDmRoomId) {
        dmStore.updateUnread(dmRoomId, unreadCount)
      }
    }
  }
}

// ── 채널 삭제 처리 ──────────────────────────────────────────
function handleChannelDeleted(channelId) {
  channelStore.removeChannel(channelId)
  if (channelStore.currentChannelId === channelId) {
    const next = channelStore.channels[0]
    if (next) {
      router.replace(`/channels/${next.id}`)
    } else {
      channelStore.setCurrentChannel(null)
      messageStore.clearMessages()
    }
  }
}

// ── 메시지 삭제 ─────────────────────────────────────────────
function handleDeleteMessage(messageId) {
  uiStore.openModal({
    title: '메시지 삭제',
    message: '이 메시지를 삭제할까요?',
    onConfirm: async () => {
      try {
        await messageApi.delete(messageId)
      } catch { /* 인터셉터에서 처리 */ }
    },
  })
}

// ── 메시지 수정 ─────────────────────────────────────────────
async function handleEditMessage(messageId, content) {
  try {
    await messageApi.update(messageId, { content })
  } catch { /* 인터셉터에서 처리 */ }
}

// ── 이모지 반응 ─────────────────────────────────────────────
async function handleToggleReaction(messageId, emoji) {
  const msg = messageStore.messages.find(m => m.id === messageId)
  if (!msg) return

  const existing = msg.reactions?.find(r => r.emoji === emoji)
  // userIds: WebSocket 브로드캐스트, isMyReaction: REST 초기 응답
  const alreadyReacted = existing?.userIds != null
    ? existing.userIds.includes(authStore.userId)
    : !!existing?.isMyReaction

  try {
    if (alreadyReacted) {
      await reactionApi.remove(messageId, { emoji })
    } else {
      await reactionApi.add(messageId, { emoji })
    }
  } catch { /* 인터셉터에서 처리 */ }
}

function handleOpenEmojiPicker(messageId, evt) {
  const rect = evt.target.getBoundingClientRect()
  emojiPickerState.value = {
    open: true,
    messageId,
    x: Math.max(8, rect.left - 310),
    y: Math.max(8, rect.top - 300),
  }
}

async function handleReactionEmoji(messageId, emoji) {
  emojiPickerState.value.open = false
  await handleToggleReaction(messageId, emoji)
}

// ── 공지 지정 ───────────────────────────────────────────────
async function handleSetNotice(messageId) {
  // 관리자 API — Phase 16에서 AdminView와 통합
  // 여기서는 직접 admin API 호출
  const { adminApi } = await import('@/api/admin')
  try {
    const channelId = channelStore.currentChannelId
    await adminApi.setNotice(channelId, { messageId })
  } catch { /* 인터셉터에서 처리 */ }
}

// ── 공지 스크롤 ─────────────────────────────────────────────
function handleScrollToNotice(noticeMessageId) {
  const el = document.querySelector(`[data-message-id="${noticeMessageId}"]`)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
}
</script>

<style scoped>
.channel-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.channel-body {
  display: flex;
  flex: 1;
  overflow: hidden;
  margin-top: 64px; /* GNB 높이 */
}

.channel-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
}

.channel-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
  font-size: 1rem;
}

.floating-emoji {
  position: fixed;
  z-index: 500;
}
</style>
