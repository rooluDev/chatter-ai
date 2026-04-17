import api from './axios'

export const messageApi = {
  update: (id, data) => api.put(`/messages/${id}`, data),
  delete: (id)       => api.delete(`/messages/${id}`),
}
