<template>
  <div v-if="files.length" class="file-preview">
    <div v-for="(f, idx) in files" :key="f.name + idx" class="preview-item">
      <img
        v-if="f.isImage && f.objectUrl"
        class="thumb"
        :src="f.objectUrl"
        :alt="f.name"
      />
      <span v-else class="file-icon">📄</span>
      <div class="file-info">
        <span class="file-name">{{ f.name }}</span>
        <span class="file-size">{{ formatSize(f.size) }}</span>
      </div>
      <button class="remove-btn" @click="$emit('remove', idx)" title="제거">×</button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  files: { type: Array, default: () => [] },
})

defineEmits(['remove'])

function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return `${bytes}B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)}MB`
}
</script>

<style scoped>
.file-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 12px 0;
}

.preview-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  max-width: 220px;
}

.thumb {
  width: 36px;
  height: 36px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}

.file-icon {
  font-size: 1.5rem;
  flex-shrink: 0;
}

.file-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.file-name {
  font-size: 0.8125rem;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.remove-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 1.125rem;
  color: var(--color-text-muted);
  padding: 0 2px;
  flex-shrink: 0;
  line-height: 1;
  transition: color 0.1s;
}

.remove-btn:hover {
  color: var(--color-error);
}
</style>
