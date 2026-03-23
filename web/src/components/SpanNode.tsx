import { memo } from 'react'
import { Handle, Position } from '@xyflow/react'
import type { NodeProps } from '@xyflow/react'
import type { SpanType, SpanStatus } from '../types'

interface SpanNodeData {
  name: string
  type: SpanType
  status: SpanStatus
  model?: string | null
  inputTokens?: number | null
  outputTokens?: number | null
  [key: string]: unknown
}

const typeColors: Record<SpanType, string> = {
  LLM_CALL: 'border-blue-500 bg-blue-500/10',
  TOOL_CALL: 'border-green-500 bg-green-500/10',
  RETRIEVAL: 'border-orange-500 bg-orange-500/10',
  CUSTOM: 'border-slate-500 bg-slate-500/10',
}

const typeLabels: Record<SpanType, string> = {
  LLM_CALL: 'LLM',
  TOOL_CALL: 'Tool',
  RETRIEVAL: 'Retrieval',
  CUSTOM: 'Custom',
}

export const SpanNode = memo(({ data, selected }: NodeProps) => {
  const d = data as SpanNodeData
  const isFailed = d.status === 'FAILED'
  const isRunning = d.status === 'RUNNING'
  const borderClass = isFailed ? 'border-red-500 bg-red-500/10' : typeColors[d.type]
  const totalTokens = (d.inputTokens ?? 0) + (d.outputTokens ?? 0)

  return (
    <div
      className={`
        relative px-3 py-2 rounded-lg border-2 min-w-[160px] text-sm
        ${borderClass}
        ${selected ? 'ring-2 ring-white/30' : ''}
        ${isRunning ? 'animate-pulse' : ''}
      `}
    >
      <Handle type="target" position={Position.Top} className="!bg-slate-500" />
      <div className="flex items-center gap-2 mb-1">
        <span className={`text-xs px-1.5 py-0.5 rounded font-medium ${isFailed ? 'bg-red-500/30 text-red-300' : 'bg-slate-700 text-slate-300'}`}>
          {typeLabels[d.type]}
        </span>
        {isFailed && <span className="text-xs text-red-400">FAILED</span>}
        {isRunning && <span className="text-xs text-blue-400">RUNNING</span>}
      </div>
      <div className="text-white font-medium truncate max-w-[200px]">{d.name}</div>
      {d.model && <div className="text-xs text-slate-400 mt-0.5 truncate">{d.model}</div>}
      {totalTokens > 0 && (
        <div className="text-xs text-slate-400 mt-0.5">{totalTokens} tokens</div>
      )}
      <Handle type="source" position={Position.Bottom} className="!bg-slate-500" />
    </div>
  )
})

SpanNode.displayName = 'SpanNode'
