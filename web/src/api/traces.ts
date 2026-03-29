import { client } from './client'
import type { Annotation, PageResponse, Score, Trace } from '../types'

export const fetchTraces = (agentId: string, page = 0, size = 20, tag?: string): Promise<PageResponse<Trace>> =>
  client.get<PageResponse<Trace>>('/traces', { params: { agentId, page, size, tag } }).then((r) => r.data)

export const fetchTrace = (id: string): Promise<Trace> =>
  client.get<Trace>(`/traces/${id}`).then((r) => r.data)

export const fetchTraceScores = (traceId: string): Promise<Score[]> =>
  client.get<Score[]>(`/traces/${traceId}/scores`).then((r) => r.data)

export const mergeTraceTags = (traceId: string, tags: string[]): Promise<Trace> =>
  client.post<Trace>(`/traces/${traceId}/tags`, { tags }).then((r) => r.data)

export const deleteTraceTag = (traceId: string, tag: string): Promise<void> =>
  client.delete(`/traces/${traceId}/tags/${encodeURIComponent(tag)}`).then(() => undefined)

export const fetchTags = (): Promise<string[]> =>
  client.get<string[]>('/tags').then((r) => r.data)

export const createAnnotation = (traceId: string, text: string, author: string): Promise<Annotation> =>
  client.post<Annotation>(`/traces/${traceId}/annotations`, { text, author }).then((r) => r.data)

export const fetchAnnotations = (traceId: string): Promise<Annotation[]> =>
  client.get<Annotation[]>(`/traces/${traceId}/annotations`).then((r) => r.data)
