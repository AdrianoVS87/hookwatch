import { useEffect, useRef, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Search } from 'lucide-react'
import { useUIStore } from '../stores/useUIStore'
import { useAgentStore } from '../stores/useAgentStore'
import { useTraceStore } from '../stores/useTraceStore'

export default function CommandPalette() {
  const { commandPaletteOpen, closeCommandPalette } = useUIStore()
  const { agents, selectAgent } = useAgentStore()
  const { traces, selectTrace } = useTraceStore()
  const [query, setQuery] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (commandPaletteOpen) {
      setQuery('')
      setTimeout(() => inputRef.current?.focus(), 50)
    }
  }, [commandPaletteOpen])

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault()
        useUIStore.getState().toggleCommandPalette()
      }
      if (e.key === 'Escape') closeCommandPalette()
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [closeCommandPalette])

  const q = query.toLowerCase()
  const matchedAgents = agents.filter((a) => a.name.toLowerCase().includes(q))
  const matchedTraces = traces.filter((t) => t.id.toLowerCase().includes(q) || t.status.toLowerCase().includes(q))

  return (
    <AnimatePresence>
      {commandPaletteOpen && (
        <>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/60 z-40"
            onClick={closeCommandPalette}
          />
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: -20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: -20 }}
            className="fixed top-24 left-1/2 -translate-x-1/2 w-full max-w-lg bg-slate-800 border border-slate-600 rounded-xl shadow-2xl z-50 overflow-hidden"
          >
            <div className="flex items-center gap-3 px-4 py-3 border-b border-slate-700">
              <Search size={16} className="text-slate-400" />
              <input
                ref={inputRef}
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search agents or traces…"
                className="flex-1 bg-transparent text-white placeholder-slate-500 outline-none text-sm"
              />
              <kbd className="text-xs text-slate-500 bg-slate-700 px-1.5 py-0.5 rounded">Esc</kbd>
            </div>
            <div className="max-h-80 overflow-y-auto">
              {matchedAgents.length > 0 && (
                <Section label="Agents">
                  {matchedAgents.map((a) => (
                    <Item
                      key={a.id}
                      label={a.name}
                      sub={a.description ?? a.id}
                      onClick={() => { selectAgent(a.id); closeCommandPalette() }}
                    />
                  ))}
                </Section>
              )}
              {matchedTraces.length > 0 && (
                <Section label="Traces">
                  {matchedTraces.map((t) => (
                    <Item
                      key={t.id}
                      label={t.id.slice(0, 8) + '…'}
                      sub={`${t.status} · ${t.spans.length} spans`}
                      onClick={() => { selectTrace(t.id); closeCommandPalette() }}
                    />
                  ))}
                </Section>
              )}
              {matchedAgents.length === 0 && matchedTraces.length === 0 && (
                <p className="text-center text-slate-500 text-sm py-8">No results</p>
              )}
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  )
}

function Section({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <p className="px-4 py-2 text-xs text-slate-500 uppercase tracking-wide">{label}</p>
      {children}
    </div>
  )
}

function Item({ label, sub, onClick }: { label: string; sub: string; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className="w-full text-left px-4 py-2.5 hover:bg-slate-700 flex flex-col"
    >
      <span className="text-white text-sm">{label}</span>
      <span className="text-slate-400 text-xs truncate">{sub}</span>
    </button>
  )
}
