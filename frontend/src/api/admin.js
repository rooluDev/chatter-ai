import api from './axios'

export const adminApi = {
  // 채널
  createChannel: (data)       => api.post('/admin/channels', data),
  deleteChannel: (id)         => api.delete(`/admin/channels/${id}`),

  // 공지
  setNotice:     (channelId, data) => api.post(`/admin/channels/${channelId}/notice`, data),
  removeNotice:  (channelId)       => api.delete(`/admin/channels/${channelId}/notice`),

  // 회원
  getUsers:      (params)     => api.get('/admin/users', { params }),
  disableUser:   (id)         => api.put(`/admin/users/${id}/disable`),
  enableUser:    (id)         => api.put(`/admin/users/${id}/enable`),
}
