<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="isOpen" class="modal-overlay" @click.self="close" @keydown.esc="close">
        <div class="modal-panel" role="dialog" aria-modal="true">
          <div class="modal-header">
            <h2 class="modal-title">새 다이렉트 메시지</h2>
            <button class="close-btn" @click="close">×</button>
          </div>

          <!-- 검색 입력 -->
          <div class="search-wrapper">
            <span class="search-icon">🔍</span>
            <input
              ref="searchInputRef"
              v-model="keyword"
              class="search-input"
              type="text"
              placeholder="닉네임 검색"
              autocomplete="off"
              @input="handleInput"
            />
          </div>

          <!-- 결과 목록 -->
          <ul class="result-list">
            <li v-if="isSearching" class="result-loading">검색 중...</li>
            <li v-else-if="keyword && results.length === 0" class="result-empty">
              검색 결과가 없습니다.
            </li>
            <li
              v-for="user in results"
              :key="user.id"
              class="result-item"
              :class="{ selected: selectedUser?.id === user.id }"
              @click="selectUser(user)"
            >
              <div class="result-avatar">
                <UserAvatar :nickname="user.nickname" size="sm" />
                <PresenceDot
                  :status="user.isOnline ? 'ONLINE' : 'OFFLINE'"
                  class="presence-overlay"
                />
              </div>
              <span class="result-nickname">{{ user.nickname }}</span>
              <span v-if="selectedUser?.id === user.id" class="check-icon">✓</span>
            </li>
          </ul>

          <!-- 하단 버튼 -->
          <div class="modal-footer">
            <button
              class="btn-primary"
              :disabled="!selectedUser || isCreating"
              @click="handleStart"
            >
              {{ isCreating ? '이동 중...' : '시작하기' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/user'
import { dmApi } from '@/api/dm'
import { useDmStore } from '@/stores/dm'
import UserAvatar from './UserAvatar.vue'
import PresenceDot from './PresenceDot.vue'

const props = defineProps({
  isOpen: { type: Boolean, default: false },
})

const emit = defineEmits(['close'])

const router  = useRouter()
const dmStore = useDmStore()

const keyword        = ref('')
const results        = ref([])
const selectedUser   = ref(null)
const isSearching    = ref(false)
const isCreating     = ref(false)
const searchInputRef = ref(null)
let   searchTimer    = null

// 열릴 때 인풋 포커스 & 상태 초기화
watch(() => props.isOpen, async (val) => {
  if (val) {
    keyword.value      = ''
    results.value      = []
    selectedUser.value = null
    await nextTick()
    searchInputRef.value?.focus()
  }
})

function handleInput() {
  selectedUser.value = null
  clearTimeout(searchTimer)
  if (keyword.value.trim().length === 0) {
    results.value  = []
    isSearching.value = false
    return
  }
  isSearching.value = true
  searchTimer = setTimeout(doSearch, 300)
}

async function doSearch() {
  try {
    const res = await userApi.searchUsers(keyword.value.trim())
    results.value = res.data ?? []
  } catch {
    results.value = []
  } finally {
    isSearching.value = false
  }
}

function selectUser(user) {
  selectedUser.value = selectedUser.value?.id === user.id ? null : user
}

async function handleStart() {
  if (!selectedUser.value || isCreating.value) return
  isCreating.value = true
  try {
    const res = await dmApi.createOrGetDmRoom({ targetUserId: selectedUser.value.id })
    const room = res.data
    dmStore.upsertDmRoom(room)
    close()
    router.push(`/dm/${room.id}`)
  } catch {
    /* 인터셉터에서 처리 */
  } finally {
    isCreating.value = false
  }
}

function close() {
  clearTimeout(searchTimer)
  emit('close')
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 400;
  padding: 1rem;
}

.modal-panel {
  width: 100%;
  max-width: 480px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.25);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: 80vh;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
}

.modal-title {
  font-size: 1.0625rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
}

.close-btn {
  background: transparent;
  border: none;
  font-size: 1.25rem;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
}

.close-btn:hover {
  color: var(--color-text);
}

/* 검색 */
.search-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--color-border);
}

.search-icon {
  font-size: 1rem;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 0.9375rem;
  color: var(--color-text);
  background: transparent;
  font-family: inherit;
}

.search-input::placeholder {
  color: var(--color-text-muted);
}

/* 결과 목록 */
.result-list {
  list-style: none;
  margin: 0;
  padding: 8px 0;
  overflow-y: auto;
  flex: 1;
  max-height: 300px;
}

.result-loading,
.result-empty {
  padding: 16px 20px;
  font-size: 0.875rem;
  color: var(--color-text-muted);
  text-align: center;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 20px;
  cursor: pointer;
  transition: background 0.1s;
}

.result-item:hover {
  background: var(--color-bg-secondary);
}

.result-item.selected {
  background: var(--color-primary-light);
}

.result-avatar {
  position: relative;
  flex-shrink: 0;
}

.presence-overlay {
  position: absolute;
  bottom: -1px;
  right: -1px;
}

.result-nickname {
  flex: 1;
  font-size: 0.9375rem;
  color: var(--color-text);
}

.check-icon {
  color: var(--color-primary);
  font-weight: 700;
}

/* 하단 버튼 */
.modal-footer {
  padding: 12px 20px;
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: flex-end;
}

/* 트랜지션 */
.fade-enter-active,
.fade-leave-active { transition: opacity 0.15s; }
.fade-enter-from,
.fade-leave-to     { opacity: 0; }
</style>
