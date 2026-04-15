import { useEffect, useState } from 'react'
import { Webhook } from 'lucide-react'
import { useAgentStore } from '../stores/useAgentStore'
import { useTraceStore } from '../stores/useTraceStore'
import { useSettingsStore } from '../stores/useSettingsStore'
import TraceTable from '../components/TraceTable'
import MetricsBar from '../components/MetricsBar'
import ErrorState from '../components/ErrorState'

export default function Dashboard({ onCompare }: { onCompare?: () => void }) {
  const autoRefreshSeconds = useSettingsStore((s) => s.settings.autoRefreshSeconds)
  const { agents, selectedAgentId } = useAgentStore()
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

  const [error, setError] = useState(false)

  useEffect(() => { loadTags() }, [loadTags])

  useEffect(() => {
    if (!selectedAgentId) return
    setError(false)
    loadTraces(selectedAgentId).catch(() => setError(true))
    const interval = setInterval(() => loadTraces(selectedAgentId).catch(() => setError(true)), autoRefreshSeconds * 1000)
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
        padding: 'clamp(12px, 3vw, 20px) clamp(12px, 4vw, 40px) 16px',
        borderBottom: '1px solid var(--border)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: availableTags.length > 0 ? 12 : 0 }}>
          <h1 style={{ fontSize: 20, fontWeight: 600, color: 'var(--text-primary)', letterSpacing: '-0.02em', margin: 0 }}>
            Dashboard
          </h1>
        </div>

        {availableTags.length > 0 && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
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
                    transition: 'all var(--transition)',
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
      <div>
        <MetricsBar />
      </div>

      {/* Trace list */}
      <main style={{ flex: 1, overflow: 'auto' }}>
        {tracesLoading && <LoadingState />}
        {!tracesLoading && error && selectedAgentId && (
          <ErrorState message="Failed to load traces" onRetry={() => { setError(false); loadTraces(selectedAgentId) }} />
        )}
        {!tracesLoading && !error && !selectedAgentId && (
          <EmptyState title="Select an agent to view traces" subtitle="Use the agent selector above" showIcon>
            {agents.length > 0 && null}
          </EmptyState>
        )}
        {!tracesLoading && !error && selectedAgentId && traces.length === 0 && (
          <EmptyState title="No traces yet" subtitle="No traces yet — send your first webhook to start observing" showIcon />
        )}
        {!tracesLoading && !error && traces.length > 0 && (
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

function LoadingState() {
  return (
    <div className="page-padding" style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      {[...Array(6)].map((_, i) => (
        <div key={i} className="skeleton" style={{
          height: 44, borderRadius: 6,
          marginBottom: 2,
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
