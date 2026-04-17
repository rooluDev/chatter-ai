<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="isOpen" class="modal-overlay" @click.self="close">
        <div class="modal-panel">
          <div class="modal-header">
            <h2 class="modal-title">새 채널 만들기</h2>
            <button class="close-btn" @click="close">×</button>
          </div>

          <div class="modal-body">
            <!-- 채널 이름 -->
            <div class="form-group">
              <label for="ch-name">채널 이름 <span class="required">*</span></label>
              <input
                id="ch-name"
                v-model="form.name"
                type="text"
                placeholder="ex) 개발팀"
                maxlength="50"
                :disabled="isSubmitting"
                @keydown.enter.prevent="handleSubmit"
              />
              <p v-if="errors.name" class="field-error">{{ errors.name }}</p>
            </div>

            <!-- 설명 -->
            <div class="form-group">
              <label for="ch-desc">설명 <span class="optional">(선택)</span></label>
              <textarea
                id="ch-desc"
                v-model="form.description"
                placeholder="채널 설명을 입력하세요"
                rows="3"
                maxlength="200"
                :disabled="isSubmitting"
              />
            </div>

            <!-- 비공개 토글 -->
            <div class="form-group toggle-group">
              <div class="toggle-info">
                <span class="toggle-label">비공개 채널</span>
                <span class="toggle-desc">비공개 채널은 초대된 사람만 참여 가능합니다.</span>
              </div>
              <button
                class="toggle-btn"
                :class="{ on: form.isPrivate }"
                type="button"
                @click="form.isPrivate = !form.isPrivate"
              >
                <span class="toggle-thumb" />
              </button>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn-cancel" @click="close" :disabled="isSubmitting">취소</button>
            <button class="btn-primary" @click="handleSubmit" :disabled="isSubmitting || !form.name.trim()">
              {{ isSubmitting ? '생성 중...' : '만들기' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import { adminApi } from '@/api/admin'
import { useChannelStore } from '@/stores/channel'
import { useUiStore } from '@/stores/ui'

const props = defineProps({ isOpen: { type: Boolean, default: false } })
const emit  = defineEmits(['close', 'created'])

const channelStore = useChannelStore()
const uiStore      = useUiStore()

const form = ref({ name: '', description: '', isPrivate: false })
const errors = ref({ name: '' })
const isSubmitting = ref(false)

watch(() => props.isOpen, (val) => {
  if (val) {
    form.value = { name: '', description: '', isPrivate: false }
    errors.value = { name: '' }
  }
})

async function handleSubmit() {
  errors.value.name = ''
  if (!form.value.name.trim()) {
    errors.value.name = '채널 이름을 입력해 주세요.'
    return
  }

  isSubmitting.value = true
  try {
    const res = await adminApi.createChannel({
      name:        form.value.name.trim(),
      description: form.value.description.trim() || null,
      isPrivate:   form.value.isPrivate,
    })
    channelStore.addChannel(res.data)
    uiStore.showToast('success', `#${res.data.name} 채널이 생성되었습니다.`)
    emit('created', res.data)
    close()
  } catch (err) {
    if (err?.response?.status === 409) {
      errors.value.name = '이미 사용 중인 채널 이름입니다.'
    }
    // 다른 에러는 인터셉터에서 처리
  } finally {
    isSubmitting.value = false
  }
}

function close() { emit('close') }
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
  max-width: 440px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  overflow: hidden;
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
}

.close-btn:hover { color: var(--color-text); }

.modal-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text);
}

.required { color: var(--color-error); margin-left: 2px; }
.optional  { color: var(--color-text-muted); font-weight: 400; font-size: 0.8125rem; }

.form-group input,
.form-group textarea {
  padding: 8px 12px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  font-size: 0.9375rem;
  color: var(--color-text);
  font-family: inherit;
  resize: none;
  transition: border-color 0.15s;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}

.form-group input:disabled,
.form-group textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.field-error {
  font-size: 0.8125rem;
  color: var(--color-error);
  margin: 0;
}

/* 토글 */
.toggle-group {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.toggle-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.toggle-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text);
}

.toggle-desc {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.toggle-btn {
  width: 44px;
  height: 24px;
  border-radius: 9999px;
  background: var(--color-border);
  border: none;
  cursor: pointer;
  position: relative;
  transition: background 0.2s;
  flex-shrink: 0;
}

.toggle-btn.on { background: var(--color-primary); }

.toggle-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  background: #fff;
  border-radius: 50%;
  transition: left 0.2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}

.toggle-btn.on .toggle-thumb { left: calc(100% - 22px); }

/* 하단 */
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid var(--color-border);
}

.btn-cancel {
  padding: 8px 16px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: #fff;
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
  cursor: pointer;
  transition: background 0.1s;
}

.btn-cancel:hover:not(:disabled) { background: var(--color-bg-secondary); }
.btn-cancel:disabled { opacity: 0.5; cursor: not-allowed; }

/* fade */
.fade-enter-active, .fade-leave-active { transition: opacity 0.15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
