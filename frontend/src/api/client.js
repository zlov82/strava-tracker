import axios from 'axios'

const api = axios.create()

api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      window.location.href = '/auth/strava'
    }
    return Promise.reject(error)
  }
)

export const getAthlete = () => api.get('/api/athlete').then(r => r.data)
export const getSummary = () => api.get('/api/stats/summary').then(r => r.data)
export const getWeekly = (weeks = 12, type = '') => api.get('/api/stats/weekly', { params: { weeks, type } }).then(r => r.data)
export const getMonthly = (months = 6) => api.get('/api/stats/monthly', { params: { months } }).then(r => r.data)
export const getBreakdown = () => api.get('/api/stats/breakdown').then(r => r.data)
export const getActivities = (page = 0, size = 20) => api.get('/api/activities', { params: { page, size } }).then(r => r.data)
export const getSyncStatus = () => api.get('/api/sync/status').then(r => r.data)
