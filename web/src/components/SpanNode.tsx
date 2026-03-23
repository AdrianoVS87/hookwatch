import { memo } from 'react'
import { Handle, Position, type NodeProps } from '@xyflow/react'
import type { SpanType, SpanStatus } from '../types'

export interface SpanNodeData extends Record<string, unknown> {
  name: string
  type: SpanType
  status: SpanStatus
  model: string | null
  inputTokens: number | null
  outputTokens: number | null
}

const TYPE_CONFIG: Record<SpanType, { color: string; bg: string; label: string }> = {
  LLM_CALL:  { color: '#6366F1', bg: 'rgba(99,102,241,0.08)',  label: 'LLM' },
  TOOL_CALL: { color: '#10B981', bg: 'rgba(16,185,129,0.08)',  label: 'Tool' },
  RETRIEVAL: { color: '#F59E0B', bg: 'rgba(245,158,11,0.08)',  label: 'Retrieval' },
  CUSTOM:    { color: '#8B95A1', bg: 'rgba(139,149,161,0.08)', label: 'Custom' },
}

export const SpanNode = memo(({ data, selected }: NodeProps) => {
  const d = data as SpanNodeData
  const isFailed  = d.status === 'FAILED'
  const isRunning = d.status === 'RUNNING'
  const cfg = TYPE_CONFIG[d.type]
  const tokens = (d.inputTokens ?? 0) + (d.outputTokens ?? 0)

  const borderColor = isFailed ? '#EF4444' : selected ? '#6366F1' : 'rgba(255,255,255,0.08)'
  const bgColor = isFailed ? 'rgba(239,68,68,0.06)' : cfg.bg

  return (
    <div style={{
      background: bgColor,
      border: `1px solid ${borderColor}`,
      borderRadius: 10,
      padding: '10px 14px',
      minWidth: 180,
      maxWidth: 260,
      animation: isRunning ? 'pulse 2s infinite' : 'none',
      boxShadow: selected ? `0 0 0 3px rgba(99,102,241,0.2)` : '0 2px 12px rgba(0,0,0,0.3)',
      transition: 'box-shadow 0.15s ease',
    }}>
      <Handle type="target" position={Position.Top}
        style={{ background: 'rgba(255,255,255,0.15)', border: '1px solid rgba(255,255,255,0.1)', width: 8, height: 8 }} />

      {/* Type badge */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
        <span style={{
          fontSize: 10, fontWeight: 600, letterSpacing: '0.05em',
          color: isFailed ? '#EF4444' : cfg.color,
          textTransform: 'uppercase',
        }}>
          {cfg.label}
        </span>
        {isFailed  && <span style={{ fontSize: 10, color: '#EF4444', fontWeight: 500 }}>· Failed</span>}
        {isRunning && <span style={{ fontSize: 10, color: '#6366F1', fontWeight: 500 }}>· Running</span>}
      </div>

      {/* Name */}
      <div style={{
        fontSize: 13, fontWeight: 500, color: 'var(--text-primary)',
        overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
        marginBottom: d.model || tokens > 0 ? 6 : 0,
      }}>
        {d.name}
      </div>

      {/* Meta */}
      {(d.model || tokens > 0) && (
        <div style={{ display: 'flex', gap: 8 }}>
          {d.model && (
            <span style={{ fontSize: 11, color: 'var(--text-tertiary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {d.model}
            </span>
          )}
          {tokens > 0 && (
            <span style={{ fontSize: 11, color: 'var(--text-tertiary)', marginLeft: 'auto', flexShrink: 0 }}>
              {tokens.toLocaleString()} tok
            </span>
          )}
        </div>
      )}

      <Handle type="source" position={Position.Bottom}
        style={{ background: 'rgba(255,255,255,0.15)', border: '1px solid rgba(255,255,255,0.1)', width: 8, height: 8 }} />
    </div>
  )
})

SpanNode.displayName = 'SpanNode'
