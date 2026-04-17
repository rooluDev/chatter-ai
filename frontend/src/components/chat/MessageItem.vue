<template>
  <div
    class="message-item"
    :class="{
      'first-in-group': isFirstInGroup,
      'ai-message': message.isAiMessage && !message.isAiLoading,
      'deleted': message.isDeleted,
      'optimistic': message._tempId,
    }"
    @mouseenter="hovered = true"
    @mouseleave="hovered = false; closeMenu()"
  >
    <!-- 날짜 구분선 -->
    <div v-if="showDateDivider" class="date-divider">
      <span>{{ dateLabel }}</span>
    </div>

    <!-- 메시지 본체 -->
    <div class="message-row">
      <!-- 아바타 영역 (36px) -->
      <div class="avatar-col">
        <template v-if="isFirstInGroup">
          <div v-if="message.isAiMessage" class="ai-avatar">🤖</div>
          <UserAvatar v-else :nickname="message.nickname || '?'" size="sm" />
        </template>
        <span v-else class="hover-time" :class="{ visible: hovered }">
          {{ shortTime(message.createdAt) }}
        </span>
      </div>

      <!-- 내용 -->
      <div class="message-body">
        <!-- 헤더 (첫 번째 메시지만) -->
        <div v-if="isFirstInGroup" class="message-header">
          <span class="message-nickname" :class="{ 'ai-nickname': message.isAiMessage }">
            {{ message.isAiMessage ? 'ChatterAI' : message.nickname }}
          </span>
          <span class="message-time">{{ fullTime(message.createdAt) }}</span>
        </div>

        <!-- 수정 모드 -->
        <MessageEditInline
          v-if="isEditing"
          :initial-content="message.content"
          @save="handleEditSave"
          @cancel="isEditing = false"
        />

        <!-- AI 로딩 -->
        <div v-else-if="message.isAiLoading" class="ai-content ai-loading">
          <span class="dot" />
          <span class="dot" style="animation-delay: 0.2s" />
          <span class="dot" style="animation-delay: 0.4s" />
        </div>

        <!-- 삭제된 메시지 -->
        <p v-else-if="message.isDeleted" class="deleted-content">🚫 삭제된 메시지입니다.</p>

        <!-- AI 메시지 내용 -->
        <div v-else-if="message.isAiMessage" class="ai-content">{{ message.content }}</div>

        <!-- 일반 메시지 내용 -->
        <p v-else class="message-content" v-html="highlightAi(message.content)" />

        <!-- 첨부파일 -->
        <div v-if="!message.isDeleted && message.attachments?.length" class="attachments">
          <a
            v-for="att in message.attachments"
            :key="att.fileUrl"
            class="attachment-item"
            :href="att.fileUrl"
            target="_blank"
            rel="noopener noreferrer"
          >
            <span>{{ att.fileType === 'IMAGE' ? '🖼️' : '📄' }}</span>
            <span class="att-name">{{ att.fileName }}</span>
            <span class="att-size">{{ formatSize(att.fileSize) }}</span>
          </a>
        </div>

        <!-- 이모지 반응 -->
        <ReactionBar
          v-if="!message.isDeleted && !message.isAiLoading"
          :reactions="message.reactions ?? []"
          :show-add="false"
          @toggle="(emoji) => $emit('toggleReaction', message.id, emoji)"
        />
      </div>

      <!-- 호버 액션 버튼 -->
      <div v-if="hovered && !message.isDeleted && !message.isAiLoading" class="message-actions">
        <button class="action-btn" title="이모지 반응" @click.stop="$emit('openEmojiPicker', message.id, $event)">😊</button>
        <button class="action-btn" title="답장 (준비 중)">↩</button>
        <div class="action-more-wrapper" ref="moreRef">
          <button class="action-btn" title="더보기" @click.stop="toggleMenu">⋯</button>
          <div v-if="menuOpen" class="more-menu">
            <button
              v-if="canEdit"
              class="more-item"
              @click="startEdit"
            >수정</button>
            <button
              v-if="canDelete"
              class="more-item danger"
              @click="$emit('delete', message.id); menuOpen = false"
            >삭제</button>
            <button
              v-if="authStore.isAdmin"
              class="more-item"
              @click="$emit('setNotice', message.id); menuOpen = false"
            >공지로 지정</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from '@/components/common/UserAvatar.vue'
import ReactionBar from './ReactionBar.vue'
import MessageEditInline from './MessageEditInline.vue'

const props = defineProps({
  message:        { type: Object,  required: true },
  isFirstInGroup: { type: Boolean, default: true },
  showDateDivider:{ type: Boolean, default: false },
  prevDate:       { type: String,  default: null },
})

const emit = defineEmits([
  'toggleReaction',
  'openEmojiPicker',
  'delete',
  'edit',
  'setNotice',
])

const authStore = useAuthStore()
const hovered   = ref(false)
const menuOpen  = ref(false)
const isEditing = ref(false)
const moreRef   = ref(null)

const canEdit = computed(() =>
  !props.message.isAiMessage &&
  props.message.userId === authStore.userId
)

const canDelete = computed(() => {
  if (props.message.isAiMessage) return authStore.isAdmin
  return props.message.userId === authStore.userId || authStore.isAdmin
})

