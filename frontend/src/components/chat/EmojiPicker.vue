<template>
  <div class="emoji-picker" ref="pickerRef">
    <div class="emoji-section">
      <p class="section-label">자주 쓰는 이모지</p>
      <div class="emoji-grid">
        <button
          v-for="e in recentEmojis"
          :key="e"
          class="emoji-btn"
          @click="select(e)"
        >{{ e }}</button>
      </div>
    </div>
    <div class="emoji-section">
      <p class="section-label">전체</p>
      <div class="emoji-grid">
        <button
          v-for="e in ALL_EMOJIS"
          :key="e"
          class="emoji-btn"
          @click="select(e)"
        >{{ e }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const emit = defineEmits(['select', 'close'])

const ALL_EMOJIS = [
  '😀','😃','😄','😁','😆','😅','🤣','😂','🙂','🙃',
  '😉','😊','😇','🥰','😍','🤩','😘','😗','😚','😙',
  '🥲','😋','😛','😜','🤪','😝','🤑','🤗','🤭','🤫',
  '🤔','🤐','🤨','😐','😑','😶','😶‍🌫️','😏','😒','🙄',
  '😬','🤥','😌','😔','😪','🤤','😴','😷','🤒','🤕',
  '🤢','🤮','🤧','🥵','🥶','🥴','😵','🤯','🤠','🥳',
  '🥸','😎','🤓','🧐','😕','😟','🙁','☹️','😮','😯',
  '😲','😳','🥺','😦','😧','😨','😰','😥','😢','😭',
  '😱','😖','😣','😞','😓','😩','😫','🥱','😤','😡',
  '😠','🤬','😈','👿','💀','☠️','💩','🤡','👹','👺',
  '👻','👽','👾','🤖','😺','😸','😹','😻','😼','😽',
  '🙀','😿','😾','👋','🤚','🖐️','✋','🖖','👌','🤌',
  '🤏','✌️','🤞','🤟','🤘','🤙','👈','👉','👆','🖕',
  '👇','☝️','👍','👎','✊','👊','🤛','🤜','👏','🙌',
  '👐','🤲','🤝','🙏','✍️','💅','🤳','💪','🦾','🦿',
  '❤️','🧡','💛','💚','💙','💜','🖤','🤍','🤎','💔',
  '❣️','💕','💞','💓','💗','💖','💘','💝','💟','☮️',
  '✝️','☪️','🕉️','☸️','✡️','🔯','🕎','☯️','☦️','🛐',
  '⛎','♈','♉','♊','♋','♌','♍','♎','♏','♐',
  '🎉','🎊','🎈','🎁','🎀','🎗️','🎟️','🎫','🏆','🥇',
  '🔥','💯','⭐','🌟','✨','💫','🌈','☀️','🌙','❄️',
  '👍','❤️','😂','🎉','🙏','😮','😢','🔥','💯','✅',
]

const RECENT_KEY = 'chatterai:recentEmojis'
const recentEmojis = ref(JSON.parse(localStorage.getItem(RECENT_KEY) ?? '["👍","❤️","😂","🎉","🙏","😮","😢","🔥"]'))
const pickerRef = ref(null)

function select(emoji) {
  // 최근 사용 목록 업데이트
  const list = [emoji, ...recentEmojis.value.filter(e => e !== emoji)].slice(0, 8)
  recentEmojis.value = list
  localStorage.setItem(RECENT_KEY, JSON.stringify(list))
  emit('select', emoji)
}

function handleClickOutside(e) {
  if (pickerRef.value && !pickerRef.value.contains(e.target)) {
    emit('close')
  }
}

onMounted(() => document.addEventListener('click', handleClickOutside, true))
onUnmounted(() => document.removeEventListener('click', handleClickOutside, true))
</script>

<style scoped>
.emoji-picker {
  width: 300px;
  max-height: 280px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  padding: 8px;
}

.emoji-section {
  margin-bottom: 8px;
}

.section-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-muted);
  margin: 0 0 4px 4px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.emoji-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
}

.emoji-btn {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.1s;
  line-height: 1;
}

.emoji-btn:hover {
  background: var(--color-bg-secondary);
}
</style>
