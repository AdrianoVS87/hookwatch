import { useEffect, useRef, useState, useMemo } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Search } from 'lucide-react'
import { useUIStore } from '../stores/useUIStore'
import { useAgentStore } from '../stores/useAgentStore'
import { useTraceStore } from '../stores/useTraceStore'

type PaletteItem = {
  id: string
  primary: string
  secondary: string
  action: () => void
}

export default function CommandPalette() {
  const { commandPaletteOpen, closeCommandPalette } = useUIStore()
  const { agents, selectAgent } = useAgentStore()
  const { traces, selectTrace } = useTraceStore()
  const [query, setQuery] = useState('')
  const [selectedIndex, setSelectedIndex] = useState(0)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (commandPaletteOpen) {
      setQuery('')
      setSelectedIndex(0)
      setTimeout(() => inputRef.current?.focus(), 50)
    }
  }, [commandPaletteOpen])

  // Global keyboard shortcut registration
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault()
        useUIStore.getState().toggleCommandPalette()
      }
      if (e.key === 'Escape') closeCommandPalette()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [closeCommandPalette])

  const q = query.toLowerCase()

  const items: PaletteItem[] = useMemo(() => {
    const agentItems = agents
      .filter((a) => a.name.toLowerCase().includes(q) || (a.description ?? '').toLowerCase().includes(q))
      .map((a) => ({
        id: `agent-${a.id}`,
        primary: a.name,
        secondary: a.description ?? a.id,
        action: () => { selectAgent(a.id); closeCommandPalette() },
      }))

    const traceItems = traces
      .filter((t) => t.id.toLowerCase().includes(q) || t.status.toLowerCase().includes(q))
      .map((t) => ({
        id: `trace-${t.id}`,
        primary: t.id.slice(0, 8) + '…',
        secondary: `${t.status} · ${t.spans.length} spans`,
        action: () => { selectTrace(t.id); closeCommandPalette() },
      }))

    return [...agentItems, ...traceItems]
  }, [agents, traces, q, selectAgent, selectTrace, closeCommandPalette])

  // Keyboard navigation within the palette
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setSelectedIndex((i) => Math.min(i + 1, items.length - 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setSelectedIndex((i) => Math.max(i - 1, 0))
    } else if (e.key === 'Enter' && items[selectedIndex]) {
      items[selectedIndex].action()
    }
  }

  return (
    <AnimatePresence>
      {commandPaletteOpen && (
        <>
          <motion.div
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/60 z-40"
            onClick={closeCommandPalette}
          />
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: -16 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: -16 }}
            className="fixed top-24 left-1/2 -translate-x-1/2 w-full max-w-lg z-50 bg-slate-800 border border-slate-600 rounded-xl shadow-2xl overflow-hidden"
          >
            <div className="flex items-center gap-3 px-4 py-3 border-b border-slate-700">
              <Search size={16} className="text-slate-400 shrink-0" />
              <input
                ref={inputRef}
                value={query}
                onChange={(e) => { setQuery(e.target.value); setSelectedIndex(0) }}
                onKeyDown={handleKeyDown}
                placeholder="Search agents or traces…"
                className="flex-1 bg-transparent text-white placeholder-slate-500 outline-none text-sm"
              />
              <kbd className="text-xs text-slate-500 bg-slate-700 px-1.5 py-0.5 rounded shrink-0">Esc</kbd>
            </div>
            <div className="max-h-80 overflow-y-auto">
              {items.length === 0 && (
                <p className="text-center text-slate-500 text-sm py-8">
                  {query ? `No results for "${query}"` : 'Type to search agents or traces'}
                </p>
              )}
              {items.map((item, idx) => (
                <button
                  key={item.id}
                  onClick={item.action}
                  className={`w-full text-left px-4 py-2.5 flex flex-col gap-0.5 transition-colors ${
                    idx === selectedIndex ? 'bg-slate-700' : 'hover:bg-slate-700/50'
                  }`}
                >
                  <span className="text-white text-sm">{item.primary}</span>
                  <span className="text-slate-400 text-xs truncate">{item.secondary}</span>
                </button>
              ))}
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  )
}
