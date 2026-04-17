<template>
  <div class="input-area">
    <!-- 첨부 미리보기 -->
    <FilePreview :files="attachedFiles" @remove="removeFile" />

    <!-- 타이핑 인디케이터 -->
    <TypingIndicator ref="typingRef" />

    <!-- 입력창 컨테이너 -->
    <div class="input-container">
      <!-- 파일 첨부 버튼 -->
      <button
        class="input-icon-btn"
        title="파일 첨부"
        :disabled="attachedFiles.length >= 5"
        @click="triggerFileInput"
      >📎</button>
      <input
        ref="fileInputRef"
        type="file"
        multiple
        accept="image/jpeg,image/png,image/gif,image/webp,application/pdf,application/zip,text/plain,text/markdown"
        style="display:none"
        @change="handleFileSelect"
      />

      <!-- 텍스트 입력 -->
      <div class="input-wrapper">
        <div
          ref="editorRef"
          class="editor"
          contenteditable="true"
          :data-placeholder="placeholder"
          @keydown="handleKeydown"
          @input="handleInput"
          @paste="handlePaste"
        />
      </div>

      <!-- 이모지 버튼 -->
      <div class="emoji-wrapper">
        <button class="input-icon-btn" title="이모지" @click.stop="toggleEmojiPicker">😊</button>
        <div v-if="emojiPickerOpen" class="emoji-picker-popover">
          <EmojiPicker @select="insertEmoji" @close="emojiPickerOpen = false" />
        </div>
      </div>

      <!-- 전송 버튼 -->
      <button
        class="send-btn"
        :class="{ active: canSend }"
        :disabled="!canSend"
        @click="handleSend"
      >▶</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { fileApi } from '@/api/file'
import { useUiStore } from '@/stores/ui'
import FilePreview from './FilePreview.vue'
import EmojiPicker from './EmojiPicker.vue'
import TypingIndicator from './TypingIndicator.vue'

const props = defineProps({
  placeholder:   { type: String, default: '메시지를 입력하세요...' },
  channelName:   { type: String, default: '' },
})

const emit = defineEmits(['send', 'typing'])

const uiStore        = useUiStore()
const editorRef      = ref(null)
const fileInputRef   = ref(null)
const typingRef      = ref(null)
const emojiPickerOpen = ref(false)
const attachedFiles  = ref([])  // { file, name, size, isImage, objectUrl }
const isUploading    = ref(false)
const typingTimer    = ref(null)
const inputText      = ref('')

const canSend = computed(() => {
  return (inputText.value.length > 0 || attachedFiles.value.length > 0) && !isUploading.value
})

const fullPlaceholder = computed(() =>
  props.channelName ? `${props.placeholder} (#${props.channelName})` : props.placeholder
)

// ── 파일 처리 ────────────────────────────────────────────────
function triggerFileInput() {
  fileInputRef.value?.click()
}

async function handleFileSelect(e) {
  const files = Array.from(e.target.files ?? [])
  e.target.value = ''  // 동일 파일 재선택 허용

  const remaining = 5 - attachedFiles.value.length
  const toAdd = files.slice(0, remaining)

  for (const file of toAdd) {
    const isImage = file.type.startsWith('image/')
    const objectUrl = isImage ? URL.createObjectURL(file) : null
    attachedFiles.value.push({ file, name: file.name, size: file.size, isImage, objectUrl })
  }
}

function removeFile(idx) {
  const f = attachedFiles.value[idx]
  if (f?.objectUrl) URL.revokeObjectURL(f.objectUrl)
  attachedFiles.value.splice(idx, 1)
}

// ── 이모지 피커 ─────────────────────────────────────────────
function toggleEmojiPicker() {
  emojiPickerOpen.value = !emojiPickerOpen.value
}

function insertEmoji(emoji) {
  emojiPickerOpen.value = false
  editorRef.value?.focus()
  document.execCommand('insertText', false, emoji)
}

// ── 키보드 ──────────────────────────────────────────────────
function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

