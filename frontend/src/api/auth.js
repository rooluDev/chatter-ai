import api from './axios'

export const authApi = {
  join:    (data) => api.post('/auth/join', data),
  login:   (data) => api.post('/auth/login', data),
  refresh: ()     => api.post('/auth/refresh', null, { _isRefresh: true }),
  logout:  ()     => api.post('/auth/logout'),
}
