import { create } from 'zustand'
import type { Agent, AgentMetrics } from '../types'
import { fetchAgents, fetchAgentMetrics } from '../api/agents'

export const AVAILABLE_MODELS = [
  'claude-opus-4-6',
  'claude-sonnet-4-6',
  'claude-haiku-4-5',
  'codex-5.3',
] as const

export type ModelOption = typeof AVAILABLE_MODELS[number]

interface AgentState {
  agents: Agent[]
  selectedAgentId: string | null
  selectedModel: string | null
  metrics: AgentMetrics | null
  loading: boolean
  loadAgents: () => Promise<void>
  selectAgent: (id: string) => Promise<void>
  setSelectedModel: (model: string | null) => void
}

export const useAgentStore = create<AgentState>((set) => ({
  agents: [],
  selectedAgentId: null,
  selectedModel: null,
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
  setSelectedModel: (model: string | null) => {
    set({ selectedModel: model })
  },
}))
