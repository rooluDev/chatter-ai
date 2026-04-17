import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', () => {
  const toasts = ref([])
  const modal = ref(null)
  const isGlobalLoading = ref(false)

  let toastIdCounter = 0

  /**
   * 토스트 표시. 3초 후 자동 소멸.
   * @param {'success'|'error'|'info'|'warning'} type
   * @param {string} message
   */
  function showToast(type, message) {
    const id = ++toastIdCounter
    toasts.value.push({ id, type, message })
    setTimeout(() => removeToast(id), 3000)
  }

  function removeToast(id) {
    const idx = toasts.value.findIndex(t => t.id === id)
    if (idx !== -1) toasts.value.splice(idx, 1)
  }

  /**
   * 확인 모달 열기.
   * @param {{ title: string, message: string, onConfirm: Function, onCancel?: Function }} options
   */
  function openModal(options) {
    modal.value = options
  }

  function closeModal() {
    modal.value = null
  }

  function setGlobalLoading(val) {
    isGlobalLoading.value = val
  }

  return {
    toasts,
    modal,
    isGlobalLoading,
    showToast,
    removeToast,
    openModal,
    closeModal,
    setGlobalLoading,
  }
})