const dateLabel = computed(() => {
  if (!props.message.createdAt) return ''
  const d = new Date(props.message.createdAt)
  const today = new Date()
  const yesterday = new Date(today)
  yesterday.setDate(today.getDate() - 1)

  if (isSameDay(d, today)) return '오늘'
  if (isSameDay(d, yesterday)) return '어제'
  return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일`
})

function isSameDay(a, b) {
  return a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
}

function fullTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false })
}

function shortTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false })
}

function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return `${bytes}B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)}MB`
}

function highlightAi(content) {
  if (!content) return ''
  // XSS 방지: 텍스트 이스케이프 후 @AI만 강조
  const escaped = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  return escaped.replace(/@AI/g, '<strong class="ai-mention">@AI</strong>')
}

function toggleMenu() {
  menuOpen.value = !menuOpen.value
  if (menuOpen.value) {
    setTimeout(() => document.addEventListener('click', closeMenuOutside), 0)
  }
}

function closeMenu() {
  menuOpen.value = false
  document.removeEventListener('click', closeMenuOutside)
}

function closeMenuOutside(e) {
  if (moreRef.value && !moreRef.value.contains(e.target)) {
    closeMenu()
  }
}

function startEdit() {
  menuOpen.value = false
  isEditing.value = true
}

function handleEditSave(newContent) {
  isEditing.value = false
  emit('edit', props.message.id, newContent)
}

onUnmounted(() => document.removeEventListener('click', closeMenuOutside))
</script>

<style scoped>
.message-item {
  padding: 2px 16px;
  position: relative;
}

.message-item:hover {
  background: rgba(0, 0, 0, 0.02);
}

.message-item.first-in-group {
  margin-top: 12px;
}

.message-item.optimistic {
  opacity: 0.6;
}

/* 날짜 구분선 */
.date-divider {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 16px 0 12px;
  color: var(--color-text-muted);
  font-size: 0.75rem;
  font-weight: 600;
}

.date-divider::before,
.date-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--color-border);
}

/* 메시지 행 */
.message-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

/* 아바타 열 */
.avatar-col {
  width: 36px;
  min-width: 36px;
  display: flex;
  justify-content: center;
}

.ai-avatar {
  width: 36px;
  height: 36px;
  background: var(--color-primary-light);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.125rem;
}

/* 연속 메시지 호버 시 시각 */
.hover-time {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
  visibility: hidden;
  white-space: nowrap;
  padding-top: 3px;
}

.hover-time.visible {
  visibility: visible;
}

/* 메시지 본문 */
.message-body {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 2px;
}

.message-nickname {
  font-weight: 700;
  font-size: 0.9375rem;
  color: var(--color-text);
}

.ai-nickname {
  color: var(--color-primary);
}

.message-time {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.message-content {
  font-size: 0.9375rem;
  color: var(--color-text);
  margin: 0;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

/* AI 멘션 강조 */
:deep(.ai-mention) {
  color: var(--color-primary);
  font-weight: 700;
}

/* AI 메시지 */
.ai-content {
  background: var(--color-ai-bg);
  border: 1px solid var(--color-ai-border);
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 0.9375rem;
  color: var(--color-text);
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

/* AI 로딩 점 애니메이션 */
.ai-loading {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
}

.dot {
  width: 8px;
  height: 8px;
  background: var(--color-text-muted);
  border-radius: 50%;
  display: inline-block;
  animation: dotPulse 1.2s ease-in-out infinite;
}

@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0.2; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}

/* 삭제된 메시지 */
.deleted-content {
  font-size: 0.9375rem;
  font-style: italic;
  color: var(--color-text-muted);
  margin: 0;
}

/* 첨부파일 */
.attachments {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 6px;
}

.attachment-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  text-decoration: none;
  color: var(--color-text);
  font-size: 0.875rem;
  max-width: 300px;
  transition: background 0.15s;
}

.attachment-item:hover {
  background: var(--color-bg-tertiary);
}

.att-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.att-size {
  color: var(--color-text-muted);
  font-size: 0.8125rem;
  flex-shrink: 0;
}

/* 호버 액션 */
.message-actions {
  position: absolute;
  top: 0;
  right: 16px;
  display: flex;
  align-items: center;
  gap: 2px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 2px 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.action-btn {
  padding: 4px 6px;
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  transition: background 0.1s;
}

.action-btn:hover {
  background: var(--color-bg-secondary);
}

/* 더보기 메뉴 */
.action-more-wrapper {
  position: relative;
}

.more-menu {
  position: absolute;
  top: calc(100% + 4px);
  right: 0;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  min-width: 130px;
  overflow: hidden;
  z-index: 50;
}

.more-item {
  display: block;
  width: 100%;
  padding: 8px 14px;
  text-align: left;
  background: transparent;
  border: none;
  font-size: 0.9375rem;
  color: var(--color-text);
  cursor: pointer;
  transition: background 0.1s;
}

.more-item:hover {
  background: var(--color-bg-secondary);
}

.more-item.danger {
  color: var(--color-error);
}

.more-item.danger:hover {
  background: rgba(239, 68, 68, 0.08);
}
</style>
