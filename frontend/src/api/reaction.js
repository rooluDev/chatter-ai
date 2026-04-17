import api from './axios'

export const reactionApi = {
  add:    (messageId, data) => api.post(`/messages/${messageId}/reactions`, data),
  remove: (messageId, data) => api.delete(`/messages/${messageId}/reactions`, { data }),
}
