/**
 * stompClient.js — STOMP over SockJS 싱글톤 클라이언트
 *
 * 설계 원칙:
 * - reconnectDelay: 0으로 자동 재연결 비활성화 → 수동 재연결 전략 구현 (즉시 → 3초 → 5초 → 중단)
 * - 재연결 성공 시 기존 구독(채널·DM·알림) 자동 복구
 * - JWT 만료(UNAUTHORIZED) 시 Refresh Token으로 갱신 후 재연결
 * - 하트비트: 30초마다 /app/presence/heartbeat 전송
 */

import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

// ─── 상수 ──────────────────────────────────────────────────
const RECONNECT_DELAYS = [0, 3000, 5000]   // 즉시 → 3초 → 5초
const HEARTBEAT_INTERVAL = 30_000          // 30초

// ─── 내부 상태 ──────────────────────────────────────────────
let stompClient = null
let heartbeatTimer = null
let reconnectAttempts = 0
let intentionalDisconnect = false

// 콜백 등록 저장소 (재연결 후 복구용)
const channelCallbacks      = new Map()  // channelId  → handler
const channelTypingCallbacks = new Map() // channelId  → handler
const dmCallbacks            = new Map() // dmRoomId   → handler
let   notificationCallback   = null

// STOMP 구독 객체 저장소 (unsubscribe용)
const channelSubs       = new Map()
const channelTypingSubs = new Map()
const dmSubs            = new Map()
let   notificationSub   = null

import { useAuthStore } from '@/stores/auth'
import { useUiStore }   from '@/stores/ui'

// ─── 재연결 스케줄러 ────────────────────────────────────────
function scheduleReconnect() {
  if (intentionalDisconnect) return

  const delay = RECONNECT_DELAYS[reconnectAttempts]
  if (delay === undefined) {
    // 3회 모두 실패
    const uiStore = useUiStore()
    uiStore.showToast('error', '연결이 끊어졌습니다. 페이지를 새로 고침해 주세요.')
    return
  }

  reconnectAttempts++

  if (delay === 0) {
    doReconnect()
  } else {
    setTimeout(doReconnect, delay)
  }
}

function doReconnect() {
  if (intentionalDisconnect) return
  const authStore = useAuthStore()
  if (!authStore.accessToken) return  // 로그아웃 상태면 재연결 불필요

  createClient(authStore.accessToken)
  stompClient.activate()
}

// ─── JWT 갱신 후 재연결 ─────────────────────────────────────
async function handleTokenRefreshAndReconnect() {
  const authStore = useAuthStore()
  const uiStore   = useUiStore()

  try {
    const { authApi } = await import('@/api/auth')
    const { userApi } = await import('@/api/user')

    const refreshRes = await authApi.refresh()
    authStore.setAccessToken(refreshRes.data.accessToken)

    const userRes = await userApi.getMe()
    authStore.setAuth({
      userId:      userRes.data.id,
      nickname:    userRes.data.nickname,
      role:        userRes.data.role,
      accessToken: refreshRes.data.accessToken,
    })

    reconnectAttempts = 0
    createClient(refreshRes.data.accessToken)
    stompClient.activate()
  } catch {
    authStore.clearAuth()
    const ret = encodeURIComponent(window.location.pathname + window.location.search)
    window.location.replace(`/login?ret=${ret}`)
  }
}

// ─── 하트비트 ───────────────────────────────────────────────
function startHeartbeat() {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    sendHeartbeat()
  }, HEARTBEAT_INTERVAL)
}

function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

// ─── 연결 성공 후 구독 복구 ─────────────────────────────────
function restoreSubscriptions() {
  // 채널
  for (const [channelId, handler] of channelCallbacks) {
    channelSubs.set(
      channelId,
      stompClient.subscribe(`/topic/channel/${channelId}`, (frame) => {
        handler(JSON.parse(frame.body))
      }),
    )
  }

  // 채널 타이핑
  for (const [channelId, handler] of channelTypingCallbacks) {
    channelTypingSubs.set(
      channelId,
      stompClient.subscribe(`/topic/channel/${channelId}/typing`, (frame) => {
        handler(JSON.parse(frame.body))
      }),
    )
  }

  // DM
  for (const [dmRoomId, handler] of dmCallbacks) {
    dmSubs.set(
      dmRoomId,
      stompClient.subscribe(`/topic/dm/${dmRoomId}`, (frame) => {
        handler(JSON.parse(frame.body))
      }),
    )
  }

  // 개인 알림
  if (notificationCallback) {
    notificationSub = stompClient.subscribe('/user/queue/notification', (frame) => {
      notificationCallback(JSON.parse(frame.body))
    })
  }
}

// ─── 클라이언트 생성 ─────────────────────────────────────────
function createClient(accessToken) {
  // 기존 클라이언트 정리 (이벤트 핸들러만 제거, deactivate는 이미 됐거나 불필요)
  if (stompClient) {
    stompClient.onConnect    = () => {}
    stompClient.onDisconnect = () => {}
    stompClient.onStompError = () => {}
    stompClient = null
  }

  stompClient = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    reconnectDelay: 0,  // 자동 재연결 비활성화 — 수동으로 처리
    connectHeaders: {
      Authorization: `Bearer ${accessToken}`,
    },

    onConnect: () => {
      reconnectAttempts = 0
      restoreSubscriptions()
      startHeartbeat()
    },

    onDisconnect: () => {
      stopHeartbeat()
      scheduleReconnect()
    },

    onStompError: (frame) => {
      stopHeartbeat()
      const msg = frame.headers?.message || ''
      if (msg.includes('UNAUTHORIZED')) {
        handleTokenRefreshAndReconnect()
      } else {
        const uiStore = useUiStore()
        uiStore.showToast('error', '채팅 연결에 문제가 발생했습니다.')
        scheduleReconnect()
      }
    },

    onWebSocketError: () => {
      stopHeartbeat()
      scheduleReconnect()
    },
  })
}

