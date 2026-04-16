import { useMemo } from 'react'
import { useAgentStore } from '../stores/useAgentStore'
import { useTraceStore } from '../stores/useTraceStore'
import type { Trace } from '../types'

function fmtNum(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}k`
  return String(n)
}

function fmtCost(n: number): string {
  if (n === 0) return '$0.00'
  if (n < 0.01) return `$${n.toFixed(4)}`
  return `$${n.toFixed(2)}`
}

interface ComputedMetrics {
  totalTraces: number
  avgTokens: number
  avgCost: number
  successRate: number
  p95LatencyMs: number
}

function computeMetrics(traces: Trace[]): ComputedMetrics {
  const n = traces.length
  if (n === 0) {
    return { totalTraces: 0, avgTokens: 0, avgCost: 0, successRate: 0, p95LatencyMs: 0 }
  }

  let tokenSum = 0
  let costSum = 0
  let completed = 0
  const latencies: number[] = []

  for (const t of traces) {
    tokenSum += t.totalTokens ?? 0
    costSum += Number(t.totalCost ?? 0)
    if (t.status === 'COMPLETED') completed += 1
    if (t.startedAt && t.completedAt) {
      const started = new Date(t.startedAt).getTime()
      const finished = new Date(t.completedAt).getTime()
      const d = finished - started
      if (!Number.isNaN(d) && d >= 0) latencies.push(d)
    }
  }

  latencies.sort((a, b) => a - b)
  const p95Index = Math.max(0, Math.ceil(latencies.length * 0.95) - 1)
  const p95 = latencies.length > 0 ? Math.round(latencies[p95Index]) : 0

  return {
    totalTraces: n,
    avgTokens: tokenSum / n,
    avgCost: costSum / n,
    successRate: (completed / n) * 100,
    p95LatencyMs: p95,
  }
}

export default function MetricsBar() {
  const serverMetrics = useAgentStore((s) => s.metrics)
  const traces = useTraceStore((s) => s.traces)
  const selectedModel = useTraceStore((s) => s.selectedModel)
  const selectedTags = useTraceStore((s) => s.selectedTags)

  const isFiltered = selectedModel !== null || selectedTags.length > 0

  const computed = useMemo(() => computeMetrics(traces), [traces])

  // Use filtered metrics when a model/tag filter is active; otherwise use server-side aggregate
  const metrics: ComputedMetrics | null = isFiltered
    ? computed
    : serverMetrics
      ? {
          totalTraces: serverMetrics.totalTraces,
          avgTokens: serverMetrics.avgTokens,
          avgCost: serverMetrics.avgCost,
          successRate: serverMetrics.successRate,
          p95LatencyMs: serverMetrics.p95LatencyMs,
        }
      : null

  if (!metrics) return null

  const items = [
    { label: 'Traces',       value: fmtNum(metrics.totalTraces) },
    { label: 'Avg Tokens',   value: fmtNum(Math.round(metrics.avgTokens)) },
    { label: 'Avg Cost',     value: fmtCost(metrics.avgCost) },
    { label: 'Success Rate', value: `${metrics.successRate.toFixed(1)}%` },
    { label: 'p95 Latency',  value: metrics.p95LatencyMs > 0 ? `${metrics.p95LatencyMs}ms` : '—' },
  ]

  return (
    <div className="metrics-bar" style={{
      display: 'flex', gap: 0, flexWrap: 'wrap',
      borderBottom: '1px solid var(--border)',
      background: 'var(--surface)',
      position: 'relative',
    }}>
      {isFiltered && (
        <div style={{
          position: 'absolute', top: 4, right: 8,
          fontSize: 10, color: 'var(--accent-hover)',
          textTransform: 'uppercase', letterSpacing: '0.08em',
          fontWeight: 600,
        }}>
          Filtered
        </div>
      )}
      {items.map((item, i) => (
        <div key={i} style={{
          flex: '1 1 auto',
          minWidth: 100,
          padding: '10px clamp(8px, 2vw, 20px)',
          borderRight: i < items.length - 1 ? '1px solid var(--border)' : 'none',
        }}>
          <div style={{ fontSize: 11, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: 4 }}>
            {item.label}
          </div>
          <div style={{ fontSize: 16, fontWeight: 600, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
            {item.value}
          </div>
        </div>
      ))}
    </div>
  )
}
