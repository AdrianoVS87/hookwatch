import { client } from './client'
import type { PageResponse, Trace } from '../types'

export const fetchTraces = (agentId: string, page = 0, size = 20) =>
  client.get<PageResponse<Trace>>('/traces', { params: { agentId, page, size } }).then((r) => r.data)

export const fetchTrace = (id: string) =>
  client.get<Trace>(`/traces/${id}`).then((r) => r.data)
