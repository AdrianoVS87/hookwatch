import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { ArrowUp, ArrowDown, GitCompareArrows } from 'lucide-react'
import type { Trace, TraceStatus, Score } from '../types'
import { fetchTraceScores } from '../api/traces'
import { useCompareStore } from '../stores/useCompareStore'

interface Props { traces: Trace[]; onSelect: (id: string) => void; onCompare?: () => void }
type SortKey = 'status' | 'totalTokens' | 'totalCost' | 'startedAt'

const STATUS_CONFIG: Record<TraceStatus, { label: string; color: string; bg: string }> = {
  COMPLETED: { label: 'Completed', color: '#10B981', bg: 'rgba(16,185,129,0.1)' },
  RUNNING:   { label: 'Running',   color: '#6366F1', bg: 'rgba(99,102,241,0.1)' },
  FAILED:    { label: 'Failed',    color: '#EF4444', bg: 'rgba(239,68,68,0.1)' },
}

function durationMs(trace: Trace): string {
  if (!trace.completedAt) return '—'
  const ms = new Date(trace.completedAt).getTime() - new Date(trace.startedAt).getTime()
  return ms > 1000 ? `${(ms/1000).toFixed(1)}s` : `${ms}ms`
}

function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime()
  if (diff < 60000) return `${Math.round(diff/1000)}s ago`
  if (diff < 3600000) return `${Math.round(diff/60000)}m ago`
  if (diff < 86400000) return `${Math.round(diff/3600000)}h ago`
  return new Date(iso).toLocaleDateString()
}

function ScoreBadge({ score }: { score: Score }) {
  if (score.dataType === 'NUMERIC' && score.numericValue != null) {
    const v = score.numericValue
    const color = v > 0.7 ? '#10B981' : v > 0.4 ? '#F59E0B' : '#EF4444'
    return (
      <span style={{
        display: 'inline-flex', alignItems: 'center', gap: 4,
        fontSize: 10, fontWeight: 500, color, marginRight: 4,
      }}>
        <span style={{ width: 7, height: 7, borderRadius: '50%', background: color, flexShrink: 0 }} />
        {score.name}
      </span>
    )
  }
  if (score.dataType === 'BOOLEAN') {
    const ok = score.booleanValue === true
    return (
      <span style={{
        fontSize: 10, fontWeight: 500, marginRight: 4,
        color: ok ? '#10B981' : '#EF4444',
      }}>
        {ok ? '\u2713' : '\u2717'} {score.name}
      </span>
    )
  }
  if (score.dataType === 'CATEGORICAL' && score.stringValue) {
    return (
      <span style={{
        display: 'inline-block', fontSize: 9, fontWeight: 500,
        padding: '1px 5px', borderRadius: 3, marginRight: 4,
        background: 'rgba(99,102,241,0.1)', color: '#6366F1',
      }}>
        {score.name}: {score.stringValue}
      </span>
    )
  }
  return null
}

