import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { X } from 'lucide-react'
import type { Span } from '../types'

type Tab = 'overview' | 'input' | 'output' | 'error'

interface Props {
  span: Span
  onClose: () => void
}

export default function SpanDetail({ span, onClose }: Props) {
  const [tab, setTab] = useState<Tab>('overview')
  const duration = span.completedAt
    ? new Date(span.completedAt).getTime() - new Date(span.startedAt).getTime()
    : null

  const tabs: Tab[] = ['overview', 'input', 'output', 'error']

  return (
    <AnimatePresence>
      <motion.div
        initial={{ x: 320, opacity: 0 }}
        animate={{ x: 0, opacity: 1 }}
        exit={{ x: 320, opacity: 0 }}
        className="w-80 bg-slate-900 border-l border-slate-700 flex flex-col h-full overflow-hidden"
      >
        <div className="flex items-center justify-between px-4 py-3 border-b border-slate-700">
          <h3 className="text-white font-semibold text-sm truncate">{span.name}</h3>
          <button onClick={onClose} className="text-slate-400 hover:text-white">
            <X size={16} />
          </button>
        </div>

        <div className="flex border-b border-slate-700">
          {tabs.map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`flex-1 py-2 text-xs capitalize ${
                tab === t ? 'text-indigo-400 border-b-2 border-indigo-400' : 'text-slate-400 hover:text-white'
              }`}
            >
              {t}
            </button>
          ))}
        </div>

        <div className="flex-1 overflow-auto p-4 text-sm">
          {tab === 'overview' && (
            <dl className="space-y-3">
              <Row label="Type" value={span.type} />
              <Row label="Status" value={span.status} />
              {span.model && <Row label="Model" value={span.model} />}
              {span.inputTokens != null && <Row label="Input Tokens" value={String(span.inputTokens)} />}
              {span.outputTokens != null && <Row label="Output Tokens" value={String(span.outputTokens)} />}
              {span.cost != null && <Row label="Cost" value={`$${span.cost.toFixed(6)}`} />}
              {duration != null && <Row label="Duration" value={`${duration}ms`} />}
            </dl>
          )}
          {tab === 'input' && <Pre text={span.input} />}
          {tab === 'output' && <Pre text={span.output} />}
          {tab === 'error' && <Pre text={span.error} className="text-red-400" />}
        </div>
      </motion.div>
    </AnimatePresence>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs text-slate-400 uppercase tracking-wide">{label}</dt>
      <dd className="text-white mt-0.5">{value}</dd>
    </div>
  )
}

function Pre({ text, className = 'text-slate-300' }: { text: string | null; className?: string }) {
  if (!text) return <p className="text-slate-500 italic">No data</p>
  return (
    <pre className={`whitespace-pre-wrap break-words text-xs font-mono ${className}`}>{text}</pre>
  )
}
