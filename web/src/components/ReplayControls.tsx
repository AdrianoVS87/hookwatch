import { useState, useEffect, useCallback } from 'react'
import { Play, Pause, SkipBack } from 'lucide-react'
import type { Span } from '../types'

interface Props {
  spans: Span[]
  onFrame: (visibleSpans: Span[]) => void
}

/**
 * Replay slider that scrubs through spans in temporal order.
 * Calls onFrame with the subset of spans visible at the current frame.
 */
export default function ReplayControls({ spans, onFrame }: Props) {
  const sorted = [...spans].sort(
    (a, b) => new Date(a.startedAt).getTime() - new Date(b.startedAt).getTime()
  )

  const [frame, setFrame] = useState(sorted.length)
  const [playing, setPlaying] = useState(false)
  const [speed, setSpeed] = useState(1)

  const emitFrame = useCallback(
    (f: number) => {
      onFrame(sorted.slice(0, f))
    },
    [sorted, onFrame]
  )

  useEffect(() => {
    emitFrame(frame)
  }, [frame, emitFrame])

  // Auto-advance when playing
  useEffect(() => {
    if (!playing) return
    const interval = setInterval(() => {
      setFrame((f) => {
        if (f >= sorted.length) {
          setPlaying(false)
          return f
        }
        return f + 1
      })
    }, 800 / speed)
    return () => clearInterval(interval)
  }, [playing, speed, sorted.length])

  const reset = () => {
    setFrame(0)
    setPlaying(false)
  }

  return (
    <div className="flex items-center gap-3 px-4 py-3 bg-slate-800 border-t border-slate-700">
      <button onClick={reset} className="text-slate-400 hover:text-white transition-colors">
        <SkipBack size={16} />
      </button>
      <button
        onClick={() => setPlaying((p) => !p)}
        className="text-indigo-400 hover:text-indigo-300 transition-colors"
      >
        {playing ? <Pause size={20} /> : <Play size={20} />}
      </button>
      <input
        type="range"
        min={0}
        max={sorted.length}
        value={frame}
        onChange={(e) => {
          setPlaying(false)
          setFrame(Number(e.target.value))
        }}
        className="flex-1 accent-indigo-500"
      />
      <span className="text-slate-400 text-xs w-20 text-right shrink-0">
        {frame}/{sorted.length} spans
      </span>
      <select
        value={speed}
        onChange={(e) => setSpeed(Number(e.target.value))}
        className="bg-slate-700 text-slate-300 text-xs rounded px-2 py-1 border border-slate-600"
      >
        <option value={0.5}>0.5×</option>
        <option value={1}>1×</option>
        <option value={2}>2×</option>
        <option value={4}>4×</option>
      </select>
    </div>
  )
}
