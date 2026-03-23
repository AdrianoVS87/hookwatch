export type SpanType = 'LLM_CALL' | 'TOOL_CALL' | 'RETRIEVAL' | 'CUSTOM'
export type SpanStatus = 'RUNNING' | 'COMPLETED' | 'FAILED'
export type TraceStatus = 'RUNNING' | 'COMPLETED' | 'FAILED'

export interface Span {
  id: string
  traceId: string
  parentSpanId: string | null
  name: string
  type: SpanType
  status: SpanStatus
  startedAt: string
  completedAt: string | null
  inputTokens: number | null
  outputTokens: number | null
  cost: number | null
  model: string | null
  input: string | null
  output: string | null
  error: string | null
  sortOrder: number | null
}

export interface Trace {
  id: string
  agentId: string
  status: TraceStatus
  startedAt: string
  completedAt: string | null
  totalTokens: number | null
  totalCost: number | null
  metadata: Record<string, unknown> | null
  spans: Span[]
}

export interface Agent {
  id: string
  tenantId: string
  name: string
  description: string | null
  createdAt: string
}

export interface AgentMetrics {
  totalTraces: number
  avgTokens: number
  avgCost: number
  successRate: number
  p95LatencyMs: number
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}
