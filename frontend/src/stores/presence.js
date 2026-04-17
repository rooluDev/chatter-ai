import { ref } from 'vue'
import { defineStore } from 'pinia'

export const usePresenceStore = defineStore('presence', () => {
  /** { [userId]: 'ONLINE' | 'AWAY' | 'OFFLINE' } */
  const presenceMap = ref({})

  function setPresence(userId, status) {
    presenceMap.value[userId] = status
  }

  function setOffline(userId) {
    presenceMap.value[userId] = 'OFFLINE'
  }

  function getStatus(userId) {
    return presenceMap.value[userId] ?? 'OFFLINE'
  }

  function isOnline(userId) {
    return presenceMap.value[userId] === 'ONLINE'
  }

  return {
    presenceMap,
    setPresence,
    setOffline,
    getStatus,
    isOnline,
  }
})
