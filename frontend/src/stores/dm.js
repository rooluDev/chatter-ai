import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useDmStore = defineStore('dm', () => {
  const dmRooms = ref([])
  const currentDmRoomId = ref(null)

  const currentDmRoom = computed(() =>
    dmRooms.value.find(r => r.id === currentDmRoomId.value) ?? null
  )

  function setDmRooms(list) {
    dmRooms.value = list
  }

  function setCurrentDmRoom(id) {
    currentDmRoomId.value = id
  }

  /** DM 방 목록에 새 방 추가 또는 기존 방 갱신. */
  function upsertDmRoom(room) {
    const idx = dmRooms.value.findIndex(r => r.id === room.id)
    if (idx !== -1) {
      dmRooms.value.splice(idx, 1, room)
    } else {
      dmRooms.value.unshift(room)
    }
  }

  /** 안읽은 수 초기화 (읽음 처리 후). */
  function resetUnread(dmRoomId) {
    const room = dmRooms.value.find(r => r.id === dmRoomId)
    if (room) room.unreadCount = 0
  }

  /** 안읽은 수 업데이트 (알림 이벤트 수신 시). */
  function updateUnread(dmRoomId, unreadCount) {
    const room = dmRooms.value.find(r => r.id === dmRoomId)
    if (room) room.unreadCount = unreadCount
  }

  /** 마지막 메시지 업데이트 (새 메시지 수신 시). */
  function updateLastMessage(dmRoomId, message) {
    const room = dmRooms.value.find(r => r.id === dmRoomId)
    if (room) {
      room.lastMessage = {
        content: message.content,
        createdAt: message.createdAt,
      }
      // 목록 맨 위로 이동
      const idx = dmRooms.value.indexOf(room)
      if (idx > 0) {
        dmRooms.value.splice(idx, 1)
        dmRooms.value.unshift(room)
      }
    }
  }

  return {
    dmRooms,
    currentDmRoomId,
    currentDmRoom,
    setDmRooms,
    setCurrentDmRoom,
    upsertDmRoom,
    resetUnread,
    updateUnread,
    updateLastMessage,
  }
})
