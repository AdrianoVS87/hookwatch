import { create } from 'zustand'
import type { Agent, AgentMetrics } from '../types'

// Demo data
const DEMO_AGENTS: Agent[] = [
  { id: 'agent-1', name: 'GPT-4 Agent', description: 'Main production agent' },
  { id: 'agent-2', name: 'Claude Opus', description: 'Reasoning agent' },
  { id: 'agent-3', name: 'Gemini Pro', description: 'Multimodal agent' },
]

const DEMO_METRICS: AgentMetrics = {
  totalTraces: 142,
  avgTokens: 3847,
  avgCost: 0.0231,
  successRate: 94.4,
  p95LatencyMs: 4820,
}

interface AgentState {
  agents: Agent[]
  selectedAgentId: string | null
  metrics: AgentMetrics | null
  loading: boolean
  loadAgents: () => void
  selectAgent: (id: string) => void
}

export const useAgentStore = create<AgentState>((set) => ({
  agents: [],
  selectedAgentId: null,
  metrics: null,
  loading: false,
  loadAgents: () => {
    set({ loading: true })
    // Simulate network delay
    setTimeout(() => {
      set({ agents: DEMO_AGENTS, loading: false })
    }, 600)
  },
  selectAgent: (id) => {
    set({ selectedAgentId: id, metrics: DEMO_METRICS })
  },
}))
