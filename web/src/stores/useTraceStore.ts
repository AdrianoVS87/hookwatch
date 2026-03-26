import { create } from 'zustand'
import type { Trace } from '../types'
import { fetchTraces, fetchTrace } from '../api/traces'

interface TraceState {
  traces: Trace[]
  totalElements: number | null
  selectedTrace: Trace | null
  loading: boolean
  loadTraces: (agentId: string) => Promise<void>
  selectTrace: (id: string) => Promise<void>
  clearTrace: () => void
}

export const useTraceStore = create<TraceState>((set) => ({
  traces: [],
  totalElements: null,
  selectedTrace: null,
  loading: false,
  loadTraces: async (agentId: string) => {
    set({ loading: true })
    try {
      const page = await fetchTraces(agentId)
      set({ traces: page.content, totalElements: page.totalElements, loading: false })
    } catch {
      set({ traces: [], totalElements: null, loading: false })
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
