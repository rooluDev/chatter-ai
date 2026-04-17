<template>
  <RouterView />
  <AppToast />
  <AppModal />
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { RouterView } from 'vue-router'
import AppToast from '@/components/common/AppToast.vue'
import AppModal from '@/components/common/AppModal.vue'
import stompClient from '@/socket/stompClient'
import { useAuthStore } from '@/stores/auth'

const AWAY_TIMEOUT = 10 * 60 * 1000  // 10분

const authStore = useAuthStore()
let awayTimer = null

function resetAwayTimer() {
  clearTimeout(awayTimer)
  awayTimer = setTimeout(() => {
    if (stompClient.isConnected() && authStore.isLoggedIn) {
      stompClient.sendAway()
    }
  }, AWAY_TIMEOUT)
}

const ACTIVITY_EVENTS = ['mousemove', 'keydown', 'mousedown', 'touchstart']

onMounted(() => {
  ACTIVITY_EVENTS.forEach(e => document.addEventListener(e, resetAwayTimer, { passive: true }))
  resetAwayTimer()
})

onUnmounted(() => {
  ACTIVITY_EVENTS.forEach(e => document.removeEventListener(e, resetAwayTimer))
  clearTimeout(awayTimer)
})
</script>
