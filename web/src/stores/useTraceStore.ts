import { create } from 'zustand'
import type { Trace } from '../types'
import { fetchTraces, fetchTrace } from '../api/traces'

interface TraceState {
  traces: Trace[]
  selectedTrace: Trace | null
  loading: boolean
  loadTraces: (agentId: string) => Promise<void>
  selectTrace: (id: string) => Promise<void>
  clearTrace: () => void
}

export const useTraceStore = create<TraceState>((set) => ({
  traces: [],
  selectedTrace: null,
  loading: false,
  loadTraces: async (agentId) => {
    set({ loading: true })
    try {
      const page = await fetchTraces(agentId)
      set({ traces: page.content, loading: false })
    } catch {
      set({ loading: false })
    }
  },
  selectTrace: async (id) => {
    set({ selectedTrace: null })
    try {
      const trace = await fetchTrace(id)
      set({ selectedTrace: trace })
    } catch {
      // trace unavailable
    }
  },
  clearTrace: () => set({ selectedTrace: null }),
}))
