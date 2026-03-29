import { client } from './client'
import type { AnalyticsData } from '../types'

export async function fetchAnalytics(
  agentId: string,
  from: string,
  to: string,
  granularity: string = 'day',
  model?: string | null,
): Promise<AnalyticsData> {
  const params: Record<string, string> = { agentId, from, to, granularity }
  if (model) params.model = model
  const { data } = await client.get('/analytics', { params })
  return data
}
