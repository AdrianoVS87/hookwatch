import { client } from './client'
import type { PageResponse, Score, Trace } from '../types'

export const fetchTraces = (agentId: string, page = 0, size = 20): Promise<PageResponse<Trace>> =>
  client.get<PageResponse<Trace>>('/traces', { params: { agentId, page, size } }).then((r) => r.data)

export const fetchTrace = (id: string): Promise<Trace> =>
  client.get<Trace>(`/traces/${id}`).then((r) => r.data)

export const fetchTraceScores = (traceId: string): Promise<Score[]> =>
  client.get<Score[]>(`/traces/${traceId}/scores`).then((r) => r.data)
