<template>
  <div class="edit-inline">
    <textarea
      ref="textareaRef"
      v-model="editContent"
      class="edit-textarea"
      rows="1"
      @keydown.enter.exact.prevent="handleSave"
      @keydown.esc="$emit('cancel')"
      @input="autoResize"
    />
    <p class="edit-hint">ESC로 취소 · Enter로 저장</p>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'

const props = defineProps({
  initialContent: { type: String, required: true },
})

const emit = defineEmits(['save', 'cancel'])

const editContent  = ref(props.initialContent)
const textareaRef  = ref(null)

onMounted(async () => {
  await nextTick()
  autoResize()
  textareaRef.value?.focus()
  // 커서를 끝으로
  const len = textareaRef.value?.value.length
  textareaRef.value?.setSelectionRange(len, len)
})

function autoResize() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 200) + 'px'
}

function handleSave() {
  const trimmed = editContent.value.trim()
  if (!trimmed || trimmed === props.initialContent) {
    emit('cancel')
    return
  }
  emit('save', trimmed)
}
</script>

<style scoped>
.edit-inline {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 2px;
}

.edit-textarea {
  width: 100%;
  padding: 8px 10px;
  background: #fff;
  border: 1px solid var(--color-primary);
  border-radius: 6px;
  font-size: 0.9375rem;
  color: var(--color-text);
  resize: none;
  overflow: hidden;
  line-height: 1.5;
  outline: none;
  font-family: inherit;
  box-shadow: 0 0 0 2px rgba(88, 101, 242, 0.2);
}

.edit-hint {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
  margin: 0;
}
</style>
