import { useAgentStore } from '../stores/useAgentStore'

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

export default function MetricsBar() {
  const metrics = useAgentStore((s) => s.metrics)
  if (!metrics) return null

  const items = [
    { label: 'Traces',       value: fmtNum(metrics.totalTraces) },
    { label: 'Avg Tokens',   value: fmtNum(Math.round(metrics.avgTokens)) },
    { label: 'Avg Cost',     value: fmtCost(metrics.avgCost) },
    { label: 'Success Rate', value: `${metrics.successRate.toFixed(1)}%` },
    { label: 'p95 Latency',  value: metrics.p95LatencyMs > 0 ? `${metrics.p95LatencyMs}ms` : '—' },
  ]

  return (
    <div style={{
      display: 'flex', gap: 0,
      borderBottom: '1px solid var(--border)',
      background: 'var(--surface)',
    }}>
      {items.map((item, i) => (
        <div key={i} style={{
          flex: 1,
          padding: '12px 20px',
          borderRight: i < items.length - 1 ? '1px solid var(--border)' : 'none',
        }}>
          <div style={{ fontSize: 11, color: 'rgba(255,255,255,0.55)', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: 4 }}>
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
