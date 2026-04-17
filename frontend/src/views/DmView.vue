<template>
  <div class="channel-layout">
    <!-- GNB -->
    <AppGnb />

    <div class="channel-body">
      <!-- 사이드바 -->
      <AppSidebar @open-admin="router.push('/admin')" />

      <!-- DM 메인 -->
      <main class="channel-main">
        <template v-if="dmStore.currentDmRoom">
          <!-- DM 헤더 -->
          <header class="dm-header">
            <div class="dm-header-info">
              <div class="dm-avatar-wrapper">
                <UserAvatar :nickname="dmStore.currentDmRoom.opponent?.nickname || '?'" size="sm" />
                <PresenceDot
                  :status="opponentStatus"
                  class="presence-overlay"
                />
              </div>
              <span class="dm-header-name">{{ dmStore.currentDmRoom.opponent?.nickname }}</span>
              <span class="dm-header-status">{{ opponentStatusLabel }}</span>
            </div>
          </header>

          <!-- 메시지 목록 -->
          <MessageList
            ref="messageListRef"
            @load-more="handleLoadMore"
            @toggle-reaction="handleToggleReaction"
            @open-emoji-picker="handleOpenEmojiPicker"
            @delete-message="handleDeleteMessage"
            @edit-message="handleEditMessage"
          />

          <!-- 입력 영역 -->
          <MessageInput
            ref="messageInputRef"
            :placeholder="`@${dmStore.currentDmRoom.opponent?.nickname}에게 메시지 보내기`"
            @send="handleSend"
            @typing="handleTyping"
          />
        </template>

        <div v-else class="channel-empty">
          <p>DM 방을 선택해 주세요.</p>
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
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { dmApi } from '@/api/dm'
import { messageApi } from '@/api/message'
import { reactionApi } from '@/api/reaction'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useDmStore } from '@/stores/dm'
import { useMessageStore } from '@/stores/message'
import { useChannelStore } from '@/stores/channel'
import { useUiStore } from '@/stores/ui'
import stompClient from '@/socket/stompClient'
import AppGnb from '@/components/layout/AppGnb.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import MessageList from '@/components/chat/MessageList.vue'
import MessageInput from '@/components/chat/MessageInput.vue'
import EmojiPicker from '@/components/chat/EmojiPicker.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import PresenceDot from '@/components/common/PresenceDot.vue'

const route        = useRoute()
const router       = useRouter()
const authStore    = useAuthStore()
const dmStore      = useDmStore()
const channelStore = useChannelStore()
const messageStore = useMessageStore()
const uiStore      = useUiStore()

const messageListRef  = ref(null)
const messageInputRef = ref(null)
const emojiPickerState = ref({ open: false, messageId: null, x: 0, y: 0 })

const opponentStatus = computed(() => {
  return dmStore.currentDmRoom?.opponent?.isOnline ? 'ONLINE' : 'OFFLINE'
})

const opponentStatusLabel = computed(() => {
  return opponentStatus.value === 'ONLINE' ? '온라인' : '오프라인'
})

// ── WebSocket 연결 ──────────────────────────────────────────
onMounted(async () => {
  if (!stompClient.isConnected()) {
    stompClient.connect(authStore.accessToken)
  }

  // 알림 구독 (DM 비의존)
  stompClient.subscribeNotification(handleNotificationEvent)

  // DM 목록 로드
  if (dmStore.dmRooms.length === 0) {
    try {
      const res = await dmApi.getDmRooms()
      dmStore.setDmRooms(res.data ?? [])
    } catch { /* ignore */ }
  }

  const dmRoomId = Number(route.params.dmRoomId)
  if (dmRoomId) await enterDmRoom(dmRoomId)
})

onUnmounted(() => {
  if (dmStore.currentDmRoomId) {
    stompClient.unsubscribeDm(dmStore.currentDmRoomId)
  }
  stompClient.unsubscribeNotification()
})

// ── 라우트 변경 감지 ────────────────────────────────────────
watch(() => route.params.dmRoomId, async (newId) => {
  if (newId) await enterDmRoom(Number(newId))
})

// ── DM 방 입장 ──────────────────────────────────────────────
async function enterDmRoom(dmRoomId) {
  const prevId = dmStore.currentDmRoomId

  if (prevId && prevId !== dmRoomId) {
    stompClient.unsubscribeDm(prevId)
  }

  dmStore.setCurrentDmRoom(dmRoomId)
  messageStore.clearMessages()

  await loadMessages(dmRoomId)

  dmStore.resetUnread(dmRoomId)
  try { await dmApi.markDmAsRead(dmRoomId) } catch { /* ignore */ }

  stompClient.subscribeDm(dmRoomId, handleDmEvent)

  nextTick(() => messageListRef.value?.scrollToBottom(true))
}

