import { useAgentStore } from '../stores/useAgentStore'

export default function MetricsBar() {
  const metrics = useAgentStore((s) => s.metrics)

  if (!metrics) return null

  const fmt = (n: number, decimals = 0) => n.toFixed(decimals)

  return (
    <div className="flex gap-6 px-6 py-3 bg-slate-800 border-b border-slate-700 text-sm">
      <Metric label="Total Traces" value={String(metrics.totalTraces)} />
      <Metric label="Avg Tokens" value={fmt(metrics.avgTokens)} />
      <Metric label="Avg Cost" value={`$${fmt(metrics.avgCost, 4)}`} />
      <Metric label="Success Rate" value={`${fmt(metrics.successRate, 1)}%`} />
      <Metric label="p95 Latency" value={`${metrics.p95LatencyMs}ms`} />
    </div>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col">
      <span className="text-slate-400 text-xs uppercase tracking-wide">{label}</span>
      <span className="text-white font-semibold">{value}</span>
    </div>
  )
}
