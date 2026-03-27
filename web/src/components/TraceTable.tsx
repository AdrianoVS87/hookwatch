import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { ArrowUp, ArrowDown, GitCompareArrows, Plus } from 'lucide-react'
import type { Trace, TraceStatus, Score } from '../types'
import { fetchTraceScores } from '../api/traces'
import { useCompareStore } from '../stores/useCompareStore'

interface Props {
  traces: Trace[]
  onSelect: (id: string) => void
  onCompare?: () => void
  totalElements?: number
  onTagClick?: (tag: string) => void
  onAddTags?: (traceId: string, tags: string[]) => Promise<void> | void
  onDeleteTag?: (traceId: string, tag: string) => Promise<void> | void
}

type SortKey = 'status' | 'totalTokens' | 'totalCost' | 'startedAt'

const STATUS_CONFIG: Record<TraceStatus, { label: string; color: string; bg: string; dot: string }> = {
  COMPLETED: { label: 'Completed', color: 'var(--text-secondary)', bg: 'rgba(255,255,255,0.06)', dot: '#8B95A1' },
  RUNNING:   { label: 'Running',   color: '#F59E0B', bg: 'rgba(245,158,11,0.1)', dot: '#F59E0B' },
  FAILED:    { label: 'Failed',    color: '#EF4444', bg: 'rgba(239,68,68,0.1)', dot: '#EF4444' },
}

function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  const mins = Math.floor(ms / 60000)
  const secs = Math.round((ms % 60000) / 1000)
  return `${mins}m ${secs}s`
}

function durationMs(trace: Trace): string {
  if (!trace.completedAt) return '—'
  const ms = new Date(trace.completedAt).getTime() - new Date(trace.startedAt).getTime()
  return formatDuration(ms)
}

