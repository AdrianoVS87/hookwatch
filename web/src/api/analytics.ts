import { client } from './client'
import type { AnalyticsData } from '../types'

export async function fetchAnalytics(
  agentId: string,
  from: string,
  to: string,
  granularity: string = 'day'
): Promise<AnalyticsData> {
  const { data } = await client.get('/analytics', {
    params: { agentId, from, to, granularity }
  })
  return data
}
