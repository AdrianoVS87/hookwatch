import { create } from "zustand"

interface CompareState {
  selectedTraces: string[]  // max 2 trace IDs
  toggleTrace: (id: string) => void
  clearSelection: () => void
}

export const useCompareStore = create<CompareState>((set, get) => ({
  selectedTraces: [],
  toggleTrace: (id: string) => {
    const current = get().selectedTraces
    if (current.includes(id)) {
      set({ selectedTraces: current.filter(t => t !== id) })
    } else if (current.length < 2) {
      set({ selectedTraces: [...current, id] })
    }
    // if already 2, replace the oldest
    else {
      set({ selectedTraces: [current[1], id] })
    }
  },
  clearSelection: () => set({ selectedTraces: [] }),
}))
