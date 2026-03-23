import { useEffect } from 'react'
import { motion } from 'framer-motion'
import { useAgentStore } from '../stores/useAgentStore'
import { useTraceStore } from '../stores/useTraceStore'
import TraceTable from '../components/TraceTable'
import MetricsBar from '../components/MetricsBar'

export default function Dashboard() {
  const { agents, selectedAgentId, loading: agentsLoading, loadAgents, selectAgent } = useAgentStore()
  const { traces, loading: tracesLoading, loadTraces, selectTrace } = useTraceStore()

  useEffect(() => { loadAgents() }, [loadAgents])
  useEffect(() => {
    if (!selectedAgentId) return
    loadTraces(selectedAgentId)
    // Auto-refresh every 30s to pick up new traces from live agent
    const interval = setInterval(() => loadTraces(selectedAgentId), 30_000)
    return () => clearInterval(interval)
  }, [selectedAgentId, loadTraces])

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Header */}
      <header style={{
        padding: '28px 40px 20px',
        borderBottom: '1px solid var(--border)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
          <h1 style={{ fontSize: 20, fontWeight: 600, color: 'var(--text-primary)', letterSpacing: '-0.02em', margin: 0 }}>
            Dashboard
          </h1>
          {selectedAgentId && (
            <span style={{
              display: 'inline-flex', alignItems: 'center', gap: 5,
              fontSize: 10, fontWeight: 600, letterSpacing: '0.08em',
              textTransform: 'uppercase', padding: '2px 7px', borderRadius: 4,
              background: 'rgba(16,185,129,0.1)', color: '#10B981',
              border: '1px solid rgba(16,185,129,0.2)',
            }}>
              <span style={{ width: 5, height: 5, borderRadius: '50%', background: '#10B981', animation: 'pulse 2s infinite' }} />
              Live
            </span>
          )}
        </div>

        {/* Agent pills */}
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
          {agentsLoading ? (
            <>
              <AgentPillSkeleton />
              <AgentPillSkeleton />
            </>
          ) : agents.map((agent) => {
            const active = selectedAgentId === agent.id
            return (
              <motion.button
                key={agent.id}
                whileTap={{ scale: 0.97 }}
                onClick={() => selectAgent(agent.id)}
                style={{
                  padding: '5px 12px',
                  borderRadius: 20,
                  border: active ? '1px solid rgba(99,102,241,0.4)' : '1px solid var(--border)',
                  background: active ? 'rgba(99,102,241,0.12)' : 'var(--surface-2)',
                  color: active ? 'var(--accent-hover)' : 'var(--text-secondary)',
                  fontSize: 12, fontWeight: active ? 500 : 400,
                  cursor: 'pointer', transition: 'all 0.15s ease',
                }}
              >
                {agent.name}
              </motion.button>
            )
          })}
        </div>
      </header>

      {/* Metrics bar */}
      <MetricsBar />

      {/* Trace list */}
      <main style={{ flex: 1, overflow: 'auto' }}>
        {tracesLoading && <LoadingState />}
        {!tracesLoading && !selectedAgentId && (
          <EmptyState title="Select an agent" subtitle="Choose an agent above to view its traces" />
        )}
        {!tracesLoading && selectedAgentId && traces.length === 0 && (
          <EmptyState title="No traces yet" subtitle="Traces will appear here once the agent runs" />
        )}
        {!tracesLoading && traces.length > 0 && (
          <TraceTable traces={traces} onSelect={selectTrace} />
        )}
      </main>
    </div>
  )
}

function AgentPillSkeleton() {
  return (
    <div style={{
      width: 100, height: 28, borderRadius: 20,
      background: 'var(--surface-2)', border: '1px solid var(--border)',
    }} />
  )
}

function LoadingState() {
  return (
    <div style={{ padding: '40px', display: 'flex', flexDirection: 'column', gap: 1 }}>
      {[...Array(5)].map((_, i) => (
        <div key={i} style={{
          height: 44, borderRadius: 6,
          background: `rgba(255,255,255,${0.025 - i * 0.003})`,
          marginBottom: 1,
        }} />
      ))}
    </div>
  )
}

function EmptyState({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '50%', gap: 6 }}>
      <p style={{ fontSize: 14, fontWeight: 500, color: 'var(--text-secondary)', margin: 0 }}>{title}</p>
      <p style={{ fontSize: 12, color: 'var(--text-tertiary)', margin: 0 }}>{subtitle}</p>
    </div>
  )
}
