import { create } from 'zustand'
import type { Trace } from '../types'
import { deleteTraceTag, fetchTags, fetchTrace, fetchTraces, mergeTraceTags } from '../api/traces'

interface TraceState {
  traces: Trace[]
  rawTraces: Trace[]
  totalElements: number | null
  selectedTrace: Trace | null
  loading: boolean
  availableTags: string[]
  selectedTags: string[]
  loadTraces: (agentId: string) => Promise<void>
  loadTags: () => Promise<void>
  setSelectedTags: (tags: string[]) => void
  setSelectedTag: (tag: string) => void
  addTagsToTrace: (traceId: string, tags: string[]) => Promise<void>
  removeTagFromTrace: (traceId: string, tag: string) => Promise<void>
  selectTrace: (id: string) => Promise<void>
  clearTrace: () => void
}

function normalizeTag(tag: string): string {
  return tag.trim().toLowerCase()
}

function applyTagFilter(traces: Trace[], selectedTags: string[]): Trace[] {
  if (selectedTags.length === 0) {
    return traces
  }

  return traces.filter((trace) => {
    const traceTags = (trace.tags ?? []).map(normalizeTag)
    return selectedTags.every((tag) => traceTags.includes(tag))
  })
}

function updateTraceList(traces: Trace[], updated: Trace): Trace[] {
  return traces.map((trace) => (trace.id === updated.id ? updated : trace))
}

export const useTraceStore = create<TraceState>((set, get) => ({
  traces: [],
  rawTraces: [],
  totalElements: null,
  selectedTrace: null,
  loading: false,
  availableTags: [],
  selectedTags: [],
  loadTraces: async (agentId: string) => {
    set({ loading: true })
    try {
      const page = await fetchTraces(agentId)
      const selectedTags = get().selectedTags
      set({
        rawTraces: page.content,
        traces: applyTagFilter(page.content, selectedTags),
        totalElements: page.totalElements,
        loading: false,
      })
    } catch {
      set({ traces: [], rawTraces: [], totalElements: null, loading: false })
    }
  },
  loadTags: async () => {
    try {
      const tags = await fetchTags()
      set({ availableTags: tags })
    } catch {
      set({ availableTags: [] })
    }
  },
  setSelectedTags: (tags: string[]) => {
    const normalized = Array.from(new Set(tags.map(normalizeTag).filter(Boolean))).sort()
    const rawTraces = get().rawTraces
    set({
      selectedTags: normalized,
      traces: applyTagFilter(rawTraces, normalized),
    })
  },
  setSelectedTag: (tag: string) => {
    const normalized = normalizeTag(tag)
    if (!normalized) return
    const current = get().selectedTags
    if (current.length === 1 && current[0] === normalized) {
      get().setSelectedTags([])
      return
    }
    get().setSelectedTags([normalized])
  },
  addTagsToTrace: async (traceId: string, tags: string[]) => {
    const cleaned = Array.from(new Set(tags.map(normalizeTag).filter(Boolean)))
    if (cleaned.length === 0) return

    const updated = await mergeTraceTags(traceId, cleaned)
    const state = get()
    const rawTraces = updateTraceList(state.rawTraces, updated)
    set({
      rawTraces,
      traces: applyTagFilter(rawTraces, state.selectedTags),
      selectedTrace: state.selectedTrace?.id === updated.id ? updated : state.selectedTrace,
    })
    await get().loadTags()
  },
  removeTagFromTrace: async (traceId: string, tag: string) => {
    await deleteTraceTag(traceId, tag)
    const refreshed = await fetchTrace(traceId)
    const state = get()
    const rawTraces = updateTraceList(state.rawTraces, refreshed)
    set({
      rawTraces,
      traces: applyTagFilter(rawTraces, state.selectedTags),
      selectedTrace: state.selectedTrace?.id === refreshed.id ? refreshed : state.selectedTrace,
    })
    await get().loadTags()
  },
  selectTrace: async (id: string) => {
    set({ selectedTrace: null })
    try {
      const trace = await fetchTrace(id)
      set({ selectedTrace: trace })
    } catch {
      // fetch failed — panel stays empty
    }
  },
  clearTrace: () => set({ selectedTrace: null }),
}))
