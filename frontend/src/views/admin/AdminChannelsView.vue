<template>
  <div class="admin-channels">
    <div class="page-header">
      <h1 class="page-title">채널 관리</h1>
      <button class="btn-primary" @click="showCreateModal = true">+ 새 채널 만들기</button>
    </div>

    <div v-if="isLoading" class="loading">불러오는 중...</div>

    <table v-else class="data-table">
      <thead>
        <tr>
          <th>#</th>
          <th>채널명</th>
          <th>설명</th>
          <th>공개 여부</th>
          <th>작업</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="channels.length === 0">
          <td colspan="5" class="empty-row">채널이 없습니다.</td>
        </tr>
        <tr v-for="ch in channels" :key="ch.id">
          <td class="col-id">{{ ch.id }}</td>
          <td class="col-name">#{{ ch.name }}</td>
          <td class="col-desc">{{ ch.description || '—' }}</td>
          <td class="col-public">
            <span class="badge" :class="ch.isPrivate ? 'badge-gray' : 'badge-green'">
              {{ ch.isPrivate ? '비공개' : '공개' }}
            </span>
          </td>
          <td class="col-actions">
            <button class="action-btn danger" @click="confirmDelete(ch)">삭제</button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- 채널 생성 모달 -->
    <ChannelCreateModal
      :is-open="showCreateModal"
      @close="showCreateModal = false"
      @created="handleCreated"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { channelApi } from '@/api/channel'
import { adminApi } from '@/api/admin'
import { useChannelStore } from '@/stores/channel'
import { useUiStore } from '@/stores/ui'
import ChannelCreateModal from '@/components/admin/ChannelCreateModal.vue'

const channelStore   = useChannelStore()
const uiStore        = useUiStore()
const channels       = ref([])
const isLoading      = ref(true)
const showCreateModal = ref(false)

onMounted(loadChannels)

async function loadChannels() {
  isLoading.value = true
  try {
    const res = await channelApi.getChannels()
    channels.value = res.data ?? []
  } catch { /* 인터셉터 처리 */ } finally {
    isLoading.value = false
  }
}

function handleCreated(newChannel) {
  channels.value.push(newChannel)
}

function confirmDelete(ch) {
  uiStore.openModal({
    title: '채널 삭제',
    message: `#${ch.name} 채널을 삭제할까요? 이 작업은 되돌릴 수 없습니다.`,
    onConfirm: () => deleteChannel(ch),
  })
}

async function deleteChannel(ch) {
  try {
    await adminApi.deleteChannel(ch.id)
    channels.value = channels.value.filter(c => c.id !== ch.id)
    channelStore.removeChannel(ch.id)
    uiStore.showToast('success', `#${ch.name} 채널이 삭제되었습니다.`)
  } catch { /* 인터셉터 처리 */ }
}
</script>

<style scoped>
.admin-channels { max-width: 900px; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-size: 1.375rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
}

.loading {
  color: var(--color-text-muted);
  font-size: 0.9375rem;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  overflow: hidden;
  font-size: 0.9375rem;
}

.data-table th {
  padding: 12px 16px;
  text-align: left;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-muted);
  background: var(--color-bg-secondary);
  border-bottom: 1px solid var(--color-border);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.data-table td {
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text);
  vertical-align: middle;
}

.data-table tr:last-child td { border-bottom: none; }

.data-table tr:hover td { background: var(--color-bg-secondary); }

.col-id    { width: 60px;  color: var(--color-text-muted); }
.col-name  { font-weight: 600; }
.col-desc  { color: var(--color-text-secondary); max-width: 260px; }
.col-public{ width: 90px; }
.col-actions{ width: 80px; }

.empty-row {
  text-align: center;
  color: var(--color-text-muted);
  padding: 32px !important;
}

.badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 9999px;
  font-size: 0.8125rem;
  font-weight: 600;
}

.badge-green { background: rgba(34, 197, 94, 0.12); color: #16a34a; }
.badge-gray  { background: var(--color-bg-secondary); color: var(--color-text-muted); }

.action-btn {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
  border: 1px solid transparent;
  transition: background 0.1s;
}

.action-btn.danger {
  color: var(--color-error);
  border-color: var(--color-error);
  background: transparent;
}

.action-btn.danger:hover {
  background: rgba(239, 68, 68, 0.08);
}
</style>
