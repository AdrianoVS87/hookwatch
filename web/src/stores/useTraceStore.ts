import { create } from 'zustand'
import type { Trace, Span } from '../types'
import { fetchTraces, fetchTrace } from '../api/traces'

// ---------------------------------------------------------------------------
// Demo data — realistic traces shown when real API is unavailable
// ---------------------------------------------------------------------------

const now = new Date()
const t = (offsetMs: number) => new Date(now.getTime() - offsetMs).toISOString()

const DEMO_SPANS_1: Span[] = [
  {
    id: 'demo-span-1', traceId: 'demo-trace-1', parentSpanId: null,
    name: 'web_search', type: 'TOOL_CALL', status: 'COMPLETED',
    model: null, inputTokens: null, outputTokens: null, cost: null, sortOrder: 0,
    input: '{"query": "Spring Boot JPA best practices 2026"}',
    output: '{"results": [{"title": "Spring Data JPA Guide", "url": "https://spring.io/"}]}',
    error: null, startedAt: t(12000), completedAt: t(10500),
  },
  {
    id: 'demo-span-2', traceId: 'demo-trace-1', parentSpanId: 'demo-span-1',
    name: 'claude-sonnet-completion', type: 'LLM_CALL', status: 'COMPLETED',
    model: 'claude-sonnet-4-6', inputTokens: 612, outputTokens: 1211, cost: 0.019989, sortOrder: 1,
    input: 'You are a senior Java engineer. Review this service layer for issues:\n```java\n// WebhookService.java\n```',
    output: 'The service looks well-structured. Key suggestions:\n1. Add @Transactional(readOnly=true) on query methods\n2. Use Optional.orElseThrow() consistently\n3. Consider @EntityGraph to avoid N+1 on findAll()',
    error: null, startedAt: t(10400), completedAt: t(5100),
  },
]

const DEMO_SPANS_2: Span[] = [
  {
    id: 'demo-span-3', traceId: 'demo-trace-2', parentSpanId: null,
    name: 'vector_retrieve', type: 'RETRIEVAL', status: 'COMPLETED',
    model: null, inputTokens: null, outputTokens: null, cost: null, sortOrder: 0,
    input: '{"query_embedding": "[0.12, 0.87, ...]", "top_k": 5}',
    output: '{"documents": [{"content": "Flyway migration best practices...", "score": 0.94}]}',
    error: null, startedAt: t(95000), completedAt: t(93500),
  },
  {
    id: 'demo-span-4', traceId: 'demo-trace-2', parentSpanId: 'demo-span-3',
    name: 'claude-sonnet-completion', type: 'LLM_CALL', status: 'FAILED',
    model: 'claude-sonnet-4-6', inputTokens: 380, outputTokens: 132, cost: null, sortOrder: 1,
    input: 'Summarize the retrieved documents about database migrations.',
    output: null,
    error: 'Rate limit exceeded after 3 retries (429 Too Many Requests)',
    startedAt: t(93400), completedAt: t(92000),
  },
]

const DEMO_SPANS_3: Span[] = [
  {
    id: 'demo-span-5', traceId: 'demo-trace-3', parentSpanId: null,
    name: 'file_read', type: 'TOOL_CALL', status: 'COMPLETED',
    model: null, inputTokens: null, outputTokens: null, cost: null, sortOrder: 0,
    input: '{"path": "src/main/java/com/hookwatch/service/TraceService.java"}',
    output: '{"content": "package com.hookwatch.service;\\n..."}',
    error: null, startedAt: t(180000), completedAt: t(179500),
  },
  {
    id: 'demo-span-6', traceId: 'demo-trace-3', parentSpanId: 'demo-span-5',
    name: 'claude-opus-analysis', type: 'LLM_CALL', status: 'COMPLETED',
    model: 'claude-opus-4-6', inputTokens: 1501, outputTokens: 3001, cost: 0.054540, sortOrder: 1,
    input: 'Perform a deep security and performance review of this service class. Think step by step.',
    output: 'After careful analysis:\n\n**Critical:** The .toList() call on line 47 returns an immutable list. Hibernate\'s CollectionType.replaceElements() calls .clear() during merge — this will throw UnsupportedOperationException at runtime.\n\n**Fix:** Replace .toList() with new ArrayList<>(...) or collect(Collectors.toCollection(ArrayList::new)).\n\n**Performance:** Missing @Transactional(readOnly=true) on findByAgentId and findById reduces read performance by preventing Hibernate from skipping dirty checking.',
    error: null, startedAt: t(179400), completedAt: t(172000),
  },
]

const DEMO_TRACES: Trace[] = [
  {
    id: 'demo-trace-1', agentId: 'demo-agent-1', status: 'COMPLETED',
    totalTokens: 1823, totalCost: 0.019989, metadata: { model: 'claude-sonnet-4-6', task: 'code-review' },
    spans: DEMO_SPANS_1, startedAt: t(12000), completedAt: t(5000),
  },
  {
    id: 'demo-trace-2', agentId: 'demo-agent-1', status: 'FAILED',
    totalTokens: 512, totalCost: null, metadata: { model: 'claude-sonnet-4-6', task: 'summarize' },
    spans: DEMO_SPANS_2, startedAt: t(95000), completedAt: t(92000),
  },
  {
    id: 'demo-trace-3', agentId: 'demo-agent-2', status: 'COMPLETED',
    totalTokens: 4502, totalCost: 0.054540, metadata: { model: 'claude-opus-4-6', task: 'security-audit' },
    spans: DEMO_SPANS_3, startedAt: t(180000), completedAt: t(172000),
  },
]

// ---------------------------------------------------------------------------
// Store
// ---------------------------------------------------------------------------

interface TraceState {
  traces: Trace[]
  selectedTrace: Trace | null
  loading: boolean
  isDemo: boolean
  loadTraces: (agentId: string) => Promise<void>
  selectTrace: (id: string) => Promise<void>
  clearTrace: () => void
}

export const useTraceStore = create<TraceState>((set) => ({
  traces: [],
  selectedTrace: null,
  loading: false,
  isDemo: false,

  loadTraces: async (agentId: string) => {
    set({ loading: true })

    // Demo agents get demo traces immediately — no API call
    if (agentId.startsWith('demo-')) {
      const filtered = DEMO_TRACES.filter((t) => t.agentId === agentId)
      set({ traces: filtered.length > 0 ? filtered : DEMO_TRACES, loading: false, isDemo: true })
      return
    }

    try {
      const page = await fetchTraces(agentId)
      if (page.content.length > 0) {
        set({ traces: page.content, loading: false, isDemo: false })
      } else {
        // Real agent but no traces yet — show empty (not demo)
        set({ traces: [], loading: false, isDemo: false })
      }
    } catch {
      set({ traces: DEMO_TRACES, loading: false, isDemo: true })
    }
  },

  selectTrace: async (id: string) => {
    set({ selectedTrace: null })

    // Demo traces are already in memory
    if (id.startsWith('demo-')) {
      const trace = DEMO_TRACES.find((t) => t.id === id) ?? null
      set({ selectedTrace: trace })
      return
    }

    try {
      const trace = await fetchTrace(id)
      set({ selectedTrace: trace })
    } catch {
      // Trace fetch failed — panel stays empty
    }
  },

  clearTrace: () => set({ selectedTrace: null }),
}))
