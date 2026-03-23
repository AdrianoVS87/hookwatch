import { useState } from 'react'
import { motion } from 'framer-motion'
import type { Trace, TraceStatus } from '../types'

interface Props {
  traces: Trace[]
  onSelect: (id: string) => void
}

type SortKey = 'status' | 'totalTokens' | 'totalCost' | 'startedAt'

const statusColors: Record<TraceStatus, string> = {
  COMPLETED: 'bg-green-500/20 text-green-400',
  RUNNING: 'bg-blue-500/20 text-blue-400',
  FAILED: 'bg-red-500/20 text-red-400',
}

function durationMs(trace: Trace): number {
  if (!trace.completedAt) return 0
  return new Date(trace.completedAt).getTime() - new Date(trace.startedAt).getTime()
}

export default function TraceTable({ traces, onSelect }: Props) {
  const [sortKey, setSortKey] = useState<SortKey>('startedAt')
  const [sortAsc, setSortAsc] = useState(false)

  const sorted = [...traces].sort((a, b) => {
    let av: number | string = 0
    let bv: number | string = 0
    if (sortKey === 'status') { av = a.status; bv = b.status }
    else if (sortKey === 'totalTokens') { av = a.totalTokens ?? 0; bv = b.totalTokens ?? 0 }
    else if (sortKey === 'totalCost') { av = a.totalCost ?? 0; bv = b.totalCost ?? 0 }
    else if (sortKey === 'startedAt') { av = a.startedAt; bv = b.startedAt }
    if (av < bv) return sortAsc ? -1 : 1
    if (av > bv) return sortAsc ? 1 : -1
    return 0
  })

  const toggle = (key: SortKey) => {
    if (sortKey === key) setSortAsc((p) => !p)
    else { setSortKey(key); setSortAsc(true) }
  }

  const th = (label: string, key: SortKey) => (
    <th
      className="px-4 py-3 text-left text-xs uppercase tracking-wide text-slate-400 cursor-pointer hover:text-white select-none"
      onClick={() => toggle(key)}
    >
      {label} {sortKey === key ? (sortAsc ? '↑' : '↓') : ''}
    </th>
  )

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead className="border-b border-slate-700">
          <tr>
            {th('Status', 'status')}
            <th className="px-4 py-3 text-left text-xs uppercase tracking-wide text-slate-400">Spans</th>
            {th('Tokens', 'totalTokens')}
            {th('Cost', 'totalCost')}
            <th className="px-4 py-3 text-left text-xs uppercase tracking-wide text-slate-400">Duration</th>
            {th('Started', 'startedAt')}
          </tr>
        </thead>
        <tbody>
          {sorted.map((trace) => (
            <motion.tr
              key={trace.id}
              initial={{ opacity: 0, y: 4 }}
              animate={{ opacity: 1, y: 0 }}
              className="border-b border-slate-800 hover:bg-slate-800/50 cursor-pointer"
              onClick={() => onSelect(trace.id)}
            >
              <td className="px-4 py-3">
                <span className={`px-2 py-0.5 rounded text-xs font-medium ${statusColors[trace.status]}`}>
                  {trace.status}
                </span>
              </td>
              <td className="px-4 py-3 text-slate-300">{trace.spans.length}</td>
              <td className="px-4 py-3 text-slate-300">{trace.totalTokens ?? '—'}</td>
              <td className="px-4 py-3 text-slate-300">
                {trace.totalCost != null ? `$${trace.totalCost.toFixed(4)}` : '—'}
              </td>
              <td className="px-4 py-3 text-slate-300">
                {trace.completedAt ? `${durationMs(trace)}ms` : '—'}
              </td>
              <td className="px-4 py-3 text-slate-400">
                {new Date(trace.startedAt).toLocaleString()}
              </td>
            </motion.tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
