import axios from 'axios'

const API_KEY = import.meta.env.VITE_API_KEY ?? 'demo-key-hookwatch'
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

export const client = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

client.interceptors.request.use((config) => {
  config.headers['X-API-Key'] = API_KEY
  return config
})