// ═══════════════════════════════════════════════════════════
//  Public API
// ═══════════════════════════════════════════════════════════

/**
 * WebSocket 연결을 수립한다.
 * @param {string} accessToken
 */
function connect(accessToken) {
  intentionalDisconnect = false
  reconnectAttempts = 0
  createClient(accessToken)
  stompClient.activate()
}

/**
 * WebSocket 연결을 해제한다 (의도적 종료).
 */
function disconnect() {
  intentionalDisconnect = true
  stopHeartbeat()
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
}

/**
 * 채널 메시지를 구독한다.
 * @param {number|string} channelId
 * @param {(event: object) => void} handler
 */
function subscribeChannel(channelId, handler) {
  const id = String(channelId)
  channelCallbacks.set(id, handler)

  if (stompClient?.connected) {
    channelSubs.set(
      id,
      stompClient.subscribe(`/topic/channel/${id}`, (frame) => {
        handler(JSON.parse(frame.body))
      }),
    )
  }
}

/**
 * 채널 메시지 구독을 해제한다.
 * @param {number|string} channelId
 */
function unsubscribeChannel(channelId) {
  const id = String(channelId)
  channelCallbacks.delete(id)
  channelTypingCallbacks.delete(id)

  channelSubs.get(id)?.unsubscribe()
  channelSubs.delete(id)

  channelTypingSubs.get(id)?.unsubscribe()
  channelTypingSubs.delete(id)
}

/**
 * 채널 타이핑 인디케이터를 구독한다.
 * @param {number|string} channelId
 * @param {(event: object) => void} handler
 */
function subscribeChannelTyping(channelId, handler) {
  const id = String(channelId)
  channelTypingCallbacks.set(id, handler)

  if (stompClient?.connected) {
    channelTypingSubs.set(
      id,
      stompClient.subscribe(`/topic/channel/${id}/typing`, (frame) => {
        handler(JSON.parse(frame.body))
      }),
    )
  }
}

/**
 * DM 메시지를 구독한다.
 * @param {number|string} dmRoomId
 * @param {(event: object) => void} handler
 */
function subscribeDm(dmRoomId, handler) {
  const id = String(dmRoomId)
  dmCallbacks.set(id, handler)

  if (stompClient?.connected) {
    dmSubs.set(
      id,
      stompClient.subscribe(`/topic/dm/${id}`, (frame) => {
        handler(JSON.parse(frame.body))
      }),
    )
  }
}

/**
 * DM 메시지 구독을 해제한다.
 * @param {number|string} dmRoomId
 */
function unsubscribeDm(dmRoomId) {
  const id = String(dmRoomId)
  dmCallbacks.delete(id)
  dmSubs.get(id)?.unsubscribe()
  dmSubs.delete(id)
}

/**
 * 개인 알림(미읽음 수 등)을 구독한다.
 * @param {(event: object) => void} handler
 */
function subscribeNotification(handler) {
  notificationCallback = handler

  if (stompClient?.connected) {
    notificationSub = stompClient.subscribe('/user/queue/notification', (frame) => {
      handler(JSON.parse(frame.body))
    })
  }
}

/**
 * 개인 알림 구독을 해제한다.
 */
function unsubscribeNotification() {
  notificationCallback = null
  notificationSub?.unsubscribe()
  notificationSub = null
}

/**
 * 채널에 메시지를 전송한다.
 * @param {number|string} channelId
 * @param {{ content: string, type: string, attachments?: Array }} payload
 */
function sendChannelMessage(channelId, payload) {
  if (!stompClient?.connected) return
  stompClient.publish({
    destination: `/app/channel/${channelId}/send`,
    body: JSON.stringify(payload),
  })
}

/**
 * DM 방에 메시지를 전송한다.
 * @param {number|string} dmRoomId
 * @param {{ content: string, type: string, attachments?: Array }} payload
 */
function sendDmMessage(dmRoomId, payload) {
  if (!stompClient?.connected) return
  stompClient.publish({
    destination: `/app/dm/${dmRoomId}/send`,
    body: JSON.stringify(payload),
  })
}

/**
 * 타이핑 인디케이터를 전송한다.
 * @param {number|string} channelId
 */
function sendTyping(channelId) {
  if (!stompClient?.connected) return
  stompClient.publish({
    destination: `/app/channel/${channelId}/typing`,
    body: JSON.stringify({}),
  })
}

/**
 * 온라인 하트비트를 전송한다 (30초마다 자동 호출).
 */
function sendHeartbeat() {
  if (!stompClient?.connected) return
  stompClient.publish({
    destination: '/app/presence/heartbeat',
    body: JSON.stringify({}),
  })
}

/**
 * 자리비움 상태로 전환한다.
 */
function sendAway() {
  if (!stompClient?.connected) return
  stompClient.publish({
    destination: '/app/presence/away',
    body: JSON.stringify({}),
  })
}

/** 현재 연결 여부를 반환한다. */
function isConnected() {
  return stompClient?.connected === true
}

export default {
  connect,
  disconnect,
  subscribeChannel,
  unsubscribeChannel,
  subscribeChannelTyping,
  subscribeDm,
  unsubscribeDm,
  subscribeNotification,
  unsubscribeNotification,
  sendChannelMessage,
  sendDmMessage,
  sendTyping,
  sendHeartbeat,
  sendAway,
  isConnected,
}
