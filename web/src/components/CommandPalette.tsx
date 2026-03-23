import { useEffect, useRef, useState, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Search, User, GitBranch } from 'lucide-react'
import { useUIStore } from '../stores/useUIStore'
import { useAgentStore } from '../stores/useAgentStore'
import { useTraceStore } from '../stores/useTraceStore'
import type { Agent, Trace } from '../types'

type Item = { type: 'agent'; data: Agent } | { type: 'trace'; data: Trace }

export default function CommandPalette() {
  const { commandPaletteOpen, closeCommandPalette, toggleCommandPalette } = useUIStore()
  const { agents, selectAgent } = useAgentStore()
  const { traces, selectTrace } = useTraceStore()
  const [query, setQuery] = useState('')
  const [selectedIndex, setSelectedIndex] = useState(0)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (commandPaletteOpen) {
      setQuery('')
      setSelectedIndex(0)
      setTimeout(() => inputRef.current?.focus(), 30)
    }
  }, [commandPaletteOpen])

  useEffect(() => {
    const fn = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') { e.preventDefault(); toggleCommandPalette() }
      if (e.key === 'Escape') closeCommandPalette()
    }
    window.addEventListener('keydown', fn)
    return () => window.removeEventListener('keydown', fn)
  }, [closeCommandPalette, toggleCommandPalette])

  const q = query.toLowerCase()
  const items: Item[] = [
    ...agents.filter(a => a.name.toLowerCase().includes(q) || (a.description ?? '').toLowerCase().includes(q))
      .map(a => ({ type: 'agent' as const, data: a })),
    ...traces.filter(t => t.id.toLowerCase().includes(q) || t.status.toLowerCase().includes(q))
      .map(t => ({ type: 'trace' as const, data: t })),
  ]

  const activate = useCallback((item: Item) => {
    if (item.type === 'agent') selectAgent(item.data.id)
    else selectTrace(item.data.id)
    closeCommandPalette()
  }, [selectAgent, selectTrace, closeCommandPalette])

  useEffect(() => {
    if (!commandPaletteOpen) return
    const fn = (e: KeyboardEvent) => {
      if (e.key === 'ArrowDown') { e.preventDefault(); setSelectedIndex(i => Math.min(i + 1, items.length - 1)) }
      if (e.key === 'ArrowUp') { e.preventDefault(); setSelectedIndex(i => Math.max(i - 1, 0)) }
      if (e.key === 'Enter' && items[selectedIndex]) activate(items[selectedIndex])
    }
    window.addEventListener('keydown', fn)
    return () => window.removeEventListener('keydown', fn)
  }, [commandPaletteOpen, items, selectedIndex, activate])

  useEffect(() => setSelectedIndex(0), [query])

  return (
    <AnimatePresence>
      {commandPaletteOpen && (
        <>
          <motion.div
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            transition={{ duration: 0.1 }}
            onClick={closeCommandPalette}
            style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', zIndex: 40, backdropFilter: 'blur(4px)' }}
          />
          <motion.div
            initial={{ opacity: 0, scale: 0.96, y: -8 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.96, y: -8 }}
            transition={{ duration: 0.15, ease: [0.16, 1, 0.3, 1] }}
            style={{
              position: 'fixed', top: 80, left: '50%', transform: 'translateX(-50%)',
              width: '100%', maxWidth: 520, zIndex: 50,
              background: 'var(--surface)',
              border: '1px solid var(--border)',
              borderRadius: 12,
              boxShadow: '0 24px 80px rgba(0,0,0,0.6)',
              overflow: 'hidden',
            }}
          >
            {/* Search input */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '12px 16px', borderBottom: '1px solid var(--border)' }}>
              <Search size={14} strokeWidth={1.5} style={{ color: 'var(--text-tertiary)', flexShrink: 0 }} />
              <input
                ref={inputRef}
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search agents and traces…"
                style={{
                  flex: 1, background: 'none', border: 'none', outline: 'none',
                  color: 'var(--text-primary)', fontSize: 14,
                  fontFamily: 'inherit',
                }}
              />
              <kbd style={{
                fontSize: 10, color: 'var(--text-tertiary)',
                background: 'var(--surface-2)', border: '1px solid var(--border)',
                borderRadius: 4, padding: '2px 6px', flexShrink: 0,
              }}>esc</kbd>
            </div>

            {/* Results */}
            <div style={{ maxHeight: 320, overflow: 'auto' }}>
              {items.length === 0 && (
                <div style={{ padding: '32px 16px', textAlign: 'center', color: 'var(--text-tertiary)', fontSize: 13 }}>
                  No results
                </div>
              )}
              {items.length > 0 && (
                <div style={{ padding: '6px 0' }}>
                  {items.map((item, i) => (
                    <button
                      key={`${item.type}-${item.data.id}`}
                      onClick={() => activate(item)}
                      style={{
                        display: 'flex', alignItems: 'center', gap: 10,
                        width: '100%', padding: '9px 16px', border: 'none',
                        background: i === selectedIndex ? 'var(--surface-2)' : 'transparent',
                        cursor: 'pointer', textAlign: 'left', transition: 'background 0.1s',
                      }}
                      onMouseEnter={() => setSelectedIndex(i)}
                    >
                      <div style={{
                        width: 28, height: 28, borderRadius: 6, flexShrink: 0,
                        background: item.type === 'agent' ? 'rgba(99,102,241,0.12)' : 'rgba(16,185,129,0.1)',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                      }}>
                        {item.type === 'agent'
                          ? <User size={13} strokeWidth={1.5} style={{ color: '#6366F1' }} />
                          : <GitBranch size={13} strokeWidth={1.5} style={{ color: '#10B981' }} />
                        }
                      </div>
                      <div style={{ overflow: 'hidden' }}>
                        <div style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {item.type === 'agent' ? item.data.name : `Trace ${item.data.id.slice(0, 8)}…`}
                        </div>
                        <div style={{ fontSize: 11, color: 'var(--text-tertiary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {item.type === 'agent'
                            ? (item.data.description ?? 'Agent')
                            : `${item.data.status} · ${item.data.spans.length} spans`}
                        </div>
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  )
}
