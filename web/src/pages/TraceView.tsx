import { useEffect, useState, type CSSProperties } from 'react'
import { ArrowLeft, ChevronDown, ChevronUp, GitBranch, StickyNote, ShieldCheck } from 'lucide-react'
import { useTraceStore } from '../stores/useTraceStore'
import TraceCanvas from '../components/TraceCanvas'
import SpanDetail from '../components/SpanDetail'
import TraceSelector from '../components/TraceSelector'
import type { Annotation, Span } from '../types'
import { createAnnotation, fetchAnnotations, fetchMemoryLineage } from '../api/traces'
import { fetchTraceCompliance, type TraceComplianceReport } from '../api/compliance'

const STATUS_COLOR: Record<string, string> = {
  COMPLETED: '#10B981', RUNNING: '#6366F1', FAILED: '#EF4444',
}

export default function TraceView() {
  const { selectedTrace, clearTrace, selectTrace, traces, rawTraces } = useTraceStore()
  const [activeSpan, setActiveSpan] = useState<Span | null>(null)
  const [annotations, setAnnotations] = useState<Annotation[]>([])
  const [memoryLineage, setMemoryLineage] = useState<{ retrievalSpanNames: string[]; memoryReferences: string[]; retrievalSpanCount: number } | null>(null)
  const [compliance, setCompliance] = useState<TraceComplianceReport | null>(null)
  const [complianceOpen, setComplianceOpen] = useState(false)
  const [annotationOpen, setAnnotationOpen] = useState(true)
  const [text, setText] = useState('')
  const [author, setAuthor] = useState('adriano')
  const [saving, setSaving] = useState(false)

  // Use rawTraces for full list, fall back to filtered traces
  const allTraces = rawTraces.length > 0 ? rawTraces : traces

  useEffect(() => {
    if (!selectedTrace) {
      setAnnotations([])
      setMemoryLineage(null)
      return
    }
    fetchAnnotations(selectedTrace.id)
      .then(setAnnotations)
      .catch(() => setAnnotations([]))

    fetchMemoryLineage(selectedTrace.id)
      .then((v) => setMemoryLineage(v))
      .catch(() => setMemoryLineage(null))

    fetchTraceCompliance(selectedTrace.id)
      .then(setCompliance)
      .catch(() => setCompliance(null))
  }, [selectedTrace?.id])

  if (!selectedTrace) {
    return (
      <div className="page-padding">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
          <GitBranch size={18} strokeWidth={1.5} style={{ color: 'var(--accent)' }} />
          <h2 style={{ fontSize: 20, fontWeight: 600, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>Traces</h2>
        </div>
        {allTraces.length > 0 ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <TraceSelector
              traces={allTraces}
              selectedTraceId={null}
              onSelect={selectTrace}
              showNavigation={false}
              showCount={false}
            />
            <p style={{ color: 'var(--text-tertiary)', fontSize: 13, margin: 0 }}>
              Select a trace above to inspect its span graph.
            </p>
          </div>
        ) : (
          <p style={{ color: 'var(--text-tertiary)', fontSize: 13 }}>Select a trace from the Dashboard to inspect its span graph.</p>
        )}
      </div>
    )
  }

  const submitAnnotation = async () => {
    if (!text.trim() || !author.trim()) return
    setSaving(true)
    try {
      const created = await createAnnotation(selectedTrace.id, text.trim(), author.trim())
      setAnnotations((prev) => [created, ...prev])
      setText('')
      setAnnotationOpen(true)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <header style={{
        display: 'flex', alignItems: 'center', gap: 8,
        padding: '10px clamp(10px, 2vw, 20px)', borderBottom: '1px solid var(--border)',
        flexShrink: 0, flexWrap: 'wrap',
      }}>
        <button
          onClick={() => { clearTrace(); setActiveSpan(null) }}
          style={{
            background: 'var(--surface-2)', border: '1px solid var(--border)',
            borderRadius: 6, padding: '5px 8px', cursor: 'pointer',
            color: 'var(--text-secondary)', display: 'flex', alignItems: 'center',
          }}
        >
          <ArrowLeft size={13} strokeWidth={1.5} />
        </button>

        {/* Inline trace selector */}
        {allTraces.length > 0 && (
          <TraceSelector
            traces={allTraces}
            selectedTraceId={selectedTrace.id}
            onSelect={(id) => { selectTrace(id); setActiveSpan(null) }}
            showNavigation={true}
            showCount={true}
          />
        )}

        {/* Status + tags (shown when no selector or as supplement) */}
        {allTraces.length === 0 && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)', fontFamily: '"SF Mono", monospace' }}>
              {selectedTrace.id.slice(0, 8)}…
            </span>
            <span style={{
              fontSize: 11, fontWeight: 500,
              color: STATUS_COLOR[selectedTrace.status],
              background: `${STATUS_COLOR[selectedTrace.status]}18`,
              padding: '2px 8px', borderRadius: 4,
            }}>
              {selectedTrace.status}
            </span>
          </div>
        )}

        {(selectedTrace.tags ?? []).length > 0 && (
          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
            {selectedTrace.tags.map((tag) => (
              <span
                key={tag}
                style={{
                  fontSize: 10,
                  padding: '2px 8px',
                  borderRadius: 999,
                  border: '1px solid rgba(99,102,241,0.35)',
                  color: 'var(--accent-hover)',
                  background: 'rgba(99,102,241,0.1)',
                }}
              >
                {tag}
              </span>
            ))}
          </div>
        )}

        <div style={{ marginLeft: 'auto', display: 'flex', gap: 16 }}>
          {selectedTrace.totalTokens != null && (
            <Stat label="Tokens" value={selectedTrace.totalTokens.toLocaleString()} />
          )}
          {selectedTrace.totalCost != null && (
            <Stat label="Cost" value={`$${selectedTrace.totalCost.toFixed(4)}`} />
          )}
          <Stat label="Spans" value={String(selectedTrace.spans.length)} />
          {compliance && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <ShieldCheck
                size={14}
                strokeWidth={1.5}
                style={{
                  color: compliance.failed === 0
                    ? '#10B981'
                    : compliance.failed <= 2
                      ? '#F59E0B'
                      : '#EF4444',
                }}
              />
              <Stat
                label="Compliance"
                value={`${compliance.passed}/${compliance.totalChecks}`}
              />
            </div>
          )}
        </div>
      </header>

      <section style={{
        borderBottom: '1px solid var(--border)',
        background: 'var(--surface)',
        padding: '10px 16px',
        fontSize: 12,
      }}>
        <div style={{ color: 'var(--text-secondary)', marginBottom: 6, fontWeight: 600 }}>Memory Lineage</div>
        {(() => {
          const retrievalSpans = selectedTrace.spans.filter((s) => s.type === 'RETRIEVAL')
          const memoryNodes = (memoryLineage?.retrievalSpanNames?.length
            ? memoryLineage.retrievalSpanNames
            : retrievalSpans.slice(0, 5).map((s) => s.name)
          ).filter(Boolean)

          const metadataMemory = memoryLineage?.memoryReferences?.length
            ? memoryLineage.memoryReferences
            : selectedTrace.metadata && typeof selectedTrace.metadata === 'object'
              ? (selectedTrace.metadata['memoryLineage'] as unknown[] | undefined)
              : undefined

          if ((!metadataMemory || metadataMemory.length === 0) && memoryNodes.length === 0) {
            return <span style={{ color: 'var(--text-tertiary)' }}>No memory lineage signals captured for this trace.</span>
          }

          return (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
              {memoryNodes.map((n) => (
                <span key={n} style={{ border: '1px solid rgba(16,185,129,0.35)', background: 'rgba(16,185,129,0.1)', color: '#10B981', borderRadius: 999, padding: '3px 10px' }}>
                  retrieval:{n}
                </span>
              ))}
              {(metadataMemory ?? []).slice(0, 5).map((m, idx) => (
                <span key={`m-${idx}`} style={{ border: '1px solid rgba(99,102,241,0.35)', background: 'rgba(99,102,241,0.1)', color: 'var(--accent-hover)', borderRadius: 999, padding: '3px 10px' }}>
                  memory:{String(m)}
                </span>
              ))}
            </div>
          )
        })()}
      </section>

      {/* OTel Compliance Detail */}
      {compliance && compliance.gaps.length > 0 && (
        <section style={{
          borderBottom: '1px solid var(--border)',
          background: 'var(--surface)',
          padding: '0',
        }}>
          <button
            onClick={() => setComplianceOpen((prev) => !prev)}
            style={{
              width: '100%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              background: 'transparent',
              border: 'none',
              color: 'var(--text-primary)',
              fontSize: 12,
              padding: '10px 16px',
              cursor: 'pointer',
            }}
          >
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
              <ShieldCheck size={14} strokeWidth={1.6} color={compliance.failed <= 2 ? '#F59E0B' : '#EF4444'} />
              Compliance Gaps ({compliance.failed})
            </span>
            {complianceOpen ? <ChevronDown size={14} /> : <ChevronUp size={14} />}
          </button>
          {complianceOpen && (
            <div style={{ padding: '0 16px 14px', maxHeight: 200, overflow: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 11 }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--border)' }}>
                    <th style={{ padding: '4px 8px', textAlign: 'left', color: 'var(--text-tertiary)' }}>Field</th>
                    <th style={{ padding: '4px 8px', textAlign: 'left', color: 'var(--text-tertiary)' }}>Expected</th>
                    <th style={{ padding: '4px 8px', textAlign: 'left', color: 'var(--text-tertiary)' }}>Actual</th>
                    <th style={{ padding: '4px 8px', textAlign: 'left', color: 'var(--text-tertiary)' }}>Severity</th>
                  </tr>
                </thead>
                <tbody>
                  {compliance.gaps.map((g, i) => {
                    const sevColor = g.severity === 'ERROR' ? '#EF4444' : g.severity === 'WARNING' ? '#F59E0B' : 'var(--text-tertiary)'
                    return (
                      <tr key={i} style={{ borderBottom: '1px solid rgba(255,255,255,0.04)' }}>
                        <td style={{ padding: '6px 8px', fontFamily: 'monospace', color: 'var(--text-primary)' }}>{g.field}</td>
                        <td style={{ padding: '6px 8px', color: 'var(--text-secondary)' }}>{g.expected}</td>
                        <td style={{ padding: '6px 8px', color: sevColor }}>{g.actual}</td>
                        <td style={{ padding: '6px 8px' }}>
                          <span style={{
                            fontSize: 9, fontWeight: 600, padding: '1px 6px', borderRadius: 3,
                            background: g.severity === 'ERROR' ? 'rgba(239,68,68,0.1)' : g.severity === 'WARNING' ? 'rgba(245,158,11,0.1)' : 'rgba(255,255,255,0.06)',
                            color: sevColor,
                          }}>
                            {g.severity}
                          </span>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}

      <div className="trace-detail-layout">
        <div style={{ flex: 1 }}>
          {selectedTrace.spans.length === 0 ? (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'var(--text-tertiary)', fontSize: 13 }}>
              No spans in this trace
            </div>
          ) : (
            <TraceCanvas spans={selectedTrace.spans} onNodeClick={(id) => {
              setActiveSpan(selectedTrace.spans.find(s => s.id === id) ?? null)
            }} />
          )}
        </div>
        {activeSpan && (
          <SpanDetail span={activeSpan} onClose={() => setActiveSpan(null)} />
        )}
      </div>

      <section style={{
        borderTop: '1px solid var(--border)',
        background: 'var(--surface)',
        flexShrink: 0,
      }}>
        <button
          onClick={() => setAnnotationOpen((prev) => !prev)}
          style={{
            width: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            background: 'transparent',
            border: 'none',
            color: 'var(--text-primary)',
            fontSize: 12,
            padding: '10px 16px',
            cursor: 'pointer',
          }}
        >
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
            <StickyNote size={14} strokeWidth={1.6} />
            Annotations ({annotations.length})
          </span>
          {annotationOpen ? <ChevronDown size={14} /> : <ChevronUp size={14} />}
        </button>

        {annotationOpen && (
          <div style={{ padding: '0 16px 14px', display: 'grid', gap: 10, maxHeight: 260, overflow: 'auto' }}>
            <div className="annotation-form" style={{ display: 'grid', gridTemplateColumns: '140px 1fr auto', gap: 8 }}>
              <input
                value={author}
                onChange={(e) => setAuthor(e.target.value)}
                placeholder="author"
                style={inputStyle}
              />
              <input
                value={text}
                onChange={(e) => setText(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') void submitAnnotation() }}
                placeholder="Add annotation..."
                style={inputStyle}
              />
              <button
                onClick={() => void submitAnnotation()}
                disabled={saving || !text.trim()}
                style={{
                  borderRadius: 6,
                  border: '1px solid rgba(99,102,241,0.4)',
                  background: 'rgba(99,102,241,0.15)',
                  color: 'var(--accent-hover)',
                  padding: '0 12px',
                  fontSize: 12,
                  cursor: 'pointer',
                  opacity: saving ? 0.7 : 1,
                }}
              >
                {saving ? 'Saving…' : 'Add'}
              </button>
            </div>

            {annotations.length === 0 ? (
              <span style={{ color: 'var(--text-tertiary)', fontSize: 12 }}>No annotations yet.</span>
            ) : (
              annotations.map((annotation) => (
                <div
                  key={annotation.id}
                  style={{
                    border: '1px solid var(--border)',
                    background: 'var(--surface-2)',
                    borderRadius: 8,
                    padding: '10px 12px',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, marginBottom: 6 }}>
                    <span style={{ fontSize: 11, color: 'var(--text-secondary)', fontWeight: 500 }}>{annotation.author}</span>
                    <span style={{ fontSize: 10, color: 'var(--text-tertiary)' }}>
                      {new Date(annotation.createdAt).toLocaleString()}
                    </span>
                  </div>
                  <p style={{ margin: 0, fontSize: 12, color: 'var(--text-primary)', lineHeight: 1.45 }}>{annotation.text}</p>
                </div>
              ))
            )}
          </div>
        )}
      </section>
    </div>
  )
}

const inputStyle: CSSProperties = {
  borderRadius: 6,
  border: '1px solid var(--border)',
  background: 'var(--surface-2)',
  color: 'var(--text-primary)',
  padding: '8px 10px',
  fontSize: 12,
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div style={{ fontSize: 10, color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{label}</div>
      <div style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)', fontVariantNumeric: 'tabular-nums' }}>{value}</div>
    </div>
  )
}
