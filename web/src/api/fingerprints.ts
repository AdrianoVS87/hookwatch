import { client } from './client'

export interface FingerprintSummary {
  id: string
  agentId: string
  hash: string
  errorMessage: string
  spanType: string
  model: string | null
  firstSeenAt: string
  lastSeenAt: string
  occurrenceCount: number
}

export interface FingerprintTrendPoint {
  date: string
  count: number
}

export interface FingerprintTrendResponse {
  fingerprintId: string
  trend: FingerprintTrendPoint[]
}

/**
 * Lists grouped failure fingerprints for one agent.
 */
export async function fetchFingerprints(agentId: string): Promise<FingerprintSummary[]> {
  const { data } = await client.get('/fingerprints', { params: { agentId } })
  return data
}

/**
 * Loads daily trend timeseries for one fingerprint.
 */
export async function fetchFingerprintTrend(
  fingerprintId: string,
  from: string,
  to: string,
): Promise<FingerprintTrendResponse> {
  const { data } = await client.get(`/fingerprints/${fingerprintId}/trend`, { params: { from, to } })
  return data
}
