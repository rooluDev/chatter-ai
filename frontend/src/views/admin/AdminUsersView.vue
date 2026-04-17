<template>
  <div class="admin-users">
    <h1 class="page-title">회원 관리</h1>

    <!-- 검색 -->
    <div class="search-bar">
      <input
        v-model="keyword"
        type="text"
        class="search-input"
        placeholder="닉네임 검색"
        @input="handleSearchInput"
      />
    </div>

    <div v-if="isLoading" class="loading">불러오는 중...</div>

    <template v-else>
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>아이디</th>
            <th>닉네임</th>
            <th>역할</th>
            <th>상태</th>
            <th>작업</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="users.length === 0">
            <td colspan="6" class="empty-row">회원이 없습니다.</td>
          </tr>
          <tr v-for="user in users" :key="user.id">
            <td class="col-id">{{ user.id }}</td>
            <td>{{ user.username }}</td>
            <td class="col-nickname">{{ user.nickname }}</td>
            <td>
              <span class="badge" :class="user.role === 'ADMIN' ? 'badge-purple' : 'badge-blue'">
                {{ user.role }}
              </span>
            </td>
            <td>
              <span class="badge" :class="user.isActive ? 'badge-green' : 'badge-gray'">
                {{ user.isActive ? '활성' : '비활성' }}
              </span>
            </td>
            <td class="col-actions">
              <!-- 관리자 본인 행: 작업 없음 -->
              <template v-if="user.role !== 'ADMIN'">
                <button
                  v-if="user.isActive"
                  class="action-btn danger"
                  @click="confirmDisable(user)"
                >비활성화</button>
                <button
                  v-else
                  class="action-btn primary"
                  @click="handleEnable(user)"
                >활성화</button>
              </template>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 페이지네이션 -->
      <div v-if="totalPages > 1" class="pagination">
        <button
          class="page-btn"
          :disabled="currentPage === 1"
          @click="goToPage(currentPage - 1)"
        >이전</button>
        <button
          v-for="p in pageNumbers"
          :key="p"
          class="page-btn"
          :class="{ active: p === currentPage }"
          @click="goToPage(p)"
        >{{ p }}</button>
        <button
          class="page-btn"
          :disabled="currentPage === totalPages"
          @click="goToPage(currentPage + 1)"
        >다음</button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

const authStore = useAuthStore()
const uiStore   = useUiStore()

const users       = ref([])
const isLoading   = ref(true)
const keyword     = ref('')
const currentPage = ref(1)
const totalPages  = ref(1)
const PAGE_SIZE   = 20

let searchTimer = null

const pageNumbers = computed(() => {
  const total = totalPages.value
  const cur   = currentPage.value
  const delta = 2
  const start = Math.max(1, cur - delta)
  const end   = Math.min(total, cur + delta)
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
})

onMounted(() => loadUsers())

async function loadUsers(page = 1) {
  isLoading.value = true
  currentPage.value = page
  try {
    const res = await adminApi.getUsers({
      page,
      size: PAGE_SIZE,
      ...(keyword.value.trim() ? { keyword: keyword.value.trim() } : {}),
    })
    users.value     = res.data?.content ?? []
    totalPages.value = res.data?.totalPages ?? 1
  } catch { /* 인터셉터 처리 */ } finally {
    isLoading.value = false
  }
}

function handleSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => loadUsers(1), 300)
}

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return
  loadUsers(page)
}

function confirmDisable(user) {
  uiStore.openModal({
    title: '회원 비활성화',
    message: `${user.nickname}님을 비활성화할까요? 해당 회원은 로그인할 수 없게 됩니다.`,
    onConfirm: () => handleDisable(user),
  })
}

async function handleDisable(user) {
  try {
    await adminApi.disableUser(user.id)
    const u = users.value.find(u => u.id === user.id)
    if (u) u.isActive = false
    uiStore.showToast('success', `${user.nickname}님이 비활성화되었습니다.`)
  } catch { /* 인터셉터 처리 */ }
}

async function handleEnable(user) {
  try {
    await adminApi.enableUser(user.id)
    const u = users.value.find(u => u.id === user.id)
    if (u) u.isActive = true
    uiStore.showToast('success', `${user.nickname}님이 활성화되었습니다.`)
  } catch { /* 인터셉터 처리 */ }
}
</script>

<style scoped>
.admin-users { max-width: 960px; }

.page-title {
  font-size: 1.375rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 20px;
}

.search-bar {
  margin-bottom: 16px;
}

.search-input {
  padding: 8px 14px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  font-size: 0.9375rem;
  color: var(--color-text);
  width: 260px;
  font-family: inherit;
  transition: border-color 0.15s;
}

.search-input:focus {
  outline: none;
  border-color: var(--color-primary);
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
.data-table tbody tr:hover td { background: var(--color-bg-secondary); }

.col-id       { width: 60px; color: var(--color-text-muted); }
.col-nickname { font-weight: 600; }
.col-actions  { width: 110px; }

.empty-row {
  text-align: center;
  color: var(--color-text-muted);
  padding: 32px !important;
}

/* 뱃지 */
.badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 9999px;
  font-size: 0.8125rem;
  font-weight: 600;
}

.badge-green  { background: rgba(34, 197, 94, 0.12);  color: #16a34a; }
.badge-gray   { background: var(--color-bg-secondary); color: var(--color-text-muted); }
.badge-purple { background: rgba(139, 92, 246, 0.12); color: #7c3aed; }
.badge-blue   { background: var(--color-primary-light); color: var(--color-primary); }

/* 액션 버튼 */
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

.action-btn.danger:hover { background: rgba(239, 68, 68, 0.08); }

.action-btn.primary {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background: transparent;
}

.action-btn.primary:hover { background: var(--color-primary-light); }

/* 페이지네이션 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-top: 20px;
}

.page-btn {
  min-width: 36px;
  height: 36px;
  padding: 0 10px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: #fff;
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  cursor: pointer;
  transition: background 0.1s, border-color 0.1s, color 0.1s;
}

.page-btn:hover:not(:disabled) {
  background: var(--color-bg-secondary);
  color: var(--color-text);
}

.page-btn.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
  font-weight: 600;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
