import { client } from './client'

export interface ComplianceGap {
  field: string
  expected: string
  actual: string
  severity: 'ERROR' | 'WARNING' | 'INFO'
}

export interface TraceComplianceReport {
  traceId: string
  totalChecks: number
  passed: number
  failed: number
  gaps: ComplianceGap[]
}

export interface AgentComplianceSummary {
  agentId: string
  totalTraces: number
  compliantTraces: number
  complianceRate: number
  topGaps: { field: string; count: number; severity: string }[]
}

/**
 * Fetches the OTel compliance report for a single trace.
 */
export async function fetchTraceCompliance(traceId: string): Promise<TraceComplianceReport> {
  const { data } = await client.get(`/traces/${traceId}/compliance`)
  return data
}

/**
 * Fetches the aggregate compliance summary for an agent.
 */
export async function fetchAgentCompliance(agentId: string, limit = 100): Promise<AgentComplianceSummary> {
  const { data } = await client.get('/compliance/summary', { params: { agentId, limit } })
  return data
}
