import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useChannelStore = defineStore('channel', () => {
  const channels = ref([])
  const currentChannelId = ref(null)

  const currentChannel = computed(() =>
    channels.value.find(c => c.id === currentChannelId.value) ?? null
  )

  function setChannels(list) {
    channels.value = list
  }

  function setCurrentChannel(id) {
    currentChannelId.value = id
  }

  /** 안읽은 수 업데이트 (STOMP 메시지 수신 시). */
  function incrementUnread(channelId) {
    const ch = channels.value.find(c => c.id === channelId)
    if (ch) ch.unreadCount = (ch.unreadCount ?? 0) + 1
  }

  function resetUnread(channelId) {
    const ch = channels.value.find(c => c.id === channelId)
    if (ch) ch.unreadCount = 0
  }

  /** 채널 삭제 (CHANNEL_DELETED 이벤트). */
  function removeChannel(channelId) {
    const idx = channels.value.findIndex(c => c.id === channelId)
    if (idx !== -1) channels.value.splice(idx, 1)
  }

  /** 공지 업데이트 (NOTICE_UPDATED 이벤트). */
  function updateNotice(event) {
    const ch = channels.value.find(c => c.id === event.channelId)
    if (ch) {
      ch.noticeMessage = event.noticeMessage ?? null
    }
  }

  /** 새 채널 추가 (관리자 채널 생성 후). */
  function addChannel(channel) {
    if (!channels.value.find(c => c.id === channel.id)) {
      channels.value.push({ ...channel, unreadCount: 0 })
    }
  }

  return {
    channels,
    currentChannelId,
    currentChannel,
    setChannels,
    setCurrentChannel,
    incrementUnread,
    resetUnread,
    removeChannel,
    updateNotice,
    addChannel,
  }
})
