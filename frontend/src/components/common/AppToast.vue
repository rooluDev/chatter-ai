<template>
  <Teleport to="body">
    <div class="toast-container">
      <TransitionGroup name="toast">
        <div
          v-for="toast in uiStore.toasts"
          :key="toast.id"
          class="toast"
          :class="`toast--${toast.type}`"
          @click="uiStore.removeToast(toast.id)"
        >
          <span class="toast__icon">{{ icons[toast.type] }}</span>
          <span class="toast__message">{{ toast.message }}</span>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup>
import { useUiStore } from '@/stores/ui'

const uiStore = useUiStore()

const icons = {
  success: '✓',
  error:   '✕',
  info:    'ℹ',
  warning: '⚠',
}
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
}

.toast {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  min-width: 260px;
  max-width: 400px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  pointer-events: auto;
  color: #fff;
}

.toast--success { background: #16a34a; }
.toast--error   { background: #dc2626; }
.toast--info    { background: #2563eb; }
.toast--warning { background: #d97706; }

.toast__icon {
  font-size: 16px;
  flex-shrink: 0;
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateX(40px);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(40px);
}
.toast-move {
  transition: transform 0.3s ease;
}
</style>
