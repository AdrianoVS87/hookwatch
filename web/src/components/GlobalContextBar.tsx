import { useEffect } from 'react'
import { motion } from 'framer-motion'
import { useAgentStore, AVAILABLE_MODELS } from '../stores/useAgentStore'
import { useTraceStore } from '../stores/useTraceStore'

function AgentPillSkeleton() {
  return (
    <div style={{
      width: 90, height: 26, borderRadius: 20,
      background: 'var(--surface-2)', border: '1px solid var(--border)',
    }} />
  )
}

export default function GlobalContextBar() {
  const { agents, selectedAgentId, loading: agentsLoading, loadAgents, selectAgent } = useAgentStore()
  const { selectedModel, setSelectedModel } = useTraceStore()

  useEffect(() => {
    loadAgents()
  }, [loadAgents])

  return (
    <div style={{
      height: 44,
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      padding: '0 24px',
      background: 'var(--surface)',
      borderBottom: '1px solid var(--border)',
      flexShrink: 0,
      overflowX: 'auto',
    }}>
      {/* Agent selector label */}
      <span style={{
        fontSize: 10,
        fontWeight: 600,
        letterSpacing: '0.07em',
        textTransform: 'uppercase',
        color: 'var(--text-tertiary)',
        flexShrink: 0,
      }}>
        Agent
      </span>

      {/* Agent pills */}
      <div style={{ display: 'flex', gap: 5, alignItems: 'center' }}>
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
              data-testid="agent-pill"
              whileTap={{ scale: 0.96 }}
              onClick={() => selectAgent(agent.id)}
              style={{
                padding: '3px 10px',
                borderRadius: 20,
                border: active ? '1px solid rgba(99,102,241,0.4)' : '1px solid var(--border)',
                background: active ? 'rgba(99,102,241,0.12)' : 'var(--surface-2)',
                color: active ? 'var(--accent-hover)' : 'var(--text-secondary)',
                fontSize: 11,
                fontWeight: active ? 500 : 400,
                cursor: 'pointer',
                transition: 'all 0.15s ease',
                whiteSpace: 'nowrap',
                flexShrink: 0,
              }}
            >
              {agent.name}
            </motion.button>
          )
        })}
      </div>

      {/* Divider */}
      <div style={{ width: 1, height: 20, background: 'var(--border)', flexShrink: 0 }} />

      {/* Model filter label */}
      <span style={{
        fontSize: 10,
        fontWeight: 600,
        letterSpacing: '0.07em',
        textTransform: 'uppercase',
        color: 'var(--text-tertiary)',
        flexShrink: 0,
      }}>
        Model
      </span>

      {/* Model dropdown */}
      <select
        value={selectedModel ?? ''}
        onChange={(e) => setSelectedModel(e.target.value || null)}
        style={{
          background: 'var(--surface-2)',
          border: '1px solid var(--border)',
          borderRadius: 6,
          color: selectedModel ? 'var(--accent-hover)' : 'var(--text-secondary)',
          fontSize: 11,
          padding: '3px 8px',
          cursor: 'pointer',
          outline: 'none',
          height: 26,
          flexShrink: 0,
        }}
      >
        <option value="">All models</option>
        {AVAILABLE_MODELS.map((m) => (
          <option key={m} value={m}>{m}</option>
        ))}
      </select>

      {/* Live indicator when agent selected */}
      {selectedAgentId && (
        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 5, flexShrink: 0 }}>
          <span style={{ width: 5, height: 5, borderRadius: '50%', background: '#10B981', animation: 'pulse 2s infinite', display: 'block' }} />
          <span style={{ fontSize: 10, fontWeight: 600, letterSpacing: '0.08em', textTransform: 'uppercase', color: '#10B981' }}>
            Live
          </span>
        </div>
      )}
    </div>
  )
}
