/**
 * SSE (Server-Sent Events) client using fetch + ReadableStream.
 *
 * Browser's native EventSource does not support custom headers,
 * so we use fetch() to pass X-API-Key in the header instead of
 * leaking it as a query parameter (?apiKey=).
 *
 * Usage:
 *   const cleanup = streamSSE('/traces/abc/stream', (event) => {
 *     console.log(event.type, event.data)
 *   })
 *   // Later: cleanup() to abort
 */

const API_KEY = import.meta.env.VITE_API_KEY ?? 'demo-key-hookwatch'
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'https://hookwatch.adrianovs.net/api/v1'

export interface SSEEvent {
  type: string
  data: string
}

type SSEHandler = (event: SSEEvent) => void

/**
 * Opens an SSE connection using fetch + ReadableStream (header-based auth).
 * Returns an abort function to close the stream.
 */
export function streamSSE(path: string, onEvent: SSEHandler): () => void {
  const controller = new AbortController()

  ;(async () => {
    try {
      const response = await fetch(`${BASE_URL}${path}`, {
        headers: {
          'X-API-Key': API_KEY,
          Accept: 'text/event-stream',
          'Cache-Control': 'no-cache',
        },
        signal: controller.signal,
      })

      if (!response.ok || !response.body) {
        console.error('[SSE] Connection failed:', response.status)
        return
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() ?? ''

        let currentEvent: Partial<SSEEvent> = { type: 'message' }
        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent.type = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            currentEvent.data = line.slice(5).trim()
          } else if (line === '' && currentEvent.data !== undefined) {
            onEvent(currentEvent as SSEEvent)
            currentEvent = { type: 'message' }
          }
        }
      }
    } catch (err) {
      if ((err as Error).name !== 'AbortError') {
        console.error('[SSE] Stream error:', err)
      }
    }
  })()

  return () => controller.abort()
}
