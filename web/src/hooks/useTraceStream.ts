import { useEffect } from 'react'
import { useTraceStore } from '../stores/useTraceStore'
import type { Trace } from '../types'

const API_KEY = import.meta.env.VITE_API_KEY ?? 'demo-key-hookwatch'
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

/**
 * Opens an SSE connection for the given traceId and updates the store
 * when new span data arrives. Cleans up on unmount or traceId change.
 */
export function useTraceStream(traceId: string | null) {
  useEffect(() => {
    if (!traceId) return

    // EventSource doesn't support custom headers natively;
    // pass API key as query param for SSE endpoints
    const url = `${BASE_URL}/traces/${traceId}/stream?apiKey=${API_KEY}`
    const es = new EventSource(url)

    es.addEventListener('span', (e) => {
      try {
        const trace = JSON.parse(e.data) as Trace
        useTraceStore.getState().selectTrace(trace.id)
      } catch {
        // malformed event, skip
      }
    })

    es.onerror = () => es.close()

    return () => es.close()
  }, [traceId])
}
