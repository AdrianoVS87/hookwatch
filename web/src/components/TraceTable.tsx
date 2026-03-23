import { useState } from 'react'
import { motion } from 'framer-motion'
import { ArrowUp, ArrowDown } from 'lucide-react'
import type { Trace, TraceStatus } from '../types'

interface Props { traces: Trace[]; onSelect: (id: string) => void }
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

export default function TraceTable({ traces, onSelect }: Props) {
  const [sortKey, setSortKey] = useState<SortKey>('startedAt')
  const [sortAsc, setSortAsc] = useState(false)

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
    <div style={{ overflow: 'auto' }}>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ borderBottom: '1px solid var(--border)' }}>
            <Col label="Status" col="status" width={120} />
            <StaticCol label="Spans" width={70} />
            <Col label="Tokens" col="totalTokens" width={100} />
            <Col label="Cost" col="totalCost" width={100} />
            <StaticCol label="Duration" width={90} />
            <Col label="Started" col="startedAt" />
          </tr>
        </thead>
        <tbody>
          {sorted.map((trace, i) => {
            const sc = STATUS_CONFIG[trace.status]
            return (
              <motion.tr
                key={trace.id}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: i * 0.02, duration: 0.15 }}
                onClick={() => onSelect(trace.id)}
                style={{ borderBottom: '1px solid var(--border)', cursor: 'pointer' }}
                onMouseEnter={(e) => { (e.currentTarget as HTMLTableRowElement).style.background = 'var(--surface-2)' }}
                onMouseLeave={(e) => { (e.currentTarget as HTMLTableRowElement).style.background = 'transparent' }}
              >
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
