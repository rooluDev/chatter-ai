import api from './axios'

export const dmApi = {
  getDmRooms:        ()             => api.get('/dm/rooms'),
  createOrGetDmRoom: (data)         => api.post('/dm/rooms', data),
  getDmMessages:     (roomId, params) => api.get(`/dm/rooms/${roomId}/messages`, { params }),
  markDmAsRead:      (roomId)       => api.delete(`/dm/rooms/${roomId}/unread`),
}
