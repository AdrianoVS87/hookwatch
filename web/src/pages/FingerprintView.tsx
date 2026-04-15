import { Fragment, useEffect, useMemo, useState, useCallback } from 'react'
import { AlertTriangle, ChevronDown, ChevronUp } from 'lucide-react'
import { useAgentStore } from '../stores/useAgentStore'
import { fetchFingerprints, fetchFingerprintTrend, type FingerprintSummary, type FingerprintTrendPoint } from '../api/fingerprints'
import ErrorState from '../components/ErrorState'

function last30DaysRange() {
  const to = new Date()
  const from = new Date()
  from.setDate(to.getDate() - 30)
  const fmt = (d: Date) => d.toISOString().slice(0, 10)
  return { from: fmt(from), to: fmt(to) }
}

function Sparkline({ points }: { points: FingerprintTrendPoint[] }) {
  const width = 140
  const height = 28
  const path = useMemo(() => {
    // Render a flat baseline when no data yet
    if (!points.length) return `2,${height - 2} ${width - 2},${height - 2}`
    const max = Math.max(...points.map((p) => p.count), 1)
    return points.map((p, i) => {
      const x = (i / Math.max(points.length - 1, 1)) * (width - 4) + 2
      const y = height - (p.count / max) * (height - 4) - 2
      return `${x},${y}`
    }).join(' ')
  }, [points])

  return (
    <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`} data-testid="sparkline">
      <polyline
        points={path}
        fill="none"
        stroke={points.length ? 'var(--accent)' : 'var(--border)'}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

export default function FingerprintView() {
  const { selectedAgentId, agents } = useAgentStore()
  const [rows, setRows] = useState<FingerprintSummary[]>([])
  const [trends, setTrends] = useState<Record<string, FingerprintTrendPoint[]>>({})
  const [expandedId, setExpandedId] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(false)

  const agentId = selectedAgentId ?? agents[0]?.id ?? null

  const loadData = useCallback((id: string) => {
    const { from, to } = last30DaysRange()
    setLoading(true)
    setError(false)
    fetchFingerprints(id)
      .then(async (items) => {
        setRows(items)
        if (items.length === 0) return
        const trendEntries = await Promise.all(items.slice(0, 20).map(async (fp) => {
          try {
            const trend = await fetchFingerprintTrend(fp.id, from, to)
            return [fp.id, trend.trend] as const
          } catch {
            return [fp.id, [] as FingerprintTrendPoint[]] as const
          }
        }))
        setTrends(Object.fromEntries(trendEntries))
      })
      .catch(() => { setRows([]); setError(true) })
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (!agentId) {
      setLoading(false)
      return
    }
    loadData(agentId)
  }, [agentId, loadData])

  return (
    <div className="page-padding" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <AlertTriangle size={18} color="#F59E0B" />
        <h2 style={{ margin: 0, fontSize: 20, fontWeight: 600 }}>Failure Fingerprints</h2>
      </div>

      {loading && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          {[...Array(4)].map((_, i) => (
            <div key={i} className="skeleton" style={{ height: 48, borderRadius: 'var(--radius-md)' }} />
          ))}
        </div>
      )}

      {!loading && error && (
        <ErrorState message="Failed to load fingerprints" onRetry={() => agentId && loadData(agentId)} />
      )}

      {!loading && !error && <div className="table-responsive" style={{ border: '1px solid var(--border)', borderRadius: 10, overflow: 'hidden', background: 'var(--surface)' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border)' }}>
              <th style={{ padding: '10px 12px', textAlign: 'left' }}>Fingerprint</th>
              <th style={{ padding: '10px 12px', textAlign: 'left' }}>Type</th>
              <th style={{ padding: '10px 12px', textAlign: 'left' }}>Model</th>
              <th style={{ padding: '10px 12px', textAlign: 'right' }}>Occurrences</th>
              <th style={{ padding: '10px 12px', textAlign: 'left' }}>Trend (30d)</th>
              <th style={{ padding: '10px 12px', textAlign: 'center', width: 70 }}>Details</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => {
              const isOpen = expandedId === r.id
              return (
                <Fragment key={r.id}>
                  <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.04)' }}>
                    <td style={{ padding: '10px 12px', fontFamily: 'monospace', color: 'var(--text-primary)' }}>{r.hash.slice(0, 10)}…</td>
                    <td style={{ padding: '10px 12px' }}>{r.spanType}</td>
                    <td style={{ padding: '10px 12px' }}>{r.model ?? '—'}</td>
                    <td style={{ padding: '10px 12px', textAlign: 'right', fontWeight: 600 }}>{r.occurrenceCount}</td>
                    <td style={{ padding: '10px 12px' }}><Sparkline points={trends[r.id] ?? []} /></td>
                    <td style={{ padding: '10px 12px', textAlign: 'center' }}>
                      <button
                        onClick={() => setExpandedId((prev) => prev === r.id ? null : r.id)}
                        style={{ border: 'none', background: 'transparent', color: 'var(--text-secondary)', cursor: 'pointer' }}
                      >
                        {isOpen ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                      </button>
                    </td>
                  </tr>
                  {isOpen && (
                    <tr>
                      <td colSpan={6} style={{ padding: '10px 12px', background: 'var(--surface-2)' }}>
                        <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginBottom: 6 }}>
                          <strong style={{ color: 'var(--text-primary)' }}>Full error:</strong> {r.errorMessage}
                        </div>
                        <div style={{ fontSize: 11, color: 'var(--text-tertiary)' }}>
                          firstSeen={new Date(r.firstSeenAt).toLocaleString()} · lastSeen={new Date(r.lastSeenAt).toLocaleString()}
                        </div>
                      </td>
                    </tr>
                  )}
                </Fragment>
              )
            })}
            {!loading && rows.length === 0 && (
              <tr><td colSpan={6} style={{ padding: '14px 12px', color: 'var(--text-tertiary)' }}>No recurring failures yet.</td></tr>
            )}
          </tbody>
        </table>
      </div>}
    </div>
  )
}
