import { client } from './client'
import type { Agent, AgentMetrics } from '../types'

export const fetchAgents = () =>
  client.get<Agent[]>('/agents').then((r) => r.data)

export const fetchAgentMetrics = (id: string) =>
  client.get<AgentMetrics>(`/agents/${id}/metrics`).then((r) => r.data)
