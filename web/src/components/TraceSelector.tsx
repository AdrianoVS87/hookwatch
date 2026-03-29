import { useState, useRef, useEffect, useCallback } from 'react'
import { ChevronLeft, ChevronRight, ChevronDown, Search } from 'lucide-react'
import type { Trace, TraceStatus } from '../types'

interface Props {
  traces: Trace[]
  selectedTraceId: string | null
  onSelect: (traceId: string) => void
  showNavigation?: boolean
  showCount?: boolean
  label?: string
  compact?: boolean
}

const STATUS_DOT: Record<TraceStatus, string> = {
  COMPLETED: '#10B981',
  RUNNING: '#F59E0B',
  FAILED: '#EF4444',
}

function relativeTime(date: string): string {
  const diff = Date.now() - new Date(date).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}h ago`
  const days = Math.floor(hrs / 24)
  if (days < 30) return `${days}d ago`
  return new Date(date).toLocaleDateString()
}

export default function TraceSelector({
  traces,
  selectedTraceId,
  onSelect,
  showNavigation = true,
  showCount = true,
  label,
  compact = false,
}: Props) {
  const [open, setOpen] = useState(false)
  const [search, setSearch] = useState('')
  const dropdownRef = useRef<HTMLDivElement>(null)
  const searchRef = useRef<HTMLInputElement>(null)

  const selectedIndex = traces.findIndex((t) => t.id === selectedTraceId)
  const selectedTrace = selectedIndex >= 0 ? traces[selectedIndex] : null

  const filtered = search.trim()
    ? traces.filter((t) => t.id.toLowerCase().includes(search.toLowerCase().trim()))
    : traces

  const handleOpen = () => {
    setOpen(true)
    setSearch('')
    setTimeout(() => searchRef.current?.focus(), 50)
  }

  const handleSelect = (id: string) => {
    onSelect(id)
    setOpen(false)
    setSearch('')
  }

  const handlePrev = (e: React.MouseEvent) => {
    e.stopPropagation()
    if (selectedIndex > 0) {
      onSelect(traces[selectedIndex - 1].id)
    }
  }

  const handleNext = (e: React.MouseEvent) => {
    e.stopPropagation()
    if (selectedIndex < traces.length - 1) {
      onSelect(traces[selectedIndex + 1].id)
    }
  }

  // Close on outside click
  const handleOutside = useCallback((e: MouseEvent) => {
    if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
      setOpen(false)
      setSearch('')
    }
  }, [])

  useEffect(() => {
    if (open) {
      document.addEventListener('mousedown', handleOutside)
    }
    return () => document.removeEventListener('mousedown', handleOutside)
  }, [open, handleOutside])

  const fontSize = compact ? 11 : 12
  const padding = compact ? '4px 8px' : '6px 10px'

  return (
    <div
      ref={dropdownRef}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: compact ? 4 : 6,
        position: 'relative',
      }}
    >
      {label && (
        <span style={{
          fontSize: compact ? 10 : 11,
          color: 'var(--text-tertiary)',
          textTransform: 'uppercase',
          letterSpacing: '0.06em',
          whiteSpace: 'nowrap',
        }}>
          {label}
        </span>
      )}

      {/* Prev button */}
      {showNavigation && (
        <button
          onClick={handlePrev}
          disabled={selectedIndex <= 0}
          title="Previous trace"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: compact ? 22 : 26,
            height: compact ? 22 : 26,
            borderRadius: 5,
            border: '1px solid var(--border)',
            background: 'var(--surface-2)',
            color: selectedIndex <= 0 ? 'var(--text-tertiary)' : 'var(--text-secondary)',
            cursor: selectedIndex <= 0 ? 'not-allowed' : 'pointer',
            opacity: selectedIndex <= 0 ? 0.4 : 1,
            flexShrink: 0,
          }}
        >
          <ChevronLeft size={compact ? 11 : 13} strokeWidth={2} />
        </button>
      )}

      {/* Dropdown trigger */}
      <button
        onClick={handleOpen}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 6,
          padding,
          borderRadius: 6,
          border: open
            ? '1px solid var(--border-focus)'
            : '1px solid var(--border)',
          background: open ? 'rgba(99,102,241,0.08)' : 'var(--surface-2)',
          color: 'var(--text-primary)',
          cursor: 'pointer',
          fontSize,
          fontFamily: selectedTrace ? '"SF Mono", "Fira Code", monospace' : 'inherit',
          minWidth: compact ? 160 : 200,
          maxWidth: compact ? 220 : 280,
          transition: 'all 0.15s ease',
          position: 'relative',
        }}
      >
        {selectedTrace ? (
          <>
            <span style={{
              width: 6,
              height: 6,
              borderRadius: '50%',
              background: STATUS_DOT[selectedTrace.status],
              flexShrink: 0,
            }} />
            <span style={{ flex: 1, textAlign: 'left', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {selectedTrace.id.slice(0, 8)}
            </span>
            {selectedTrace.totalCost != null && (
              <span style={{ color: 'var(--text-tertiary)', fontSize: compact ? 10 : 11, flexShrink: 0 }}>
                ${selectedTrace.totalCost.toFixed(4)}
              </span>
            )}
          </>
        ) : (
          <span style={{ color: 'var(--text-tertiary)', fontSize, flex: 1, textAlign: 'left' }}>
            Select a trace…
          </span>
        )}
        <ChevronDown
          size={compact ? 11 : 13}
          strokeWidth={1.5}
          style={{
            flexShrink: 0,
            color: 'var(--text-tertiary)',
            transform: open ? 'rotate(180deg)' : 'none',
            transition: 'transform 0.15s ease',
          }}
        />
      </button>

      {/* Next button */}
      {showNavigation && (
        <button
          onClick={handleNext}
          disabled={selectedIndex < 0 || selectedIndex >= traces.length - 1}
          title="Next trace"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: compact ? 22 : 26,
            height: compact ? 22 : 26,
            borderRadius: 5,
            border: '1px solid var(--border)',
            background: 'var(--surface-2)',
            color: (selectedIndex < 0 || selectedIndex >= traces.length - 1) ? 'var(--text-tertiary)' : 'var(--text-secondary)',
            cursor: (selectedIndex < 0 || selectedIndex >= traces.length - 1) ? 'not-allowed' : 'pointer',
            opacity: (selectedIndex < 0 || selectedIndex >= traces.length - 1) ? 0.4 : 1,
            flexShrink: 0,
          }}
        >
          <ChevronRight size={compact ? 11 : 13} strokeWidth={2} />
        </button>
      )}

      {/* Count label */}
      {showCount && traces.length > 0 && (
        <span style={{
          fontSize: compact ? 10 : 11,
          color: 'var(--text-tertiary)',
          whiteSpace: 'nowrap',
          flexShrink: 0,
        }}>
          {selectedIndex >= 0 ? `Trace ${selectedIndex + 1} of ${traces.length}` : `${traces.length} traces`}
        </span>
      )}

      {/* Dropdown panel */}
      {open && (
        <div
          style={{
            position: 'absolute',
            top: '100%',
            left: 0,
            zIndex: 50,
            marginTop: 4,
            minWidth: compact ? 240 : 300,
            maxWidth: 360,
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 8,
            boxShadow: 'var(--shadow-lg)',
            overflow: 'hidden',
            animation: 'traceSelectorOpen 0.12s ease',
          }}
        >
          {/* Search input */}
          <div style={{
            padding: '8px',
            borderBottom: '1px solid var(--border)',
            display: 'flex',
            alignItems: 'center',
            gap: 6,
          }}>
            <Search size={12} strokeWidth={1.5} style={{ color: 'var(--text-tertiary)', flexShrink: 0 }} />
            <input
              ref={searchRef}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Filter by trace ID…"
              style={{
                flex: 1,
                background: 'transparent',
                border: 'none',
                outline: 'none',
                color: 'var(--text-primary)',
                fontSize: 12,
                fontFamily: '"SF Mono", "Fira Code", monospace',
              }}
            />
          </div>

          {/* Options list */}
          <div style={{ maxHeight: 300, overflowY: 'auto' }}>
            {filtered.length === 0 ? (
              <div style={{ padding: '16px', textAlign: 'center', color: 'var(--text-tertiary)', fontSize: 12 }}>
                No traces match
              </div>
            ) : (
              filtered.map((trace) => {
                const isActive = trace.id === selectedTraceId
                return (
                  <button
                    key={trace.id}
                    onClick={() => handleSelect(trace.id)}
                    style={{
                      width: '100%',
                      display: 'flex',
                      alignItems: 'center',
                      gap: 8,
                      padding: '8px 12px',
                      background: isActive ? 'rgba(99,102,241,0.12)' : 'transparent',
                      border: 'none',
                      borderLeft: isActive ? '2px solid var(--accent)' : '2px solid transparent',
                      cursor: 'pointer',
                      textAlign: 'left',
                      transition: 'background 0.1s ease',
                    }}
                    onMouseEnter={(e) => {
                      if (!isActive) (e.currentTarget as HTMLButtonElement).style.background = 'var(--surface-2)'
                    }}
                    onMouseLeave={(e) => {
                      if (!isActive) (e.currentTarget as HTMLButtonElement).style.background = 'transparent'
                    }}
                  >
                    <span style={{
                      width: 6,
                      height: 6,
                      borderRadius: '50%',
                      background: STATUS_DOT[trace.status],
                      flexShrink: 0,
                    }} />
                    <span style={{
                      fontSize: 12,
                      fontFamily: '"SF Mono", "Fira Code", monospace',
                      color: isActive ? 'var(--accent-hover)' : 'var(--text-primary)',
                      flex: 1,
                    }}>
                      {trace.id.slice(0, 8)}
                    </span>
                    {trace.totalCost != null && (
                      <span style={{ fontSize: 11, color: 'var(--text-secondary)', flexShrink: 0 }}>
                        ${trace.totalCost.toFixed(4)}
                      </span>
                    )}
                    <span style={{ fontSize: 10, color: 'var(--text-tertiary)', flexShrink: 0 }}>
                      {relativeTime(trace.startedAt)}
                    </span>
                  </button>
                )
              })
            )}
          </div>
        </div>
      )}

      <style>{`
        @keyframes traceSelectorOpen {
          from { opacity: 0; transform: translateY(-4px); }
          to   { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </div>
  )
}
