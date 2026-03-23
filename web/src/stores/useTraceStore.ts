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
  loadTraces: async (agentId: string) => {
    set({ loading: true })
    try {
      const page = await fetchTraces(agentId)
      set({ traces: page.content, loading: false })
    } catch {
      set({ traces: [], loading: false })
    }
  },
  selectTrace: async (id: string) => {
    set({ selectedTrace: null })
    try {
      const trace = await fetchTrace(id)
      set({ selectedTrace: trace })
    } catch {
      // fetch failed — panel stays empty
    }
  },
  clearTrace: () => set({ selectedTrace: null }),
}))
