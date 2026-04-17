import api from './axios'

export const channelApi = {
  getChannels:       ()             => api.get('/channels'),
  getChannelDetail:  (id)           => api.get(`/channels/${id}`),
  getChannelMessages:(id, params)   => api.get(`/channels/${id}/messages`, { params }),
  joinChannel:       (id)           => api.post(`/channels/${id}/join`),
  markAsRead:        (id)           => api.delete(`/channels/${id}/unread`),
}
