import { useEffect } from 'react'
import { motion } from 'framer-motion'
import { Webhook } from 'lucide-react'
import { useAgentStore } from '../stores/useAgentStore'
import { useTraceStore } from '../stores/useTraceStore'
import { useSettingsStore } from '../stores/useSettingsStore'
import TraceTable from '../components/TraceTable'
import MetricsBar from '../components/MetricsBar'

export default function Dashboard({ onCompare }: { onCompare?: () => void }) {
  const autoRefreshSeconds = useSettingsStore((s) => s.settings.autoRefreshSeconds)
  const { agents, selectedAgentId, loading: agentsLoading, loadAgents, selectAgent } = useAgentStore()
  const {
    traces,
    totalElements,
    loading: tracesLoading,
    loadTraces,
    selectTrace,
    availableTags,
    selectedTags,
    setSelectedTags,
    setSelectedTag,
    loadTags,
    addTagsToTrace,
    removeTagFromTrace,
  } = useTraceStore()

  useEffect(() => { loadAgents() }, [loadAgents])
  useEffect(() => {
    loadTags()
  }, [loadTags])

  useEffect(() => {
    if (!selectedAgentId) return
    loadTraces(selectedAgentId)
    const interval = setInterval(() => loadTraces(selectedAgentId), autoRefreshSeconds * 1000)
    return () => clearInterval(interval)
  }, [selectedAgentId, loadTraces, autoRefreshSeconds])

  const toggleTag = (tag: string) => {
    if (selectedTags.includes(tag)) {
      setSelectedTags(selectedTags.filter((t) => t !== tag))
      return
    }
    setSelectedTags([...selectedTags, tag])
  }

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

        {availableTags.length > 0 && (
          <div style={{ marginTop: 12, display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
            <span style={{ fontSize: 11, color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
              Tag filter
            </span>
            {availableTags.map((tag) => {
              const active = selectedTags.includes(tag)
              return (
                <button
                  key={tag}
                  onClick={() => toggleTag(tag)}
                  style={{
                    padding: '3px 10px',
                    borderRadius: 999,
                    border: active ? '1px solid rgba(99,102,241,0.45)' : '1px solid var(--border)',
                    background: active ? 'rgba(99,102,241,0.14)' : 'var(--surface-2)',
                    color: active ? 'var(--accent-hover)' : 'var(--text-secondary)',
                    fontSize: 11,
                    cursor: 'pointer',
                  }}
                >
                  {tag}
                </button>
              )
            })}
          </div>
        )}
      </header>

      {/* Metrics bar */}
      <div style={{ marginTop: 24 }}>
        <MetricsBar />
      </div>

      {/* Trace list */}
      <main style={{ flex: 1, overflow: 'auto' }}>
        {tracesLoading && <LoadingState />}
        {!tracesLoading && !selectedAgentId && (
          <EmptyState title="Select an agent to view traces" subtitle="Use ⌘K to search" showIcon>
            {agents.length > 0 && (
              <button
                onClick={() => selectAgent(agents[0].id)}
                style={{
                  marginTop: 8, padding: '6px 14px', borderRadius: 6,
                  border: '1px solid var(--border)', background: 'var(--surface-2)',
                  color: 'var(--text-secondary)', fontSize: 12, cursor: 'pointer',
                }}
              >
                View all agents
              </button>
            )}
          </EmptyState>
        )}
        {!tracesLoading && selectedAgentId && traces.length === 0 && (
          <EmptyState title="No traces yet" subtitle="Traces will appear here once the agent runs" />
        )}
        {!tracesLoading && traces.length > 0 && (
          <TraceTable
            traces={traces}
            onSelect={selectTrace}
            onCompare={onCompare}
            totalElements={totalElements ?? undefined}
            onTagClick={setSelectedTag}
            onAddTags={addTagsToTrace}
            onDeleteTag={removeTagFromTrace}
          />
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

function EmptyState({ title, subtitle, showIcon, children }: { title: string; subtitle: string; showIcon?: boolean; children?: React.ReactNode }) {
  return (
    <div style={{
      display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
      height: '50%', gap: 12, color: 'var(--text-tertiary)',
    }}>
      {showIcon && <Webhook size={32} strokeWidth={1} style={{ opacity: 0.4 }} />}
      <p style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-secondary)', margin: 0 }}>{title}</p>
      <p style={{ fontSize: 11, margin: 0, opacity: 0.6 }}>{subtitle}</p>
      {children}
    </div>
  )
}
