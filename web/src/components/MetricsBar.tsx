import { useAgentStore } from '../stores/useAgentStore'

export default function MetricsBar() {
  const metrics = useAgentStore((s) => s.metrics)
  if (!metrics) return null

  const items = [
    { label: 'Traces',       value: String(metrics.totalTraces) },
    { label: 'Avg Tokens',   value: Math.round(metrics.avgTokens).toLocaleString() },
    { label: 'Avg Cost',     value: `$${metrics.avgCost.toFixed(4)}` },
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
          <div style={{ fontSize: 11, color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: 4 }}>
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