export default function TraceTable({ traces, onSelect, onCompare }: Props) {
  const [sortKey, setSortKey] = useState<SortKey>('startedAt')
  const [sortAsc, setSortAsc] = useState(false)
  const [traceScores, setTraceScores] = useState<Record<string, Score[]>>({})
  const { selectedTraces, toggleTrace } = useCompareStore()

  useEffect(() => {
    const traceIds = traces.map(t => t.id)
    traceIds.forEach(id => {
      if (!traceScores[id]) {
        fetchTraceScores(id).then(scores => {
          if (scores.length > 0) {
            setTraceScores(prev => ({ ...prev, [id]: scores }))
          }
        }).catch(() => {/* scores unavailable */})
      }
    })
  }, [traces])

  const sorted = [...traces].sort((a, b) => {
    const v = (t: Trace): string | number => {
      if (sortKey === 'status') return t.status
      if (sortKey === 'totalTokens') return t.totalTokens ?? 0
      if (sortKey === 'totalCost') return t.totalCost ?? 0
      return t.startedAt
    }
    const [av, bv] = [v(a), v(b)]
    return av < bv ? (sortAsc ? -1 : 1) : av > bv ? (sortAsc ? 1 : -1) : 0
  })

  const toggleSort = (key: SortKey) => {
    if (key === sortKey) {
      setSortAsc(p => !p)
    } else {
      setSortKey(key)
      setSortAsc(true)
    }
  }

  const Col = ({ label, col, width }: { label: string; col: SortKey; width?: number }) => (
    <th
      onClick={() => toggleSort(col)}
      style={{
        padding: '10px 16px', textAlign: 'left',
        fontSize: 11, fontWeight: 500, color: 'var(--text-tertiary)',
        textTransform: 'uppercase', letterSpacing: '0.06em',
        cursor: 'pointer', userSelect: 'none', width,
        whiteSpace: 'nowrap',
      }}
    >
      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
        {label}
        {sortKey === col && (sortAsc
          ? <ArrowUp size={10} strokeWidth={2} />
          : <ArrowDown size={10} strokeWidth={2} />
        )}
      </span>
    </th>
  )

  const StaticCol = ({ label, width }: { label: string; width?: number }) => (
    <th style={{
      padding: '10px 16px', textAlign: 'left',
      fontSize: 11, fontWeight: 500, color: 'var(--text-tertiary)',
      textTransform: 'uppercase', letterSpacing: '0.06em', width,
    }}>{label}</th>
  )

  return (
    <div style={{ overflowX: 'auto' }}>
      {selectedTraces.length === 2 && onCompare && (
        <div style={{ padding: '8px 16px', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: 8 }}>
          <button
            onClick={onCompare}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 6,
              padding: '5px 12px', borderRadius: 6, border: '1px solid rgba(99,102,241,0.4)',
              background: 'rgba(99,102,241,0.12)', color: 'var(--accent-hover)',
              fontSize: 12, fontWeight: 500, cursor: 'pointer',
            }}
          >
            <GitCompareArrows size={12} strokeWidth={1.5} />
            Compare selected
          </button>
          <span style={{ fontSize: 11, color: 'var(--text-tertiary)' }}>2 traces selected</span>
        </div>
      )}
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead style={{ position: 'sticky', top: 0, zIndex: 1, background: 'var(--surface)' }}>
          <tr style={{ borderBottom: '1px solid var(--border)' }}>
            <th style={{ padding: '10px 8px 10px 16px', width: 32 }} />
            <Col label="Status" col="status" width={120} />
            <StaticCol label="Spans" width={70} />
            <Col label="Tokens" col="totalTokens" width={100} />
            <Col label="Cost" col="totalCost" width={100} />
            <StaticCol label="Scores" width={140} />
            <StaticCol label="Duration" width={90} />
            <Col label="Started" col="startedAt" />
          </tr>
        </thead>
        <tbody>
          {sorted.map((trace, i) => {
            const sc = STATUS_CONFIG[trace.status]
            const scores = traceScores[trace.id] ?? []
            const isSelected = selectedTraces.includes(trace.id)
            return (
              <motion.tr
                key={trace.id}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: i * 0.02, duration: 0.15 }}
                onClick={() => onSelect(trace.id)}
                style={{
                  borderBottom: '1px solid var(--border)', cursor: 'pointer',
                  background: isSelected ? 'rgba(99,102,241,0.06)' : i % 2 === 0 ? 'transparent' : 'rgba(255,255,255,0.01)',
                  borderLeft: isSelected ? '2px solid var(--accent)' : '2px solid transparent',
                }}
                onMouseEnter={(e) => { if (!isSelected) (e.currentTarget as HTMLTableRowElement).style.background = 'var(--surface-2)' }}
                onMouseLeave={(e) => { if (!isSelected) (e.currentTarget as HTMLTableRowElement).style.background = i % 2 === 0 ? 'transparent' : 'rgba(255,255,255,0.01)' }}
              >
                <td style={{ padding: '12px 4px 12px 12px', width: 32 }}>
                  <input
                    type="checkbox"
                    checked={isSelected}
                    onClick={(e) => e.stopPropagation()}
                    onChange={() => toggleTrace(trace.id)}
                    style={{ cursor: 'pointer', accentColor: 'var(--accent)' }}
                  />
                </td>
                <td style={{ padding: '12px 16px' }}>
                  <span style={{
                    display: 'inline-flex', alignItems: 'center', gap: 6,
                    padding: '3px 8px', borderRadius: 4,
                    background: sc.bg, color: sc.color,
                    fontSize: 11, fontWeight: 500,
                  }}>
                    <span style={{ width: 5, height: 5, borderRadius: '50%', background: sc.color, flexShrink: 0 }} />
                    {sc.label}
                  </span>
                </td>
                <td style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontSize: 13 }}>
                  {trace.spans.length}
                </td>
                <td style={{ padding: '12px 16px', color: 'var(--text-primary)', fontSize: 13, fontVariantNumeric: 'tabular-nums' }}>
                  {trace.totalTokens?.toLocaleString() ?? '—'}
                </td>
                <td style={{ padding: '12px 16px', color: 'var(--text-primary)', fontSize: 13, fontVariantNumeric: 'tabular-nums' }}>
                  {trace.totalCost != null ? `$${trace.totalCost.toFixed(4)}` : '—'}
                </td>
                <td style={{ padding: '12px 16px' }}>
                  {scores.length > 0 ? (
                    <span style={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
                      {scores.map(s => <ScoreBadge key={s.id} score={s} />)}
                    </span>
                  ) : (
                    <span style={{ color: 'var(--text-tertiary)', fontSize: 11 }}>—</span>
                  )}
                </td>
                <td style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontSize: 13, fontVariantNumeric: 'tabular-nums' }}>
                  {durationMs(trace)}
                </td>
                <td style={{ padding: '12px 16px', color: 'var(--text-tertiary)', fontSize: 12 }}>
                  {timeAgo(trace.startedAt)}
                </td>
              </motion.tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
