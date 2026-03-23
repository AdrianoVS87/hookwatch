import { useState } from 'react'
import { ArrowLeft, GitBranch } from 'lucide-react'
import { useTraceStore } from '../stores/useTraceStore'
import TraceCanvas from '../components/TraceCanvas'
import SpanDetail from '../components/SpanDetail'
import type { Span } from '../types'

const STATUS_COLOR: Record<string, string> = {
  COMPLETED: '#10B981', RUNNING: '#6366F1', FAILED: '#EF4444',
}

export default function TraceView() {
  const { selectedTrace, clearTrace } = useTraceStore()
  const [activeSpan, setActiveSpan] = useState<Span | null>(null)

  if (!selectedTrace) {
    return (
      <div style={{ padding: '40px 48px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
          <GitBranch size={18} strokeWidth={1.5} style={{ color: 'var(--accent)' }} />
          <h2 style={{ fontSize: 20, fontWeight: 600, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>Traces</h2>
        </div>
        <p style={{ color: 'var(--text-tertiary)', fontSize: 13 }}>Select a trace from the Dashboard to inspect its span graph.</p>
      </div>
    )
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <header style={{
        display: 'flex', alignItems: 'center', gap: 12,
        padding: '12px 20px', borderBottom: '1px solid var(--border)',
        flexShrink: 0,
      }}>
        <button
          onClick={() => { clearTrace(); setActiveSpan(null) }}
          style={{
            background: 'var(--surface-2)', border: '1px solid var(--border)',
            borderRadius: 6, padding: '5px 8px', cursor: 'pointer',
            color: 'var(--text-secondary)', display: 'flex', alignItems: 'center',
          }}
        >
          <ArrowLeft size={13} strokeWidth={1.5} />
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)', fontFamily: '"SF Mono", monospace' }}>
            {selectedTrace.id.slice(0, 8)}…
          </span>
          <span style={{
            fontSize: 11, fontWeight: 500,
            color: STATUS_COLOR[selectedTrace.status],
            background: `${STATUS_COLOR[selectedTrace.status]}18`,
            padding: '2px 8px', borderRadius: 4,
          }}>
            {selectedTrace.status}
          </span>
        </div>

        <div style={{ marginLeft: 'auto', display: 'flex', gap: 16 }}>
          {selectedTrace.totalTokens != null && (
            <Stat label="Tokens" value={selectedTrace.totalTokens.toLocaleString()} />
          )}
          {selectedTrace.totalCost != null && (
            <Stat label="Cost" value={`$${selectedTrace.totalCost.toFixed(4)}`} />
          )}
          <Stat label="Spans" value={String(selectedTrace.spans.length)} />
        </div>
      </header>

      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        <div style={{ flex: 1 }}>
          {selectedTrace.spans.length === 0 ? (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'var(--text-tertiary)', fontSize: 13 }}>
              No spans in this trace
            </div>
          ) : (
            <TraceCanvas spans={selectedTrace.spans} onNodeClick={(id) => {
              setActiveSpan(selectedTrace.spans.find(s => s.id === id) ?? null)
            }} />
          )}
        </div>
        {activeSpan && (
          <SpanDetail span={activeSpan} onClose={() => setActiveSpan(null)} />
        )}
      </div>
    </div>
  )
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div style={{ fontSize: 10, color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{label}</div>
      <div style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)', fontVariantNumeric: 'tabular-nums' }}>{value}</div>
    </div>
  )
}
