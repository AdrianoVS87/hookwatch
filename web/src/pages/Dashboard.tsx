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
  useEffect(() => { if (selectedAgentId) loadTraces(selectedAgentId) }, [selectedAgentId, loadTraces])

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Header */}
      <header style={{
        padding: '28px 40px 20px',
        borderBottom: '1px solid var(--border)',
      }}>
        <h1 style={{ fontSize: 20, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 16, letterSpacing: '-0.02em' }}>
          Dashboard
        </h1>

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
