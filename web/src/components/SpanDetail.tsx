import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { X } from 'lucide-react'
import type { Span, Score } from '../types'
import { fetchTraceScores } from '../api/traces'

type Tab = 'overview' | 'input' | 'output' | 'error' | 'scores'
const TABS: Tab[] = ['overview', 'input', 'output', 'error', 'scores']

interface Props { span: Span; onClose: () => void }

export default function SpanDetail({ span, onClose }: Props) {
  const [tab, setTab] = useState<Tab>('overview')
  const [scores, setScores] = useState<Score[]>([])
  const durationMs = span.completedAt
    ? new Date(span.completedAt).getTime() - new Date(span.startedAt).getTime()
    : null

  useEffect(() => {
    fetchTraceScores(span.traceId).then(setScores).catch(() => setScores([]))
  }, [span.traceId])

  return (
    <motion.aside
      initial={{ x: 20, opacity: 0 }}
      animate={{ x: 0, opacity: 1 }}
      exit={{ x: 20, opacity: 0 }}
      transition={{ duration: 0.15, ease: 'easeOut' }}
      style={{
        width: 320, maxWidth: '100vw', flexShrink: 0,
        background: 'var(--surface)',
        borderLeft: '1px solid var(--border)',
        display: 'flex', flexDirection: 'column',
        height: '100%', overflow: 'hidden',
      }}
    >
      {/* Header */}
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '14px 16px', borderBottom: '1px solid var(--border)',
      }}>
        <div style={{ overflow: 'hidden' }}>
          <div style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {span.name}
          </div>
          <div style={{ fontSize: 11, color: 'var(--text-tertiary)', marginTop: 2 }}>{span.type}</div>
        </div>
        <button onClick={onClose} style={{
          background: 'none', border: 'none', cursor: 'pointer',
          color: 'var(--text-tertiary)', padding: 4, borderRadius: 4,
          display: 'flex', alignItems: 'center',
        }}>
          <X size={14} strokeWidth={1.5} />
        </button>
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', borderBottom: '1px solid var(--border)' }}>
        {TABS.map((t) => (
          <button key={t} onClick={() => setTab(t)} style={{
            flex: 1, padding: '9px 0', background: 'none', border: 'none', cursor: 'pointer',
            fontSize: 11, fontWeight: tab === t ? 500 : 400, textTransform: 'capitalize',
            color: tab === t ? 'var(--text-primary)' : 'var(--text-tertiary)',
            borderBottom: tab === t ? '1px solid var(--accent)' : '1px solid transparent',
            transition: 'all 0.15s ease',
          }}>{t === 'scores' ? `Scores${scores.length ? ` (${scores.length})` : ''}` : t}</button>
        ))}
      </div>

      {/* Content */}
      <div style={{ flex: 1, overflow: 'auto', padding: 16 }}>
        {tab === 'overview' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {[
              { label: 'Status',        value: span.status },
              { label: 'Model',         value: span.model },
              { label: 'Input Tokens',  value: span.inputTokens != null ? span.inputTokens.toLocaleString() : null },
              { label: 'Output Tokens', value: span.outputTokens != null ? span.outputTokens.toLocaleString() : null },
              { label: 'Cost',          value: span.cost != null ? `$${span.cost.toFixed(6)}` : null },
              { label: 'Duration',      value: durationMs != null ? `${durationMs}ms` : null },
            ].filter(r => r.value != null).map(({ label, value }) => (
              <div key={label}>
                <div style={{ fontSize: 11, color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 3 }}>
                  {label}
                </div>
                <div style={{ fontSize: 13, color: 'var(--text-primary)', fontWeight: 500 }}>{value}</div>
              </div>
            ))}
          </div>
        )}
        {(tab === 'input' || tab === 'output' || tab === 'error') && (
          <CodePane text={tab === 'input' ? span.input : tab === 'output' ? span.output : span.error}
            isError={tab === 'error'} />
        )}
        {tab === 'scores' && (
          <ScoresPanel scores={scores} />
        )}
      </div>
    </motion.aside>
  )
}

function ScoresPanel({ scores }: { scores: Score[] }) {
  if (scores.length === 0) {
    return <div style={{ color: 'var(--text-tertiary)', fontSize: 12, fontStyle: 'italic' }}>No scores</div>
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      {scores.map(score => (
        <div key={score.id} style={{
          padding: '10px 12px', borderRadius: 6,
          background: 'var(--surface-2)', border: '1px solid var(--border)',
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
            <span style={{ fontSize: 12, fontWeight: 500, color: 'var(--text-primary)' }}>{score.name}</span>
            <span style={{
              fontSize: 9, padding: '1px 5px', borderRadius: 3,
              background: 'rgba(99,102,241,0.1)', color: 'var(--accent)', fontWeight: 500,
            }}>{score.source}</span>
          </div>
          <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>
            {score.dataType === 'NUMERIC' && score.numericValue != null && (
              <span style={{
                color: score.numericValue > 0.7 ? 'var(--success)' : score.numericValue > 0.4 ? 'var(--warning)' : 'var(--error)',
              }}>
                {score.numericValue.toFixed(2)}
              </span>
            )}
            {score.dataType === 'BOOLEAN' && (
              <span style={{ color: score.booleanValue ? 'var(--success)' : 'var(--error)' }}>
                {score.booleanValue ? '\u2713 True' : '\u2717 False'}
              </span>
            )}
            {score.dataType === 'CATEGORICAL' && (
              <span style={{ color: 'var(--accent)' }}>{score.stringValue}</span>
            )}
          </div>
          {score.comment && (
            <div style={{ fontSize: 11, color: 'var(--text-tertiary)', marginTop: 6 }}>{score.comment}</div>
          )}
        </div>
      ))}
    </div>
  )
}

function CodePane({ text, isError }: { text: string | null; isError?: boolean }) {
  if (!text) return (
    <div style={{ color: 'var(--text-tertiary)', fontSize: 12, fontStyle: 'italic' }}>No data</div>
  )
  return (
    <pre style={{
      margin: 0, fontSize: 11, lineHeight: 1.6,
      color: isError ? 'var(--error)' : 'var(--text-secondary)',
      fontFamily: '"SF Mono", "Fira Code", "Fira Mono", monospace',
      whiteSpace: 'pre-wrap', wordBreak: 'break-word',
    }}>{text}</pre>
  )
}
