import { useCallback, useMemo } from 'react'
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  addEdge,
  type Node,
  type Edge,
  type Connection,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import dagre from '@dagrejs/dagre'
import { SpanNode } from './SpanNode'
import type { Span } from '../types'

const nodeTypes = { spanNode: SpanNode }

function buildDagreLayout(spans: Span[]): { nodes: Node[]; edges: Edge[] } {
  const g = new dagre.graphlib.Graph()
  g.setGraph({ rankdir: 'TB', nodesep: 60, ranksep: 80 })
  g.setDefaultEdgeLabel(() => ({}))

  const nodeWidth = 220
  const nodeHeight = 80

  spans.forEach((span) => {
    g.setNode(span.id, { width: nodeWidth, height: nodeHeight })
  })

  spans.forEach((span) => {
    if (span.parentSpanId) {
      g.setEdge(span.parentSpanId, span.id)
    }
  })

  dagre.layout(g)

  const nodes: Node[] = spans.map((span) => {
    const pos = g.node(span.id)
    return {
      id: span.id,
      type: 'spanNode',
      position: { x: pos.x - nodeWidth / 2, y: pos.y - nodeHeight / 2 },
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
    .filter((s) => s.parentSpanId)
    .map((span) => ({
      id: `${span.parentSpanId}-${span.id}`,
      source: span.parentSpanId!,
      target: span.id,
      style: { stroke: '#475569' },
    }))

  return { nodes, edges }
}

interface Props {
  spans: Span[]
  onNodeClick: (spanId: string) => void
}

export default function TraceCanvas({ spans, onNodeClick }: Props) {
  const { nodes: initialNodes, edges: initialEdges } = useMemo(() => buildDagreLayout(spans), [spans])
  const [nodes, , onNodesChange] = useNodesState(initialNodes)
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges)

  const onConnect = useCallback((c: Connection) => setEdges((eds) => addEdge(c, eds)), [setEdges])

  return (
    <div className="w-full h-full bg-[#0f172a]">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        nodeTypes={nodeTypes}
        onNodeClick={(_, node) => onNodeClick(node.id)}
        fitView
        colorMode="dark"
      >
        <Background color="#1e293b" />
        <Controls />
        <MiniMap nodeColor="#334155" maskColor="rgba(15,23,42,0.8)" />
      </ReactFlow>
    </div>
  )
}
