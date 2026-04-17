<template>
  <span class="presence-dot" :class="`presence-dot--${normalizedStatus}`" :title="label" />
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: {
    type: String,
    default: 'OFFLINE', // ONLINE | AWAY | OFFLINE
  },
})

const normalizedStatus = computed(() =>
  ['ONLINE', 'AWAY', 'OFFLINE'].includes(props.status) ? props.status : 'OFFLINE'
)

const label = computed(() => ({
  ONLINE:  '온라인',
  AWAY:    '자리비움',
  OFFLINE: '오프라인',
}[normalizedStatus.value]))
</script>

<style scoped>
.presence-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid transparent;
  flex-shrink: 0;
}

.presence-dot--ONLINE  { background: var(--color-online); }
.presence-dot--AWAY    { background: var(--color-away); }
.presence-dot--OFFLINE { background: var(--color-offline); }
</style>
