<template>
  <div class="avatar" :class="`avatar--${size}`" :style="{ background: bgColor }">
    {{ initial }}
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  nickname: {
    type: String,
    default: '?',
  },
  size: {
    type: String,
    default: 'md', // sm | md | lg
  },
})

const COLORS = [
  '#5865f2', '#eb459e', '#57f287', '#fee75c', '#ed4245',
  '#3ba55c', '#faa61a', '#00b0f4', '#b5bac1', '#7289da',
]

const bgColor = computed(() => {
  // 닉네임 기반으로 일관된 색상 선택
  let hash = 0
  for (const ch of (props.nickname ?? '')) {
    hash = (hash * 31 + ch.charCodeAt(0)) % COLORS.length
  }
  return COLORS[Math.abs(hash) % COLORS.length]
})

const initial = computed(() =>
  (props.nickname ?? '?').charAt(0).toUpperCase()
)
</script>

<style scoped>
.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-weight: 600;
  color: #fff;
  flex-shrink: 0;
  user-select: none;
}

.avatar--sm {
  width: 28px;
  height: 28px;
  font-size: 12px;
}

.avatar--md {
  width: 36px;
  height: 36px;
  font-size: 15px;
}

.avatar--lg {
  width: 48px;
  height: 48px;
  font-size: 20px;
}
</style>