function relativeTime(date: string): string {
  const now = Date.now()
  const then = new Date(date).getTime()
  const diff = now - then
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}h ago`
  const days = Math.floor(hrs / 24)
  if (days < 30) return `${days}d ago`
  return new Date(date).toLocaleDateString()
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

export default function TraceTable({ traces, onSelect, onCompare, totalElements, onTagClick, onAddTags }: Props) {
  const [sortKey, setSortKey] = useState<SortKey>('startedAt')
  const [sortAsc, setSortAsc] = useState(false)
  const [traceScores, setTraceScores] = useState<Record<string, Score[]>>({})
  const { selectedTraces, toggleTrace } = useCompareStore()
  const hasScores = traces.some(t => (traceScores[t.id] ?? []).length > 0)

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

  const handleAddTags = async (traceId: string) => {
    if (!onAddTags) return
    const input = window.prompt('Add tags (comma-separated):')
    if (!input) return
    const tags = input.split(',').map((t) => t.trim()).filter(Boolean)
    if (tags.length === 0) return
    await onAddTags(traceId, tags)
  }

  const Col = ({ label, col, width, align = 'left' }: { label: string; col: SortKey; width?: number; align?: 'left' | 'right' }) => (
    <th
      onClick={() => toggleSort(col)}
      style={{
        padding: '10px 16px', textAlign: align,
        fontSize: 11, fontWeight: 500, color: 'var(--text-tertiary)',
        textTransform: 'uppercase', letterSpacing: '0.06em',
        cursor: 'pointer', userSelect: 'none', width,
        whiteSpace: 'nowrap',
      }}
    >
      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, justifyContent: align === 'right' ? 'flex-end' : 'flex-start', width: '100%' }}>
        {label}
        {sortKey === col && (sortAsc
          ? <ArrowUp size={10} strokeWidth={2} />
          : <ArrowDown size={10} strokeWidth={2} />
        )}
      </span>
    </th>
  )

  const StaticCol = ({ label, width, align = 'left' }: { label: string; width?: number; align?: 'left' | 'right' }) => (
    <th style={{
      padding: '10px 16px', textAlign: align,
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
            <StaticCol label="Spans" width={70} align="right" />
            <Col label="Tokens" col="totalTokens" width={100} align="right" />
            <Col label="Cost" col="totalCost" width={100} align="right" />
            <StaticCol label="Tags" width={260} />
            {hasScores && <StaticCol label="Scores" width={140} />}
            <StaticCol label="Duration" width={90} align="right" />
            <Col label="Started" col="startedAt" />
          </tr>
        </thead>
        <tbody>
          {sorted.map((trace, i) => {
            const sc = STATUS_CONFIG[trace.status]
            const scores = traceScores[trace.id] ?? []
            const isSelected = selectedTraces.includes(trace.id)
            const isFailed = trace.status === 'FAILED'
            const model = (trace.metadata?.model as string) ?? null
            const rowBg = isSelected
              ? 'rgba(99,102,241,0.06)'
              : isFailed
                ? 'rgba(239,68,68,0.06)'
                : i % 2 === 0 ? 'transparent' : 'rgba(255,255,255,0.015)'
            return (
              <motion.tr
                key={trace.id}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: i * 0.02, duration: 0.15 }}
                onClick={() => onSelect(trace.id)}
                style={{
                  borderBottom: '1px solid rgba(255,255,255,0.04)', cursor: 'pointer',
                  background: rowBg,
                  borderLeft: isSelected ? '2px solid var(--accent)' : '2px solid transparent',
                }}
                onMouseEnter={(e) => { if (!isSelected) (e.currentTarget as HTMLTableRowElement).style.background = 'var(--surface-2)' }}
                onMouseLeave={(e) => { if (!isSelected) (e.currentTarget as HTMLTableRowElement).style.background = rowBg }}
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
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                    <span style={{
                      display: 'inline-flex', alignItems: 'center', gap: 6,
                      padding: '3px 8px', borderRadius: 4,
                      background: sc.bg, color: sc.color,
                      fontSize: 11, fontWeight: 500, width: 'fit-content',
                    }}>
                      <span style={{ width: 5, height: 5, borderRadius: '50%', background: sc.dot, flexShrink: 0 }} />
                      {sc.label}
                    </span>
                    {model && (
                      <span style={{ fontSize: 10, color: 'var(--text-tertiary)', paddingLeft: 8 }}>
                        {model}
                      </span>
                    )}
                  </div>
                </td>
                <td style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontSize: 13, textAlign: 'right' }}>
                  {trace.spans.length}
                </td>
                <td style={{ padding: '12px 16px', color: 'var(--text-primary)', fontSize: 13, fontVariantNumeric: 'tabular-nums', textAlign: 'right' }}>
                  {trace.totalTokens?.toLocaleString() ?? '—'}
                </td>
                <td style={{ padding: '12px 16px', color: 'var(--text-primary)', fontSize: 13, fontVariantNumeric: 'tabular-nums', textAlign: 'right' }}>
                  {trace.totalCost != null ? `$${trace.totalCost.toFixed(4)}` : '—'}
                </td>
                <td style={{ padding: '12px 16px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                    {(trace.tags ?? []).map((tag) => (
                      <button
                        key={`${trace.id}-${tag}`}
                        onClick={(e) => {
                          e.stopPropagation()
                          onTagClick?.(tag)
                        }}
                        style={{
                          fontSize: 10,
                          borderRadius: 999,
                          border: '1px solid rgba(99,102,241,0.35)',
                          background: 'rgba(99,102,241,0.10)',
                          color: 'var(--accent-hover)',
                          padding: '2px 8px',
                          cursor: 'pointer',
                        }}
                      >
                        {tag}
                      </button>
                    ))}
                    <button
                      onClick={(e) => {
                        e.stopPropagation()
                        void handleAddTags(trace.id)
                      }}
                      title="Add tags"
                      style={{
                        width: 20,
                        height: 20,
                        display: 'inline-flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        borderRadius: 999,
                        border: '1px dashed var(--border)',
                        background: 'transparent',
                        color: 'var(--text-tertiary)',
                        cursor: 'pointer',
                      }}
                    >
                      <Plus size={11} strokeWidth={2} />
                    </button>
                  </div>
                </td>
                {hasScores && (
                  <td style={{ padding: '12px 16px' }}>
                    {scores.length > 0 ? (
                      <span style={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
                        {scores.map(s => <ScoreBadge key={s.id} score={s} />)}
                      </span>
                    ) : (
                      <span style={{ color: 'var(--text-tertiary)', fontSize: 11 }}>—</span>
                    )}
                  </td>
                )}
                <td style={{ padding: '12px 16px', color: 'var(--text-secondary)', fontSize: 13, fontVariantNumeric: 'tabular-nums', textAlign: 'right' }}>
                  {durationMs(trace)}
                </td>
                <td style={{ padding: '12px 16px', color: 'var(--text-tertiary)', fontSize: 12 }} title={new Date(trace.startedAt).toLocaleString()}>
                  {relativeTime(trace.startedAt)}
                </td>
              </motion.tr>
            )
          })}
        </tbody>
      </table>
      {totalElements != null && (
        <div style={{
          padding: '10px 16px', borderTop: '1px solid rgba(255,255,255,0.04)',
          fontSize: 11, color: 'var(--text-tertiary)',
        }}>
          Showing {traces.length} of {totalElements} traces
        </div>
      )}
    </div>
  )
}
