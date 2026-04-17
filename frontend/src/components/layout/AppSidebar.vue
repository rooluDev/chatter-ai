<template>
  <aside class="sidebar">
    <!-- 채널 섹션 -->
    <div class="sidebar-section">
      <div class="section-header">
        <span class="section-title">채널</span>
        <button
          v-if="authStore.isAdmin"
          class="section-action"
          title="채널 생성"
          @click="$emit('openAdmin')"
        >+</button>
      </div>
      <ul class="channel-list">
        <ChannelItem
          v-for="ch in channelStore.channels"
          :key="ch.id"
          :channel="ch"
          :isActive="ch.id === channelStore.currentChannelId"
          @select="handleChannelSelect"
        />
      </ul>
    </div>

    <!-- DM 섹션 -->
    <DmList @open-search="showDmSearch = true" />

    <!-- 하단 내 정보 -->
    <div class="sidebar-footer">
      <UserProfile />
    </div>

    <!-- 사용자 검색 모달 -->
    <UserSearchModal :is-open="showDmSearch" @close="showDmSearch = false" />
  </aside>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { channelApi } from '@/api/channel'
import { useAuthStore } from '@/stores/auth'
import { useChannelStore } from '@/stores/channel'
import ChannelItem from '@/components/sidebar/ChannelItem.vue'
import DmList from '@/components/sidebar/DmList.vue'
import UserProfile from '@/components/sidebar/UserProfile.vue'
import UserSearchModal from '@/components/common/UserSearchModal.vue'

const router       = useRouter()
const authStore    = useAuthStore()
const channelStore = useChannelStore()
const showDmSearch = ref(false)

defineEmits(['openAdmin'])

onMounted(async () => {
  if (channelStore.channels.length === 0) {
    try {
      const res = await channelApi.getChannels()
      channelStore.setChannels(res.data ?? [])
    } catch { /* 인터셉터에서 처리 */ }
  }
})

function handleChannelSelect(channelId) {
  router.push(`/channels/${channelId}`)
}
</script>

<style scoped>
.sidebar {
  width: var(--sidebar-width);
  min-width: var(--sidebar-width);
  background: var(--color-sidebar);
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  overflow-y: auto;
}

.sidebar-section {
  padding: 16px 8px 8px;
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

.channel-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.sidebar-footer {
  margin-top: auto;
}
</style>
