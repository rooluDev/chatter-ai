import api from './axios'

export const fileApi = {
  upload: (formData) => api.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
}
