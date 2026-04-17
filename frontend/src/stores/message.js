import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useMessageStore = defineStore('message', () => {
  const messages = ref([])
  const hasNext = ref(false)
  const nextCursorId = ref(null)
  const isLoading = ref(false)
  const isLoadingMore = ref(false)

  function setMessages(list, paginationInfo) {
    messages.value = list
    hasNext.value = paginationInfo.hasNext
    nextCursorId.value = paginationInfo.nextCursorId ?? null
  }

  /** 이전 메시지 앞에 추가 (무한 스크롤 위로). */
  function prependMessages(list, paginationInfo) {
    messages.value = [...list, ...messages.value]
    hasNext.value = paginationInfo.hasNext
    nextCursorId.value = paginationInfo.nextCursorId ?? null
  }

  /**
   * 새 메시지 추가 (STOMP CHAT 이벤트).
   * @param {object} message  서버 메시지 DTO
   * @param {string} [tempId] 클라이언트 optimistic ID (있으면 임시 메시지 교체)
   */
  function addMessage(message, tempId = null) {
    if (tempId) {
      const tmpIdx = messages.value.findIndex(m => m._tempId === tempId)
      if (tmpIdx !== -1) {
        messages.value.splice(tmpIdx, 1, message)
        return
      }
    }
    messages.value.push(message)
  }

  /**
   * Optimistic 임시 메시지 추가.
   * 서버 브로드캐스트 수신 전까지 표시.
   */
  function addOptimisticMessage(tempMessage) {
    messages.value.push(tempMessage)
  }

  /** 임시 메시지 제거 (전송 실패 시). */
  function removeOptimisticMessage(tempId) {
    const idx = messages.value.findIndex(m => m._tempId === tempId)
    if (idx !== -1) messages.value.splice(idx, 1)
  }

  /** AI 로딩 메시지 추가 (AI_LOADING 이벤트). */
  function addLoadingMessage(event) {
    messages.value.push({
      _loadingTempId: event.tempId,
      isAiLoading: true,
      content: event.content ?? 'AI가 응답을 생성 중입니다...',
      isAiMessage: true,
      createdAt: new Date().toISOString(),
    })
  }

  /** AI 로딩 → 실제 메시지 교체 (AI_RESPONSE / AI_ERROR 이벤트). */
  function replaceLoadingMessage(event) {
    const idx = messages.value.findIndex(m => m._loadingTempId === event.tempId)
    if (idx !== -1) {
      messages.value.splice(idx, 1, event.message)
    } else {
      messages.value.push(event.message)
    }
  }

  /** 메시지 수정 (MESSAGE_UPDATED 이벤트). */
  function updateMessage(message) {
    const idx = messages.value.findIndex(m => m.id === message.id)
    if (idx !== -1) messages.value.splice(idx, 1, message)
  }

  /** 메시지 소프트 삭제 (MESSAGE_DELETED 이벤트). */
  function softDeleteMessage(messageId) {
    const msg = messages.value.find(m => m.id === messageId)
    if (msg) {
      msg.isDeleted = true
      msg.content = '삭제된 메시지입니다.'
      msg.attachments = []
      msg.reactions = []
    }
  }

  /** 이모지 반응 업데이트 (REACTION_UPDATED 이벤트). */
  function updateReactions(event) {
    const idx = messages.value.findIndex(m => m.id === event.message?.id)
    if (idx !== -1) {
      messages.value.splice(idx, 1, event.message)
    }
  }

  function clearMessages() {
    messages.value = []
    hasNext.value = false
    nextCursorId.value = null
  }

  return {
    messages,
    hasNext,
    nextCursorId,
    isLoading,
    isLoadingMore,
    setMessages,
    prependMessages,
    addMessage,
    addOptimisticMessage,
    removeOptimisticMessage,
    addLoadingMessage,
    replaceLoadingMessage,
    updateMessage,
    softDeleteMessage,
    updateReactions,
    clearMessages,
  }
})
