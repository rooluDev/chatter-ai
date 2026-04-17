import api from './axios'

export const userApi = {
  getMe:        ()          => api.get('/users/me'),
  searchUsers:  (keyword)   => api.get('/users/search', { params: { nickname: keyword } }),
  getAiUsage:   ()          => api.get('/users/me/ai-usage'),
}
