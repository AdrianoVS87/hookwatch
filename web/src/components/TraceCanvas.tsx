import { useMemo } from 'react'
import {
  ReactFlow,
  Background,
  Controls,
  BackgroundVariant,
  type Node,
  type Edge,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { SpanNode } from './SpanNode'
import type { Span } from '../types'
import type { SpanNodeData } from './SpanNode'

const nodeTypes = { spanNode: SpanNode }

interface Props {
  spans: Span[]
  onNodeClick: (id: string) => void
}

export default function TraceCanvas({ spans, onNodeClick }: Props) {
  const { nodes, edges } = useMemo(() => {
    const NODE_WIDTH = 220
    const NODE_HEIGHT = 80
    const H_GAP = 60
    const V_GAP = 60

    // Build a map for quick lookup
    const spanMap = new Map(spans.map(s => [s.id, s]))

    // Group spans by depth (BFS from roots)
    const depth = new Map<string, number>()
    const roots = spans.filter(s => !s.parentId || !spanMap.has(s.parentId))
    const queue = [...roots.map(s => ({ id: s.id, d: 0 }))]
    while (queue.length > 0) {
      const item = queue.shift()!
      depth.set(item.id, item.d)
      spans
        .filter(s => s.parentId === item.id)
        .forEach(s => queue.push({ id: s.id, d: item.d + 1 }))
    }

    // Group by depth level
    const levels = new Map<number, string[]>()
    spans.forEach(s => {
      const d = depth.get(s.id) ?? 0
      if (!levels.has(d)) levels.set(d, [])
      levels.get(d)!.push(s.id)
    })

    // Position nodes
    const nodes: Node<SpanNodeData>[] = spans.map(span => {
      const d = depth.get(span.id) ?? 0
      const levelSpans = levels.get(d) ?? []
      const idx = levelSpans.indexOf(span.id)
      const totalWidth = levelSpans.length * (NODE_WIDTH + H_GAP) - H_GAP
      const x = idx * (NODE_WIDTH + H_GAP) - totalWidth / 2

      return {
        id: span.id,
        type: 'spanNode',
        position: { x, y: d * (NODE_HEIGHT + V_GAP) },
        data: {
          name: span.name,
          type: span.type,
          status: span.status,
          model: span.model,
          inputTokens: span.inputTokens,
          outputTokens: span.outputTokens,
        },
      }
    })

    const edges: Edge[] = spans
      .filter(s => s.parentId && spanMap.has(s.parentId))
      .map(s => ({
        id: `${s.parentId}-${s.id}`,
        source: s.parentId!,
        target: s.id,
        style: { stroke: 'rgba(255,255,255,0.12)', strokeWidth: 1 },
        animated: s.status === 'RUNNING',
      }))

    return { nodes, edges }
  }, [spans])

  return (
    <ReactFlow
      nodes={nodes}
      edges={edges}
      nodeTypes={nodeTypes}
      onNodeClick={(_, node) => onNodeClick(node.id)}
      fitView
      fitViewOptions={{ padding: 0.3 }}
      proOptions={{ hideAttribution: true }}
    >
      <Background variant={BackgroundVariant.Dots} color="rgba(255,255,255,0.04)" gap={24} size={1} />
      <Controls />
    </ReactFlow>
  )
}
