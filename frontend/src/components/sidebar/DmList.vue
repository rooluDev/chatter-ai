<template>
  <div class="dm-section">
    <div class="section-header">
      <span class="section-title">다이렉트 메시지</span>
      <button class="section-action" title="새 DM" @click="$emit('openSearch')">+</button>
    </div>
    <ul class="dm-list">
      <DmItem
        v-for="room in dmStore.dmRooms"
        :key="room.id"
        :room="room"
        :is-active="room.id === dmStore.currentDmRoomId"
        @select="handleSelect"
      />
    </ul>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { dmApi } from '@/api/dm'
import { useDmStore } from '@/stores/dm'
import DmItem from './DmItem.vue'

const router  = useRouter()
const dmStore = useDmStore()

defineEmits(['openSearch'])

onMounted(async () => {
  if (dmStore.dmRooms.length === 0) {
    try {
      const res = await dmApi.getDmRooms()
      dmStore.setDmRooms(res.data ?? [])
    } catch { /* 인터셉터에서 처리 */ }
  }
})

function handleSelect(roomId) {
  router.push(`/dm/${roomId}`)
}
</script>

<style scoped>
.dm-section {
  padding: 8px 8px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px 6px;
}

.section-title {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-on-sidebar);
  opacity: 0.7;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.section-action {
  color: var(--color-text-on-sidebar);
  opacity: 0.6;
  font-size: 1.25rem;
  line-height: 1;
  padding: 0 2px;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: opacity 0.15s;
}

.section-action:hover {
  opacity: 1;
}

.dm-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
</style>