// ── 메시지 로드 ─────────────────────────────────────────────
async function loadMessages(dmRoomId, beforeId = null) {
  if (beforeId) messageStore.isLoadingMore = true
  else          messageStore.isLoading = true

  try {
    const params = { size: 20, ...(beforeId ? { before: beforeId } : {}) }
    const res = await dmApi.getDmMessages(dmRoomId, params)
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

async function handleLoadMore() {
  if (!messageStore.hasNext || messageStore.isLoadingMore) return
  await loadMessages(dmStore.currentDmRoomId, messageStore.nextCursorId)
}

// ── 메시지 전송 ─────────────────────────────────────────────
async function handleSend(payload) {
  const dmRoomId = dmStore.currentDmRoomId
  if (!dmRoomId) return

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

  const tempId = `temp-${Date.now()}`
  messageStore.addOptimisticMessage({
    _tempId:  tempId,
    userId:   authStore.userId,
    nickname: authStore.nickname,
    content:  payload.content,
    type:     payload.type,
    attachments: payload.attachments,
    reactions: [],
    isAiMessage: false,
    isDeleted: false,
    createdAt: new Date().toISOString(),
  })

  try {
    stompClient.sendDmMessage(dmRoomId, { ...payload, tempId })
  } catch {
    messageStore.removeOptimisticMessage(tempId)
    uiStore.showToast('error', '메시지 전송에 실패했습니다.')
  }
}

function handleTyping() {
  // DM은 타이핑 인디케이터 없음 (spec에 미포함)
}

// ── WebSocket DM 이벤트 처리 ────────────────────────────────
function handleDmEvent(event) {
  const dmRoomId = dmStore.currentDmRoomId

  switch (event.eventType) {
    case 'CHAT':
      messageStore.addMessage(event.message, event.tempId)
      dmStore.updateLastMessage(dmRoomId, event.message)
      // 상대방 메시지이면 읽음 처리
      if (event.message.userId !== authStore.userId) {
        dmApi.markDmAsRead(dmRoomId).catch(() => {})
      }
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
  }
}

// ── WebSocket 알림 이벤트 ───────────────────────────────────
function handleNotificationEvent(event) {
  if (event.eventType === 'UNREAD_COUNT') {
    const { channelId, dmRoomId, unreadCount } = event
    if (channelId) {
      const ch = channelStore.channels.find(c => c.id === channelId)
      if (ch) ch.unreadCount = unreadCount
    } else if (dmRoomId) {
      if (dmRoomId !== dmStore.currentDmRoomId) {
        dmStore.updateUnread(dmRoomId, unreadCount)
      }
    }
  }
}

// ── 메시지 삭제 ─────────────────────────────────────────────
function handleDeleteMessage(messageId) {
  uiStore.openModal({
    title: '메시지 삭제',
    message: '이 메시지를 삭제할까요?',
    onConfirm: async () => {
      try { await messageApi.delete(messageId) } catch { /* 인터셉터에서 처리 */ }
    },
  })
}

// ── 메시지 수정 ─────────────────────────────────────────────
async function handleEditMessage(messageId, content) {
  try { await messageApi.update(messageId, { content }) } catch { /* 인터셉터에서 처리 */ }
}

// ── 이모지 반응 ─────────────────────────────────────────────
async function handleToggleReaction(messageId, emoji) {
  const msg = messageStore.messages.find(m => m.id === messageId)
  if (!msg) return
  const existing = msg.reactions?.find(r => r.emoji === emoji)
  const alreadyReacted = existing?.userIds != null
    ? existing.userIds.includes(authStore.userId)
    : !!existing?.isMyReaction
  try {
    if (alreadyReacted) await reactionApi.remove(messageId, { emoji })
    else                await reactionApi.add(messageId, { emoji })
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
  margin-top: 64px;
}

.channel-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
}

/* DM 헤더 */
.dm-header {
  height: 56px;
  min-height: 56px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid var(--color-border);
  background: #fff;
}

.dm-header-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dm-avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.presence-overlay {
  position: absolute;
  bottom: -1px;
  right: -1px;
}

.dm-header-name {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-text);
}

.dm-header-status {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.channel-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
}

.floating-emoji {
  position: fixed;
  z-index: 500;
}
</style>
