import { useEffect, useState, useCallback } from 'react'
import { ArrowLeft, GitCompareArrows } from 'lucide-react'
import { useCompareStore } from '../stores/useCompareStore'
import { useTraceStore } from '../stores/useTraceStore'
import { fetchComparison } from '../api/compare'
import TraceCanvas from '../components/TraceCanvas'
import TraceSelector from '../components/TraceSelector'
import type { TraceComparison } from '../types'

interface Props {
  onBack: () => void
}

export default function CompareView({ onBack }: Props) {
  const { selectedTraces, clearSelection } = useCompareStore()
  const { rawTraces, traces: filteredTraces } = useTraceStore()
  const allTraces = rawTraces.length > 0 ? rawTraces : filteredTraces

  // Local state for the two selected trace IDs in this view
  const [traceAId, setTraceAId] = useState<string | null>(selectedTraces[0] ?? null)
  const [traceBId, setTraceBId] = useState<string | null>(selectedTraces[1] ?? null)

  const [comparison, setComparison] = useState<TraceComparison | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const loadComparison = useCallback((idA: string, idB: string) => {
    setLoading(true)
    setError(null)
    fetchComparison(idA, idB)
      .then(setComparison)
      .catch(() => setError('Failed to load comparison'))
      .finally(() => setLoading(false))
  }, [])

  // Load from compare store on mount (dashboard checkbox flow)
  useEffect(() => {
    if (selectedTraces.length === 2) {
      setTraceAId(selectedTraces[0])
      setTraceBId(selectedTraces[1])
      loadComparison(selectedTraces[0], selectedTraces[1])
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const handleSelectA = (id: string) => {
    setTraceAId(id)
    if (traceBId && id !== traceBId) {
      loadComparison(id, traceBId)
    }
  }

  const handleSelectB = (id: string) => {
    setTraceBId(id)
    if (traceAId && id !== traceAId) {
      loadComparison(traceAId, id)
    }
  }

  const handleBack = () => {
    clearSelection()
    onBack()
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Header */}
      <header style={{
        display: 'flex', alignItems: 'center', gap: 12,
        padding: '12px 20px', borderBottom: '1px solid var(--border)', flexShrink: 0,
      }}>
        <button
          onClick={handleBack}
          style={{
            background: 'var(--surface-2)', border: '1px solid var(--border)',
            borderRadius: 6, padding: '5px 8px', cursor: 'pointer',
            color: 'var(--text-secondary)', display: 'flex', alignItems: 'center',
          }}
        >
          <ArrowLeft size={13} strokeWidth={1.5} />
        </button>
        <GitCompareArrows size={16} strokeWidth={1.5} style={{ color: 'var(--accent)' }} />
        <span style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)' }}>
          Trace Comparison
        </span>
      </header>

      {/* Dual trace selector bar */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        padding: '10px 20px',
        borderBottom: '1px solid var(--border)',
        flexShrink: 0,
        flexWrap: 'wrap',
      }}>
        <TraceSelector
          traces={allTraces}
          selectedTraceId={traceAId}
          onSelect={handleSelectA}
          showNavigation={false}
          showCount={false}
          label="Trace A"
          compact={true}
        />
        <span style={{ fontSize: 12, color: 'var(--text-tertiary)', fontWeight: 500, padding: '0 4px' }}>
          vs
        </span>
        <TraceSelector
          traces={allTraces}
          selectedTraceId={traceBId}
          onSelect={handleSelectB}
          showNavigation={false}
          showCount={false}
          label="Trace B"
          compact={true}
        />
        {!traceAId || !traceBId ? (
          <span style={{ fontSize: 11, color: 'var(--text-tertiary)', marginLeft: 8 }}>
            Select two traces above to compare
          </span>
        ) : null}
      </div>

      {/* Loading / error states */}
      {loading && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 1, color: 'var(--text-tertiary)', fontSize: 13 }}>
          Loading comparison...
        </div>
      )}

      {!loading && error && (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', flex: 1, gap: 12 }}>
          <p style={{ color: '#EF4444', fontSize: 13 }}>{error}</p>
          <button onClick={handleBack} style={{ color: 'var(--accent)', fontSize: 12, background: 'none', border: 'none', cursor: 'pointer' }}>
            Go back
          </button>
        </div>
      )}

      {!loading && !error && !comparison && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 1, color: 'var(--text-tertiary)', fontSize: 13 }}>
          Select two traces above to compare.
        </div>
      )}

      {!loading && !error && comparison && (() => {
        const { delta } = comparison
        return (
          <>
            {/* Delta summary bar */}
            <div style={{
              display: 'flex', gap: 24, padding: '10px 20px',
              borderBottom: '1px solid var(--border)', flexShrink: 0,
            }}>
              <DeltaStat label="Tokens" value={delta.tokensDiff} format="number" invertColor />
              <DeltaStat label="Cost" value={delta.costDiff} format="cost" invertColor />
              <DeltaStat label="Latency" value={delta.latencyDiffMs} format="ms" invertColor />
              <div>
                <div style={{ fontSize: 10, color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Status</div>
                <div style={{ fontSize: 13, fontWeight: 500, color: delta.statusMatch ? '#10B981' : '#EF4444' }}>
                  {delta.statusMatch ? 'Match' : 'Mismatch'}
                </div>
              </div>
              <div>
                <div style={{ fontSize: 10, color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Spans</div>
                <div style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)', fontVariantNumeric: 'tabular-nums' }}>
                  {delta.spanCountDiff >= 0 ? '+' : ''}{delta.spanCountDiff}
                </div>
              </div>
            </div>

            {/* Side-by-side canvases */}
            <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
              <div style={{ flex: 1, borderRight: '1px solid var(--border)', position: 'relative' }}>
                <div style={{ position: 'absolute', top: 8, left: 12, fontSize: 10, fontWeight: 600, color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.05em', zIndex: 5 }}>
                  Trace A — {comparison.trace1.id.slice(0, 8)}
                </div>
                {comparison.trace1.spans.length > 0 ? (
                  <TraceCanvas spans={comparison.trace1.spans} onNodeClick={() => {}} />
                ) : (
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'var(--text-tertiary)', fontSize: 13 }}>No spans</div>
                )}
              </div>
              <div style={{ flex: 1, position: 'relative' }}>
                <div style={{ position: 'absolute', top: 8, left: 12, fontSize: 10, fontWeight: 600, color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.05em', zIndex: 5 }}>
                  Trace B — {comparison.trace2.id.slice(0, 8)}
                </div>
                {comparison.trace2.spans.length > 0 ? (
                  <TraceCanvas spans={comparison.trace2.spans} onNodeClick={() => {}} />
                ) : (
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'var(--text-tertiary)', fontSize: 13 }}>No spans</div>
                )}
              </div>
            </div>
          </>
        )
      })()}
    </div>
  )
}

function DeltaStat({ label, value, format, invertColor }: {
  label: string
  value: number
  format: 'number' | 'cost' | 'ms'
  invertColor?: boolean
}) {
  // For tokens/cost/latency, negative = improvement (fewer = better)
  const isPositive = value > 0
  const isNegative = value < 0
  const color = invertColor
    ? (isNegative ? '#10B981' : isPositive ? '#EF4444' : 'var(--text-primary)')
    : (isPositive ? '#10B981' : isNegative ? '#EF4444' : 'var(--text-primary)')

  let display: string
  if (format === 'cost') {
    display = `${value >= 0 ? '+' : ''}$${value.toFixed(4)}`
  } else if (format === 'ms') {
    display = `${value >= 0 ? '+' : ''}${value}ms`
  } else {
    display = `${value >= 0 ? '+' : ''}${value.toLocaleString()}`
  }

  return (
    <div>
      <div style={{ fontSize: 10, color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{label}</div>
      <div style={{ fontSize: 13, fontWeight: 500, color, fontVariantNumeric: 'tabular-nums' }}>{display}</div>
    </div>
  )
}