// ── 타이핑 이벤트 ────────────────────────────────────────────
function handleInput() {
  inputText.value = editorRef.value?.textContent?.trim() ?? ''
  applyAiHighlight()
  // 타이핑 이벤트 디바운스 (1초에 한 번)
  if (!typingTimer.value) {
    emit('typing')
    typingTimer.value = setTimeout(() => { typingTimer.value = null }, 1000)
  }
}

// ── 붙여넣기 (plain text만) ──────────────────────────────────
function handlePaste(e) {
  e.preventDefault()
  const text = e.clipboardData?.getData('text/plain') ?? ''
  document.execCommand('insertText', false, text)
}

// ── @AI 하이라이트 ───────────────────────────────────────────
// contenteditable에서 @AI를 강조하면 커서 이슈 발생 가능.
// 여기서는 입력 후 시각적 피드백만 제공 (실제 강조는 전송 시)
function applyAiHighlight() {
  // contenteditable 특성상 innerHTML 직접 조작은 커서 이슈.
  // 입력창 외부 미리보기 또는 send 시 content에서만 파싱.
}

// ── 전송 ────────────────────────────────────────────────────
async function handleSend() {
  if (!canSend.value) return

  const content = editorRef.value?.textContent?.trim() ?? ''
  clearEditor()

  let uploadedAttachments = []
  if (attachedFiles.value.length > 0) {
    isUploading.value = true
    try {
      uploadedAttachments = await uploadFiles()
    } catch {
      uiStore.showToast('error', '파일 업로드에 실패했습니다.')
      isUploading.value = false
      return
    }
    isUploading.value = false
  }

  emit('send', {
    content,
    type: uploadedAttachments.length > 0 ? 'FILE' : 'TEXT',
    attachments: uploadedAttachments,
  })

  // 첨부 파일 클리어
  attachedFiles.value.forEach(f => { if (f.objectUrl) URL.revokeObjectURL(f.objectUrl) })
  attachedFiles.value = []
}

async function uploadFiles() {
  const results = []
  for (const f of attachedFiles.value) {
    const formData = new FormData()
    formData.append('file', f.file)
    const res = await fileApi.upload(formData)
    results.push(res.data)
  }
  return results
}

function clearEditor() {
  if (editorRef.value) editorRef.value.textContent = ''
  inputText.value = ''
}

// ── 외부에서 타이핑 유저 표시 ─────────────────────────────────
function addTypingUser(userId, nickname) {
  typingRef.value?.addUser(userId, nickname)
}

function removeTypingUser(userId) {
  typingRef.value?.removeUser(userId)
}

onUnmounted(() => {
  attachedFiles.value.forEach(f => { if (f.objectUrl) URL.revokeObjectURL(f.objectUrl) })
  if (typingTimer.value) clearTimeout(typingTimer.value)
})

defineExpose({ addTypingUser, removeTypingUser })
</script>

<style scoped>
.input-area {
  border-top: 1px solid var(--color-border);
  background: #fff;
  flex-shrink: 0;
}

.input-container {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  margin: 0 16px 12px;
  background: #fff;
}

.input-icon-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 1.25rem;
  padding: 4px 6px;
  border-radius: 4px;
  flex-shrink: 0;
  transition: background 0.1s;
  line-height: 1;
}

.input-icon-btn:hover:not(:disabled) {
  background: var(--color-bg-secondary);
}

.input-icon-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.input-wrapper {
  flex: 1;
  min-width: 0;
}

.editor {
  min-height: 22px;
  max-height: 200px;
  overflow-y: auto;
  outline: none;
  font-size: 0.9375rem;
  color: var(--color-text);
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.editor:empty::before {
  content: attr(data-placeholder);
  color: var(--color-text-muted);
  pointer-events: none;
}

.emoji-wrapper {
  position: relative;
}

.emoji-picker-popover {
  position: absolute;
  bottom: calc(100% + 8px);
  right: 0;
  z-index: 100;
}

.send-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 1.125rem;
  padding: 4px 8px;
  border-radius: 6px;
  flex-shrink: 0;
  color: var(--color-text-muted);
  transition: background 0.1s, color 0.1s;
}

.send-btn.active {
  color: var(--color-primary);
}

.send-btn.active:hover {
  background: var(--color-primary-light);
}

.send-btn:disabled {
  cursor: not-allowed;
}
</style>
