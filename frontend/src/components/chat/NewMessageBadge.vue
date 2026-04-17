<template>
  <Transition name="fade">
    <button v-if="count > 0" class="badge" @click="$emit('click')">
      ↓ 새 메시지 {{ count }}개
    </button>
  </Transition>
</template>

<script setup>
defineProps({ count: { type: Number, default: 0 } })
defineEmits(['click'])
</script>

<style scoped>
.badge {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 9999px;
  padding: 6px 18px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  white-space: nowrap;
  z-index: 10;
  transition: background 0.15s;
}

.badge:hover {
  background: var(--color-primary-hover);
}

.fade-enter-active,
.fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from,
.fade-leave-to     { opacity: 0; }
</style>
