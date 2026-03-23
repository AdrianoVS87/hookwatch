import { useEffect } from 'react'
import { motion } from 'framer-motion'
import { Webhook } from 'lucide-react'
import { useAgentStore } from '../stores/useAgentStore'
import { useTraceStore } from '../stores/useTraceStore'
import TraceTable from '../components/TraceTable'
import MetricsBar from '../components/MetricsBar'

export default function Dashboard() {
  const { agents, selectedAgentId, loading: agentsLoading, loadAgents, selectAgent } = useAgentStore()
  const { traces, loading: tracesLoading, loadTraces, selectTrace } = useTraceStore()

  useEffect(() => { loadAgents() }, [loadAgents])
  useEffect(() => {
    if (selectedAgentId) loadTraces(selectedAgentId)
  }, [selectedAgentId, loadTraces])

  return (
    <div className="flex flex-col h-full">
      <div className="px-8 py-6 border-b border-slate-800">
        <div className="flex items-center gap-3 mb-4">
          <Webhook className="text-indigo-400" size={28} />
          <h1 className="text-2xl font-bold text-white">Dashboard</h1>
        </div>
        <div className="flex gap-2 flex-wrap">
          {agentsLoading && <span className="text-slate-400 text-sm">Loading agents…</span>}
          {agents.map((agent) => (
            <motion.button
              key={agent.id}
              whileHover={{ scale: 1.02 }}
              onClick={() => selectAgent(agent.id)}
              className={`px-3 py-1.5 rounded-lg text-sm transition-colors ${
                selectedAgentId === agent.id
                  ? 'bg-indigo-600 text-white'
                  : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
              }`}
            >
              {agent.name}
            </motion.button>
          ))}
        </div>
      </div>

      <MetricsBar />

      <div className="flex-1 overflow-auto">
        {tracesLoading && (
          <div className="flex items-center justify-center h-32 text-slate-400">Loading traces…</div>
        )}
        {!tracesLoading && selectedAgentId && traces.length === 0 && (
          <div className="flex items-center justify-center h-32 text-slate-500">No traces found</div>
        )}
        {!tracesLoading && traces.length > 0 && (
          <TraceTable traces={traces} onSelect={selectTrace} />
        )}
        {!selectedAgentId && !agentsLoading && (
          <div className="flex items-center justify-center h-32 text-slate-500">Select an agent above</div>
        )}
      </div>
    </div>
  )
}
