<template>
  <div class="dashboard">
    <h1 class="page-title">대시보드</h1>

    <div v-if="isLoading" class="loading">불러오는 중...</div>

    <div v-else class="stat-cards">
      <div class="stat-card">
        <p class="stat-label">채널 수</p>
        <p class="stat-value">{{ stats.channelCount }}</p>
      </div>
      <div class="stat-card">
        <p class="stat-label">가입 회원</p>
        <p class="stat-value">{{ stats.userCount }}명</p>
      </div>
      <div class="stat-card">
        <p class="stat-label">활성 회원</p>
        <p class="stat-value">{{ stats.activeUserCount }}명</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { channelApi } from '@/api/channel'
import { adminApi } from '@/api/admin'

const isLoading = ref(true)
const stats = ref({ channelCount: 0, userCount: 0, activeUserCount: 0 })

onMounted(async () => {
  try {
    const [channelRes, userRes] = await Promise.all([
      channelApi.getChannels(),
      adminApi.getUsers({ page: 1, size: 1 }),
    ])
    stats.value.channelCount    = channelRes.data?.length ?? 0
    stats.value.userCount       = userRes.data?.totalCount ?? 0
    stats.value.activeUserCount = userRes.data?.content?.filter(u => u.isActive).length ?? 0
  } catch { /* 인터셉터에서 처리 */ } finally {
    isLoading.value = false
  }
})
</script>

<style scoped>
.dashboard { max-width: 800px; }

.page-title {
  font-size: 1.375rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 24px;
}

.loading {
  color: var(--color-text-muted);
  font-size: 0.9375rem;
}

.stat-cards {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.stat-card {
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 20px 28px;
  min-width: 160px;
}

.stat-label {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin: 0 0 8px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
}
</style>
