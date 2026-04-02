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
  tags: string[]
  spans: Span[]
}

export interface Annotation {
  id: string
  traceId: string
  text: string
  author: string
  createdAt: string
}

export interface TraceMemoryLineage {
  traceId: string
  retrievalSpanCount: number
  retrievalSpanNames: string[]
  memoryReferences: string[]
  inferredOutcome: string
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

export interface Score {
  id: string
  traceId: string
  name: string
  dataType: "NUMERIC" | "CATEGORICAL" | "BOOLEAN"
  numericValue?: number
  stringValue?: string
  booleanValue?: boolean
  comment?: string
  source: "API" | "MANUAL" | "LLM_JUDGE"
  createdAt: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface SpanComparison {
  span1Name: string | null
  span2Name: string | null
  tokensDiff: number
  latencyDiffMs: number
  statusMatch: boolean
}

export interface TraceComparison {
  trace1: Trace
  trace2: Trace
  delta: {
    tokensDiff: number
    costDiff: number
    latencyDiffMs: number
    statusMatch: boolean
    spanCountDiff: number
    spanBySpanComparison: SpanComparison[]
  }
}

export interface AnalyticsData {
  dailyUsage: DailyUsage[]
  byModel: ModelUsage[]
  topExpensiveTraces: TopTrace[]
  costTrend: CostTrend
  memoryLineage: MemoryLineage[]
  learningVelocity: LearningVelocity
  learningVelocityByModel: LearningVelocityByModel[]
  failureFingerprints: FailureFingerprint[]
  otelCompliance: OTelCompliance
  evalLoopSummary: EvalLoopSummary
}

export interface DailyUsage {
  date: string
  totalTokens: number
  totalCost: number
  traceCount: number
  avgLatencyMs: number
  errorRate: number
}

export interface ModelUsage {
  model: string
  totalTokens: number
  totalCost: number
  traceCount: number
}

export interface TopTrace {
  traceId: string
  totalCost: number
  totalTokens: number
  startedAt: string
}

export interface CostTrend {
  percentChangeVsPreviousPeriod: number
  projectedMonthlyCost: number
}

export interface MemoryLineage {
  traceId: string
  retrievalSpanCount: number
  status: string
  startedAt: string
}

export interface LearningVelocity {
  costPerSuccessfulTrace: number
  repeatFailureRate: number
  memoryHitRate: number
  meanRecoveryMinutes: number
}

export interface LearningVelocityByModel {
  model: string
  successRate: number
  avgLatencyMs: number
  avgCost: number
  memoryHitRate: number
}

export interface FailureFingerprint {
  fingerprint: string
  count: number
  share: number
}

export interface OTelCompliance {
  totalTraces: number
  compliantTraces: number
  complianceRate: number
}

export interface EvalLoopSummary {
  totalTraces: number
  evaluatedTraces: number
  evaluationCoverage: number
  avgAutoQualityScore: number | null
}
