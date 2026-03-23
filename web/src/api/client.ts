import axios from 'axios'

/**
 * API key for HookWatch requests.
 * In production (Vercel), set VITE_API_KEY in the Vercel project environment variables.
 * In local dev, copy .env.example to .env and set the values.
 */
const API_KEY = import.meta.env.VITE_API_KEY ?? 'demo-key-hookwatch'

/**
 * Base URL for the HookWatch API.
 * Points to the VPS backend in production; proxied via Vite in dev.
 */
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://82.25.76.54/api/v1'

export const client = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
})

// Attach API key to every request
client.interceptors.request.use((config) => {
  config.headers['X-API-Key'] = API_KEY
  return config
})

// Log errors in development only
client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (import.meta.env.DEV) {
      console.warn('[HookWatch API]', err.config?.url, err.response?.status ?? err.message)
    }
    return Promise.reject(err)
  }
)
