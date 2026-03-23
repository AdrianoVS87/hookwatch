export type SpanType = 'LLM_CALL' | 'TOOL_CALL' | 'RETRIEVAL' | 'CUSTOM'
export type SpanStatus = 'COMPLETED' | 'RUNNING' | 'FAILED'
export type TraceStatus = 'COMPLETED' | 'RUNNING' | 'FAILED'

export interface Span {
  id: string
  traceId: string
  parentId: string | null
  name: string
  type: SpanType
  status: SpanStatus
  model: string | null
  inputTokens: number | null
  outputTokens: number | null
  cost: number | null
  input: string | null
  output: string | null
  error: string | null
  startedAt: string
  completedAt: string | null
}

export interface Trace {
  id: string
  agentId: string
  status: TraceStatus
  totalTokens: number | null
  totalCost: number | null
  spans: Span[]
  startedAt: string
  completedAt: string | null
}

export interface Agent {
  id: string
  name: string
  description: string | null
}

export interface AgentMetrics {
  totalTraces: number
  avgTokens: number
  avgCost: number
  successRate: number
  p95LatencyMs: number
}
