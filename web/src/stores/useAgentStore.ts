import { create } from 'zustand'
import type { Agent, AgentMetrics } from '../types'
import { fetchAgents, fetchAgentMetrics } from '../api/agents'

/**
 * Demo agents shown when the real API is unreachable or returns empty.
 * Replaced automatically once real data loads successfully.
 */
const DEMO_AGENTS: Agent[] = [
  {
    id: 'demo-agent-1',
    tenantId: 'demo-tenant',
    name: 'OpenClaw Assistant',
    description: 'General-purpose assistant for developer workflows',
    createdAt: new Date(Date.now() - 86400000 * 3).toISOString(),
  },
  {
    id: 'demo-agent-2',
    tenantId: 'demo-tenant',
    name: 'Code Review Bot',
    description: 'Automated code review and quality analysis',
    createdAt: new Date(Date.now() - 86400000 * 2).toISOString(),
  },
]

const DEMO_METRICS: AgentMetrics = {
  totalTraces: 47,
  avgTokens: 2841,
  avgCost: 0.0231,
  successRate: 93.6,
  p95LatencyMs: 4200,
}

interface AgentState {
  agents: Agent[]
  selectedAgentId: string | null
  metrics: AgentMetrics | null
  loading: boolean
  /** true when displaying demo data instead of live API data */
  isDemo: boolean
  loadAgents: () => Promise<void>
  selectAgent: (id: string) => Promise<void>
}

export const useAgentStore = create<AgentState>((set) => ({
  agents: [],
  selectedAgentId: null,
  metrics: null,
  loading: false,
  isDemo: false,

  loadAgents: async () => {
    set({ loading: true })
    try {
      const agents = await fetchAgents()
      if (agents.length > 0) {
        // Real data available — use it
        set({ agents, loading: false, isDemo: false })
      } else {
        // API reachable but empty — show demo data with indicator
        set({ agents: DEMO_AGENTS, loading: false, isDemo: true })
      }
    } catch {
      // API unreachable — fall back to demo data silently
      set({ agents: DEMO_AGENTS, loading: false, isDemo: true })
    }
  },

  selectAgent: async (id: string) => {
    set({ selectedAgentId: id, metrics: null })
    // Demo agents get demo metrics
    if (id.startsWith('demo-')) {
      set({ metrics: DEMO_METRICS })
      return
    }
    try {
      const metrics = await fetchAgentMetrics(id)
      set({ metrics })
    } catch {
      // Metrics unavailable — panel stays empty rather than crash
    }
  },
}))
