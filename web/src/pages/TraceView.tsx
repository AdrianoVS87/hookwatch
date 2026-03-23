import { useState } from 'react'
import { Activity, ArrowLeft } from 'lucide-react'
import { useTraceStore } from '../stores/useTraceStore'
import TraceCanvas from '../components/TraceCanvas'
import SpanDetail from '../components/SpanDetail'
import type { Span } from '../types'

export default function TraceView() {
  const { selectedTrace, clearTrace } = useTraceStore()
  const [selectedSpan, setSelectedSpan] = useState<Span | null>(null)

  const handleNodeClick = (spanId: string) => {
    const span = selectedTrace?.spans.find((s) => s.id === spanId) ?? null
    setSelectedSpan(span)
  }

  if (!selectedTrace) {
    return (
      <div className="p-8">
        <div className="flex items-center gap-3 mb-4">
          <Activity className="text-indigo-400" size={28} />
          <h2 className="text-2xl font-bold text-white">Trace View</h2>
        </div>
        <p className="text-slate-500">Select a trace from the Dashboard to view its span graph.</p>
      </div>
    )
  }

  return (
    <div className="flex flex-col h-full">
      <div className="flex items-center gap-3 px-6 py-4 border-b border-slate-800">
        <button onClick={clearTrace} className="text-slate-400 hover:text-white">
          <ArrowLeft size={18} />
        </button>
        <Activity className="text-indigo-400" size={20} />
        <span className="text-white font-medium">Trace {selectedTrace.id.slice(0, 8)}…</span>
        <span className={`ml-2 px-2 py-0.5 text-xs rounded ${
          selectedTrace.status === 'COMPLETED' ? 'bg-green-500/20 text-green-400'
          : selectedTrace.status === 'FAILED' ? 'bg-red-500/20 text-red-400'
          : 'bg-blue-500/20 text-blue-400'
        }`}>{selectedTrace.status}</span>
      </div>

      <div className="flex flex-1 overflow-hidden">
        <div className="flex-1">
          {selectedTrace.spans.length === 0 ? (
            <div className="flex items-center justify-center h-full text-slate-500">No spans</div>
          ) : (
            <TraceCanvas spans={selectedTrace.spans} onNodeClick={handleNodeClick} />
          )}
        </div>
        {selectedSpan && (
          <SpanDetail span={selectedSpan} onClose={() => setSelectedSpan(null)} />
        )}
      </div>
    </div>
  )
}
