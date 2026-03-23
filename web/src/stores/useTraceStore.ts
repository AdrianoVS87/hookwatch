import { create } from 'zustand'
import type { Trace, Span } from '../types'

const now = new Date()
const t = (offsetMs: number) => new Date(now.getTime() - offsetMs).toISOString()

const DEMO_SPANS: Span[] = [
  {
    id: 'span-1', traceId: 'trace-1', parentId: null,
    name: 'gpt-4o chat completion', type: 'LLM_CALL', status: 'COMPLETED',
    model: 'gpt-4o', inputTokens: 1240, outputTokens: 380, cost: 0.00496,
    input: '{"messages": [{"role": "user", "content": "Summarize this document..."}]}',
    output: '{"choices": [{"message": {"content": "The document covers..."}}]}',
    error: null, startedAt: t(12000), completedAt: t(8500),
  },
  {
    id: 'span-2', traceId: 'trace-1', parentId: 'span-1',
    name: 'search_web', type: 'TOOL_CALL', status: 'COMPLETED',
    model: null, inputTokens: null, outputTokens: null, cost: null,
    input: '{"query": "latest AI research 2024"}',
    output: '{"results": [{"title": "...", "url": "..."}]}',
    error: null, startedAt: t(8400), completedAt: t(6200),
  },
  {
    id: 'span-3', traceId: 'trace-1', parentId: 'span-1',
    name: 'vector_retrieve', type: 'RETRIEVAL', status: 'COMPLETED',
    model: null, inputTokens: null, outputTokens: null, cost: null,
    input: '{"query_embedding": [...], "top_k": 5}',
    output: '{"documents": [{"content": "...", "score": 0.92}]}',
    error: null, startedAt: t(6100), completedAt: t(5200),
  },
]

const DEMO_TRACES: Trace[] = [
  {
    id: 'trace-aabb1122', agentId: 'agent-1', status: 'COMPLETED',
    totalTokens: 1620, totalCost: 0.00496, spans: DEMO_SPANS,
    startedAt: t(12000), completedAt: t(5100),
  },
  {
    id: 'trace-ccdd3344', agentId: 'agent-1', status: 'COMPLETED',
    totalTokens: 2840, totalCost: 0.0091, spans: [],
    startedAt: t(180000), completedAt: t(172000),
  },
  {
    id: 'trace-eeff5566', agentId: 'agent-1', status: 'FAILED',
    totalTokens: 540, totalCost: 0.0017, spans: [],
    startedAt: t(3600000), completedAt: t(3595000),
  },
  {
    id: 'trace-gghh7788', agentId: 'agent-1', status: 'RUNNING',
    totalTokens: null, totalCost: null, spans: [],
    startedAt: t(45000), completedAt: null,
  },
  {
    id: 'trace-iijj9900', agentId: 'agent-1', status: 'COMPLETED',
    totalTokens: 4200, totalCost: 0.0134, spans: [],
    startedAt: t(86400000), completedAt: t(86388000),
  },
]

interface TraceState {
  traces: Trace[]
  selectedTrace: Trace | null
  loading: boolean
  loadTraces: (agentId: string) => void
  selectTrace: (id: string) => void
  clearTrace: () => void
}

export const useTraceStore = create<TraceState>((set, get) => ({
  traces: [],
  selectedTrace: null,
  loading: false,
  loadTraces: (_agentId: string) => {
    set({ loading: true })
    setTimeout(() => {
      set({ traces: DEMO_TRACES, loading: false })
    }, 400)
  },
  selectTrace: (id) => {
    const trace = get().traces.find((t) => t.id === id) ?? null
    set({ selectedTrace: trace })
  },
  clearTrace: () => set({ selectedTrace: null }),
}))
