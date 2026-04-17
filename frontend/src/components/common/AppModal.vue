<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="uiStore.modal" class="modal-overlay" @click.self="handleCancel">
        <div class="modal">
          <h3 class="modal__title">{{ uiStore.modal.title }}</h3>
          <p class="modal__message">{{ uiStore.modal.message }}</p>
          <div class="modal__actions">
            <button class="modal__btn modal__btn--cancel" @click="handleCancel">
              취소
            </button>
            <button class="modal__btn modal__btn--confirm" @click="handleConfirm">
              확인
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { useUiStore } from '@/stores/ui'

const uiStore = useUiStore()

function handleConfirm() {
  uiStore.modal?.onConfirm?.()
  uiStore.closeModal()
}

function handleCancel() {
  uiStore.modal?.onCancel?.()
  uiStore.closeModal()
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
  z-index: 9998;
  padding: 20px;
}

.modal {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}

.modal__title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
}

.modal__message {
  margin: 0 0 20px;
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.modal__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.modal__btn {
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.15s;
}

.modal__btn:hover { opacity: 0.85; }

.modal__btn--cancel {
  background: var(--color-bg-secondary);
  color: var(--color-text);
}

.modal__btn--confirm {
  background: var(--color-error);
  color: #fff;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
