import { useState, useEffect, useCallback } from 'react'
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend,
  PieChart, Pie, Cell, BarChart, Bar,
} from 'recharts'
import type { TooltipProps, PieLabelRenderProps } from 'recharts'
import { TrendingUp, TrendingDown, DollarSign, Zap, Hash, BarChart3 } from 'lucide-react'
import DateRangePicker, { presetRange, toDateStr } from '../components/DateRangePicker'
import type { DateRange } from '../components/DateRangePicker'
import { fetchAnalytics } from '../api/analytics'
import { autoEvaluateAgent } from '../api/scores'
import type { AnalyticsData, DailyUsage } from '../types'
import { useAgentStore } from '../stores/useAgentStore'
import { useTraceStore } from '../stores/useTraceStore'
import { useSettingsStore } from '../stores/useSettingsStore'


// ── helpers ──────────────────────────────────────────────────────────────────

function fmtCost(n: number): string {
  if (n === 0) return '$0.00'
  if (n < 0.001) return `$${n.toFixed(6)}`
  if (n < 0.01) return `$${n.toFixed(4)}`
  return `$${n.toFixed(2)}`
}

function fmtTokens(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}k`
  return String(n)
}

function fmtDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}

function fmtDateShort(iso: string): string {
  const d = new Date(iso)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

// ── chart theme ───────────────────────────────────────────────────────────────

const GRID_COLOR = 'rgba(255,255,255,0.06)'
const AXIS_COLOR = 'rgba(255,255,255,0.25)'
const TOOLTIP_STYLE: React.CSSProperties = {
  background: '#1A2235',
  border: '1px solid rgba(255,255,255,0.1)',
  borderRadius: 8,
  fontSize: 12,
  color: 'var(--text-primary)',
  boxShadow: '0 4px 16px rgba(0,0,0,0.5)',
  padding: '10px 14px',
}

const MODEL_COLORS: Record<string, string> = {
  'claude-opus': '#D4A574',
  'claude-sonnet': '#6366F1',
  'codex': '#412991',
  'haiku': '#10B981',
}

function modelColor(model: string): string {
  for (const key of Object.keys(MODEL_COLORS)) {
    if (model.toLowerCase().includes(key)) return MODEL_COLORS[key]
  }
  // fallback palette
  const FALLBACK = ['#6366F1', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#06B6D4']
  let hash = 0
  for (let i = 0; i < model.length; i++) hash = model.charCodeAt(i) + ((hash << 5) - hash)
  return FALLBACK[Math.abs(hash) % FALLBACK.length]
}

// ── summary card ─────────────────────────────────────────────────────────────

interface CardProps {
  label: string
  value: string
  sub?: string
  trend?: number
  icon: React.ReactNode
}

function SummaryCard({ label, value, sub, trend, icon }: CardProps) {
  return (
    <div style={{
      flex: 1,
      background: 'var(--surface)',
      border: '1px solid var(--border)',
      borderRadius: 'var(--radius-lg)',
      padding: '18px 20px',
      display: 'flex',
      flexDirection: 'column',
      gap: 8,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span style={{ fontSize: 11, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
          {label}
        </span>
        <span style={{ color: 'var(--text-tertiary)', opacity: 0.7 }}>{icon}</span>
      </div>
      <div style={{ fontSize: 22, fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
        {value}
      </div>
      {(sub || trend !== undefined) && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 4, fontSize: 11 }}>
          {trend !== undefined && (
            trend >= 0
              ? <TrendingUp size={12} color="#EF4444" />
              : <TrendingDown size={12} color="#10B981" />
          )}
          {sub && <span style={{ color: 'var(--text-tertiary)' }}>{sub}</span>}
        </div>
      )}
    </div>
  )
}

// ── chart card wrapper ────────────────────────────────────────────────────────

function ChartCard({ title, children, style }: { title: string; children: React.ReactNode; style?: React.CSSProperties }) {
  return (
    <div style={{
      background: 'var(--surface)',
      border: '1px solid var(--border)',
      borderRadius: 'var(--radius-lg)',
      padding: '20px 24px',
      ...style,
    }}>
      <h3 style={{ margin: '0 0 16px', fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>
        {title}
      </h3>
      {children}
    </div>
  )
}

// ── custom tooltip ────────────────────────────────────────────────────────────

type TipProps = TooltipProps<number, string>

function LineTooltip(props: TipProps) {
  const { active, payload, label } = props as {
    active?: boolean
    payload?: Array<{ dataKey: string; name: string; value: number; color: string }>
    label?: string
  }
  if (!active || !payload?.length) return null
  return (
    <div style={TOOLTIP_STYLE}>
      <div style={{ marginBottom: 6, color: 'var(--text-secondary)', fontSize: 11 }}>{label}</div>
      {payload.map((p) => (
        <div key={p.dataKey} style={{ display: 'flex', justifyContent: 'space-between', gap: 16, color: p.color }}>
          <span>{p.name}</span>
          <span style={{ fontWeight: 600 }}>
            {p.dataKey === 'totalCost' ? fmtCost(p.value) : fmtTokens(p.value)}
          </span>
        </div>
      ))}
    </div>
  )
}

function BarTooltip(props: TipProps) {
  const { active, payload, label } = props as {
    active?: boolean
    payload?: Array<{ dataKey: string; name: string; value: number; color: string }>
    label?: string
  }
  if (!active || !payload?.length) return null
  return (
    <div style={TOOLTIP_STYLE}>
      <div style={{ marginBottom: 6, color: 'var(--text-secondary)', fontSize: 11 }}>{label}</div>
      {payload.map((p) => (
        <div key={p.dataKey} style={{ display: 'flex', justifyContent: 'space-between', gap: 16, color: p.color }}>
          <span>{p.name}</span>
          <span style={{ fontWeight: 600 }}>{(p.value * 100).toFixed(1)}%</span>
        </div>
      ))}
    </div>
  )
}

// ── mock data (used while endpoint is not yet available) ──────────────────────

function buildMockData(from: string, to: string): AnalyticsData {
  const days: DailyUsage[] = []
  const start = new Date(from)
  const end = new Date(to)
  for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
    const rand = () => Math.random()
    days.push({
      date: toDateStr(new Date(d)),
      totalTokens: Math.floor(8000 + rand() * 40000),
      totalCost: parseFloat((0.01 + rand() * 0.8).toFixed(4)),
      traceCount: Math.floor(5 + rand() * 50),
      avgLatencyMs: Math.floor(400 + rand() * 2000),
      errorRate: parseFloat((rand() * 0.15).toFixed(3)),
    })
  }
  const byModel = [
    { model: 'claude-opus-4', totalTokens: 120000, totalCost: 3.6, traceCount: 40 },
    { model: 'claude-sonnet-4', totalTokens: 480000, totalCost: 2.4, traceCount: 200 },
    { model: 'codex-5', totalTokens: 60000, totalCost: 0.9, traceCount: 30 },
    { model: 'haiku-4', totalTokens: 200000, totalCost: 0.4, traceCount: 80 },
  ]
  const topExpensiveTraces = Array.from({ length: 10 }, (_, i) => ({
    traceId: `trace-${Math.random().toString(36).slice(2, 10)}`,
    totalCost: parseFloat((0.5 - i * 0.04 + Math.random() * 0.05).toFixed(4)),
    totalTokens: Math.floor(50000 - i * 3000 + Math.random() * 2000),
    startedAt: new Date(Date.now() - i * 3600000).toISOString(),
  }))
  return {
    dailyUsage: days,
    byModel,
    topExpensiveTraces,
    costTrend: { percentChangeVsPreviousPeriod: 12.4, projectedMonthlyCost: 18.5 },
    memoryLineage: topExpensiveTraces.slice(0, 5).map((t, i) => ({
      traceId: t.traceId,
      retrievalSpanCount: 4 - Math.min(i, 3),
      status: i % 2 === 0 ? 'COMPLETED' : 'FAILED',
      startedAt: t.startedAt,
    })),
    learningVelocity: {
      costPerSuccessfulTrace: 0.018,
      repeatFailureRate: 0.34,
      memoryHitRate: 0.61,
      meanRecoveryMinutes: 18.4,
    },
    learningVelocityByModel: [
      { model: 'claude-sonnet-4-6', successRate: 0.92, avgLatencyMs: 980, avgCost: 0.024, memoryHitRate: 0.74 },
      { model: 'openai-codex/gpt-5.3-codex', successRate: 0.89, avgLatencyMs: 840, avgCost: 0.018, memoryHitRate: 0.58 },
      { model: 'claude-opus-4-6', successRate: 0.95, avgLatencyMs: 1450, avgCost: 0.052, memoryHitRate: 0.77 },
    ],
    failureFingerprints: [
      { fingerprint: 'TOOL_TIMEOUT', count: 7, share: 0.39 },
      { fingerprint: 'RATE_LIMIT', count: 5, share: 0.28 },
      { fingerprint: 'CONTEXT_OVERFLOW', count: 3, share: 0.17 },
      { fingerprint: 'FAILED_UNKNOWN', count: 3, share: 0.17 },
    ],
    otelCompliance: { totalTraces: 42, compliantTraces: 35, complianceRate: 35 / 42 },
    evalLoopSummary: {
      totalTraces: 42,
      evaluatedTraces: 18,
      evaluationCoverage: 18 / 42,
      avgAutoQualityScore: 0.82,
    },
  }
}

// ── main view ─────────────────────────────────────────────────────────────────

export default function AnalyticsView() {
  const { agents, selectedAgentId } = useAgentStore()
  const { selectTrace, selectedModel } = useTraceStore()
  const analyticsDefaultRange = useSettingsStore((s) => s.settings.analyticsDefaultRange)

  const defaultRange = (): DateRange => {
    const r = presetRange(analyticsDefaultRange)
    return { ...r, preset: analyticsDefaultRange }
  }

  const [range, setRange] = useState<DateRange>(defaultRange)
  const [data, setData] = useState<AnalyticsData | null>(null)
  const [loading, setLoading] = useState(false)
  const [usingMock, setUsingMock] = useState(false)
  const [autoEvalRunning, setAutoEvalRunning] = useState(false)

  const load = useCallback(async (agentId: string, r: DateRange, model?: string | null) => {
    setLoading(true)
    try {
      const result = await fetchAnalytics(agentId, r.from, r.to, 'day', model)
      setData(result)
      setUsingMock(false)
    } catch {
      // endpoint not ready yet — use mock data
      setData(buildMockData(r.from, r.to))
      setUsingMock(true)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (selectedAgentId) {
      load(selectedAgentId, range, selectedModel)
    } else if (agents.length > 0) {
      load(agents[0].id, range, selectedModel)
    }
  }, [selectedAgentId, range, load, agents, selectedModel])

  const handleRangeChange = (r: DateRange) => {
    setRange(r)
  }

  const runAutoEval = async () => {
    const targetAgent = selectedAgentId ?? agents[0]?.id
    if (!targetAgent) return
    setAutoEvalRunning(true)
    try {
      await autoEvaluateAgent(targetAgent, 50)
      await load(targetAgent, range, selectedModel)
    } finally {
      setAutoEvalRunning(false)
    }
  }

  // ── derived metrics ──────────────────────────────────────────────────────────

  const totalCost = data?.dailyUsage.reduce((s, d) => s + d.totalCost, 0) ?? 0
  const totalTokens = data?.dailyUsage.reduce((s, d) => s + d.totalTokens, 0) ?? 0
  const totalTraces = data?.dailyUsage.reduce((s, d) => s + d.traceCount, 0) ?? 0
  const avgCostPerTrace = totalTraces > 0 ? totalCost / totalTraces : 0
  const projectedMonthly = data?.costTrend.projectedMonthlyCost ?? 0
  const pctChange = data?.costTrend.percentChangeVsPreviousPeriod ?? 0

  // bar chart data: success vs error rate by day
  const barData = (data?.dailyUsage ?? []).map((d) => ({
    date: fmtDateShort(d.date),
    errorRate: d.errorRate,
    successRate: 1 - d.errorRate,
  }))

  return (
    <div style={{ padding: '28px 40px', display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <BarChart3 size={18} color="var(--accent)" strokeWidth={1.5} />
          <h1 style={{ margin: 0, fontSize: 20, fontWeight: 600, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
            Cost Analytics
          </h1>
          {usingMock && (
            <span style={{
              fontSize: 10, fontWeight: 600, padding: '2px 7px', borderRadius: 4,
              background: 'rgba(245,158,11,0.1)', color: '#F59E0B',
              border: '1px solid rgba(245,158,11,0.2)', letterSpacing: '0.06em',
              textTransform: 'uppercase',
            }}>
              Demo data
            </span>
          )}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <button
            onClick={() => void runAutoEval()}
            disabled={autoEvalRunning}
            style={{
              border: '1px solid rgba(34,211,238,0.35)',
              background: 'rgba(34,211,238,0.12)',
              color: '#22d3ee',
              borderRadius: 8,
              fontSize: 12,
              fontWeight: 600,
              padding: '8px 12px',
              cursor: 'pointer',
              opacity: autoEvalRunning ? 0.7 : 1,
            }}
          >
            {autoEvalRunning ? 'Running eval…' : 'Run Auto Eval Loop'}
          </button>
          <DateRangePicker value={range} onChange={handleRangeChange} />
        </div>
      </div>

      {loading && (
        <div style={{ color: 'var(--text-tertiary)', fontSize: 13, padding: '40px 0', textAlign: 'center' }}>
          Loading analytics…
        </div>
      )}

      {!loading && data && (
        <>
          {/* Summary cards */}
          <div style={{ display: 'flex', gap: 14, flexWrap: 'wrap' }}>
            <SummaryCard
              label="Total Cost"
              value={fmtCost(totalCost)}
              icon={<DollarSign size={14} />}
            />
            <SummaryCard
              label="Projected Monthly"
              value={fmtCost(projectedMonthly)}
              sub={`${pctChange >= 0 ? '+' : ''}${pctChange.toFixed(1)}% vs last period`}
              trend={pctChange}
              icon={<TrendingUp size={14} />}
            />
            <SummaryCard
              label="Total Tokens"
              value={fmtTokens(totalTokens)}
              icon={<Hash size={14} />}
            />
            <SummaryCard
              label="Avg Cost / Trace"
              value={fmtCost(avgCostPerTrace)}
              icon={<Zap size={14} />}
            />
          </div>

          {/* Theta-style operational KPIs */}
          <div style={{ display: 'flex', gap: 14, flexWrap: 'wrap' }}>
            <SummaryCard
              label="Eval Coverage"
              value={`${(data.evalLoopSummary.evaluationCoverage * 100).toFixed(0)}%`}
              sub={`${data.evalLoopSummary.evaluatedTraces}/${data.evalLoopSummary.totalTraces} traces scored`}
              icon={<BarChart3 size={14} />}
            />
            <SummaryCard
              label="OTel GenAI Compliance"
              value={`${(data.otelCompliance.complianceRate * 100).toFixed(0)}%`}
              sub={`${data.otelCompliance.compliantTraces}/${data.otelCompliance.totalTraces} traces compliant`}
              icon={<Zap size={14} />}
            />
            <SummaryCard
              label="Memory Hit Rate"
              value={`${(data.learningVelocity.memoryHitRate * 100).toFixed(0)}%`}
              sub={`Recovery ${data.learningVelocity.meanRecoveryMinutes.toFixed(1)} min`}
              icon={<TrendingUp size={14} />}
            />
            <SummaryCard
              label="Repeat Failure Rate"
              value={`${(data.learningVelocity.repeatFailureRate * 100).toFixed(0)}%`}
              sub="Fingerprint recurrence"
              icon={<TrendingDown size={14} />}
            />
          </div>

          {/* Line chart: tokens + cost over time */}
          <ChartCard title="Daily Usage — Tokens & Cost">
            <ResponsiveContainer width="100%" height={220}>
              <LineChart data={data.dailyUsage} margin={{ top: 4, right: 16, bottom: 0, left: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke={GRID_COLOR} />
                <XAxis
                  dataKey="date"
                  tickFormatter={fmtDateShort}
                  tick={{ fontSize: 11, fill: AXIS_COLOR }}
                  axisLine={false}
                  tickLine={false}
                />
                <YAxis
                  yAxisId="tokens"
                  tickFormatter={(v) => fmtTokens(v)}
                  tick={{ fontSize: 11, fill: AXIS_COLOR }}
                  axisLine={false}
                  tickLine={false}
                  width={48}
                />
                <YAxis
                  yAxisId="cost"
                  orientation="right"
                  tickFormatter={(v) => `$${v.toFixed(2)}`}
                  tick={{ fontSize: 11, fill: AXIS_COLOR }}
                  axisLine={false}
                  tickLine={false}
                  width={52}
                />
                <Tooltip content={<LineTooltip />} />
                <Legend
                  wrapperStyle={{ fontSize: 11, color: AXIS_COLOR, paddingTop: 8 }}
                />
                <Line
                  yAxisId="tokens"
                  type="monotone"
                  dataKey="totalTokens"
                  name="Tokens"
                  stroke="#6366F1"
                  strokeWidth={2}
                  dot={false}
                  activeDot={{ r: 4 }}
                />
                <Line
                  yAxisId="cost"
                  type="monotone"
                  dataKey="totalCost"
                  name="Cost ($)"
                  stroke="#F59E0B"
                  strokeWidth={2}
                  dot={false}
                  activeDot={{ r: 4 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </ChartCard>

          {/* Pie + Bar side by side */}
          <div style={{ display: 'flex', gap: 14, flexWrap: 'wrap' }}>
            {/* Pie chart: cost by model */}
            <ChartCard title="Cost by Model" style={{ flex: 1, minWidth: 280 }}>
              <ResponsiveContainer width="100%" height={220}>
                <PieChart>
                  <Pie
                    data={data.byModel}
                    dataKey="totalCost"
                    nameKey="model"
                    cx="50%"
                    cy="50%"
                    outerRadius={80}
                    label={(entry: PieLabelRenderProps) => {
                      const d = entry as unknown as { model?: string; totalCost?: number }
                      return `${d.model ?? ''} ${fmtCost(d.totalCost ?? 0)}`
                    }}
                    labelLine={{ stroke: AXIS_COLOR }}
                  >
                    {data.byModel.map((entry) => (
                      <Cell key={entry.model} fill={modelColor(entry.model)} />
                    ))}
                  </Pie>
                  <Tooltip
                    formatter={(value) => [fmtCost(Number(value)), 'Cost']}
                    contentStyle={TOOLTIP_STYLE}
                    itemStyle={{ color: 'var(--text-primary)' }}
                  />
                </PieChart>
              </ResponsiveContainer>
            </ChartCard>

            {/* Bar chart: error rate by day */}
            <ChartCard title="Error Rate by Day" style={{ flex: 1, minWidth: 280 }}>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={barData} margin={{ top: 4, right: 8, bottom: 0, left: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke={GRID_COLOR} />
                  <XAxis
                    dataKey="date"
                    tick={{ fontSize: 11, fill: AXIS_COLOR }}
                    axisLine={false}
                    tickLine={false}
                  />
                  <YAxis
                    tickFormatter={(v) => `${(v * 100).toFixed(0)}%`}
                    tick={{ fontSize: 11, fill: AXIS_COLOR }}
                    axisLine={false}
                    tickLine={false}
                    width={42}
                  />
                  <Tooltip content={<BarTooltip />} />
                  <Legend wrapperStyle={{ fontSize: 11, color: AXIS_COLOR, paddingTop: 8 }} />
                  <Bar dataKey="successRate" name="Success" stackId="a" fill="#10B981" radius={0} />
                  <Bar dataKey="errorRate" name="Error" stackId="a" fill="#EF4444" radius={[3, 3, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </ChartCard>
          </div>

          {/* Memory lineage + fingerprint insights */}
          <div style={{ display: 'flex', gap: 14, flexWrap: 'wrap' }}>
            <ChartCard title="Learning Velocity by Model" style={{ flex: 1, minWidth: 320 }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--border)' }}>
                    <th style={{ padding: '6px 8px', textAlign: 'left', color: 'var(--text-tertiary)', fontSize: 11 }}>Model</th>
                    <th style={{ padding: '6px 8px', textAlign: 'right', color: 'var(--text-tertiary)', fontSize: 11 }}>Success</th>
                    <th style={{ padding: '6px 8px', textAlign: 'right', color: 'var(--text-tertiary)', fontSize: 11 }}>Latency</th>
                    <th style={{ padding: '6px 8px', textAlign: 'right', color: 'var(--text-tertiary)', fontSize: 11 }}>Cost</th>
                    <th style={{ padding: '6px 8px', textAlign: 'right', color: 'var(--text-tertiary)', fontSize: 11 }}>Memory Hit</th>
                  </tr>
                </thead>
                <tbody>
                  {data.learningVelocityByModel.map((m) => (
                    <tr key={m.model} style={{ borderBottom: '1px solid var(--border)' }}>
                      <td style={{ padding: '7px 8px' }}>{m.model}</td>
                      <td style={{ padding: '7px 8px', textAlign: 'right' }}>{(m.successRate * 100).toFixed(0)}%</td>
                      <td style={{ padding: '7px 8px', textAlign: 'right' }}>{Math.round(m.avgLatencyMs)}ms</td>
                      <td style={{ padding: '7px 8px', textAlign: 'right' }}>${m.avgCost.toFixed(3)}</td>
                      <td style={{ padding: '7px 8px', textAlign: 'right' }}>{(m.memoryHitRate * 100).toFixed(0)}%</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </ChartCard>

            <ChartCard title="Memory Lineage (retrieval-influenced traces)" style={{ flex: 1, minWidth: 320 }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--border)' }}>
                    <th style={{ padding: '6px 8px', textAlign: 'left', color: 'var(--text-tertiary)', fontSize: 11 }}>Trace</th>
                    <th style={{ padding: '6px 8px', textAlign: 'right', color: 'var(--text-tertiary)', fontSize: 11 }}>Retrieval Spans</th>
                    <th style={{ padding: '6px 8px', textAlign: 'right', color: 'var(--text-tertiary)', fontSize: 11 }}>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {data.memoryLineage.map((m) => (
                    <tr key={m.traceId} style={{ borderBottom: '1px solid var(--border)' }}>
                      <td style={{ padding: '7px 8px', fontFamily: 'monospace', color: 'var(--accent-hover)' }}>{m.traceId.slice(0, 8)}…{m.traceId.slice(-6)}</td>
                      <td style={{ padding: '7px 8px', textAlign: 'right' }}>{m.retrievalSpanCount}</td>
                      <td style={{ padding: '7px 8px', textAlign: 'right' }}>{m.status}</td>
                    </tr>
                  ))}
                  {data.memoryLineage.length === 0 && (
                    <tr><td colSpan={3} style={{ padding: '10px 8px', color: 'var(--text-tertiary)' }}>No retrieval-heavy traces in selected window.</td></tr>
                  )}
                </tbody>
              </table>
            </ChartCard>

            <ChartCard title="Failure Fingerprints" style={{ flex: 1, minWidth: 320 }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--border)' }}>
                    <th style={{ padding: '6px 8px', textAlign: 'left', color: 'var(--text-tertiary)', fontSize: 11 }}>Fingerprint</th>
                    <th style={{ padding: '6px 8px', textAlign: 'right', color: 'var(--text-tertiary)', fontSize: 11 }}>Count</th>
                    <th style={{ padding: '6px 8px', textAlign: 'right', color: 'var(--text-tertiary)', fontSize: 11 }}>Share</th>
                  </tr>
                </thead>
                <tbody>
                  {data.failureFingerprints.map((f) => (
                    <tr key={f.fingerprint} style={{ borderBottom: '1px solid var(--border)' }}>
                      <td style={{ padding: '7px 8px' }}>{f.fingerprint}</td>
                      <td style={{ padding: '7px 8px', textAlign: 'right' }}>{f.count}</td>
                      <td style={{ padding: '7px 8px', textAlign: 'right' }}>{(f.share * 100).toFixed(0)}%</td>
                    </tr>
                  ))}
                  {data.failureFingerprints.length === 0 && (
                    <tr><td colSpan={3} style={{ padding: '10px 8px', color: 'var(--text-tertiary)' }}>No failed traces in selected window.</td></tr>
                  )}
                </tbody>
              </table>
            </ChartCard>
          </div>

          {/* Top expensive traces table */}
          <ChartCard title="Top 10 Most Expensive Traces">
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border)' }}>
                  {['Trace ID', 'Cost', 'Tokens', 'Started'].map((col, i) => (
                    <th key={col} style={{
                      padding: '6px 10px',
                      textAlign: i === 0 ? 'left' : 'right',
                      color: 'var(--text-tertiary)',
                      fontWeight: 500,
                      fontSize: 11,
                      textTransform: 'uppercase',
                      letterSpacing: '0.05em',
                    }}>
                      {col}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {data.topExpensiveTraces.map((t) => (
                  <tr
                    key={t.traceId}
                    onClick={() => selectTrace(t.traceId)}
                    style={{
                      borderBottom: '1px solid var(--border)',
                      cursor: 'pointer',
                      transition: 'background var(--transition)',
                    }}
                    onMouseEnter={(e) => (e.currentTarget.style.background = 'var(--surface-2)')}
                    onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
                  >
                    <td style={{ padding: '8px 10px', color: 'var(--accent-hover)', fontFamily: 'monospace', fontSize: 11 }}>
                      {t.traceId.length > 16 ? `${t.traceId.slice(0, 8)}…${t.traceId.slice(-6)}` : t.traceId}
                    </td>
                    <td style={{ padding: '8px 10px', textAlign: 'right', color: 'var(--text-primary)', fontWeight: 600 }}>
                      {fmtCost(t.totalCost)}
                    </td>
                    <td style={{ padding: '8px 10px', textAlign: 'right', color: 'var(--text-secondary)' }}>
                      {fmtTokens(t.totalTokens)}
                    </td>
                    <td style={{ padding: '8px 10px', textAlign: 'right', color: 'var(--text-tertiary)', fontSize: 11 }}>
                      {fmtDate(t.startedAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </ChartCard>
        </>
      )}

      {!loading && !data && (
        <div style={{
          display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
          height: 300, gap: 12, color: 'var(--text-tertiary)',
        }}>
          <BarChart3 size={32} strokeWidth={1} style={{ opacity: 0.3 }} />
          <p style={{ margin: 0, fontSize: 13, color: 'var(--text-secondary)' }}>Select an agent to view analytics</p>
        </div>
      )}
    </div>
  )
}
