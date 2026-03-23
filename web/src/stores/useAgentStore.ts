import { create } from 'zustand'
import type { Agent, AgentMetrics } from '../types'
import { fetchAgents, fetchAgentMetrics } from '../api/agents'

interface AgentState {
  agents: Agent[]
  selectedAgentId: string | null
  metrics: AgentMetrics | null
  loading: boolean
  loadAgents: () => Promise<void>
  selectAgent: (id: string) => Promise<void>
}

export const useAgentStore = create<AgentState>((set) => ({
  agents: [],
  selectedAgentId: null,
  metrics: null,
  loading: false,
  loadAgents: async () => {
    set({ loading: true })
    try {
      const agents = await fetchAgents()
      set({ agents, loading: false })
    } catch {
      set({ agents: [], loading: false })
    }
  },
  selectAgent: async (id: string) => {
    set({ selectedAgentId: id, metrics: null })
    try {
      const metrics = await fetchAgentMetrics(id)
      set({ metrics })
    } catch {
      // metrics unavailable
    }
  },
}))
