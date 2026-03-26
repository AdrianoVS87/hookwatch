import { useState } from 'react'

export type DateRange = {
  from: string
  to: string
  preset: '7d' | '30d' | '90d' | 'custom'
}

function toDateStr(d: Date): string {
  return d.toISOString().split('T')[0]
}

function presetRange(preset: '7d' | '30d' | '90d'): { from: string; to: string } {
  const to = new Date()
  const from = new Date()
  const days = preset === '7d' ? 7 : preset === '30d' ? 30 : 90
  from.setDate(from.getDate() - days)
  return { from: toDateStr(from), to: toDateStr(to) }
}

interface Props {
  value: DateRange
  onChange: (range: DateRange) => void
}

const PRESETS: Array<{ id: '7d' | '30d' | '90d'; label: string }> = [
  { id: '7d', label: 'Last 7d' },
  { id: '30d', label: 'Last 30d' },
  { id: '90d', label: 'Last 90d' },
]

export default function DateRangePicker({ value, onChange }: Props) {
  const [customFrom, setCustomFrom] = useState(value.from)
  const [customTo, setCustomTo] = useState(value.to)

  const handlePreset = (preset: '7d' | '30d' | '90d') => {
    const range = presetRange(preset)
    onChange({ ...range, preset })
  }

  const handleCustomApply = () => {
    if (customFrom && customTo) {
      onChange({ from: customFrom, to: customTo, preset: 'custom' })
    }
  }

  const inputStyle: React.CSSProperties = {
    background: 'var(--surface-2)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-md)',
    color: 'var(--text-primary)',
    fontSize: 12,
    padding: '5px 8px',
    outline: 'none',
    colorScheme: 'dark',
  }

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      gap: 6,
      flexWrap: 'wrap',
    }}>
      {PRESETS.map(({ id, label }) => {
        const active = value.preset === id
        return (
          <button
            key={id}
            onClick={() => handlePreset(id)}
            style={{
              padding: '5px 12px',
              borderRadius: 'var(--radius-md)',
              border: active ? '1px solid rgba(99,102,241,0.4)' : '1px solid var(--border)',
              background: active ? 'rgba(99,102,241,0.12)' : 'var(--surface-2)',
              color: active ? 'var(--accent-hover)' : 'var(--text-secondary)',
              fontSize: 12,
              fontWeight: active ? 500 : 400,
              cursor: 'pointer',
              transition: 'all var(--transition)',
            }}
          >
            {label}
          </button>
        )
      })}

      <button
        onClick={() => onChange({ from: customFrom, to: customTo, preset: 'custom' })}
        style={{
          padding: '5px 12px',
          borderRadius: 'var(--radius-md)',
          border: value.preset === 'custom' ? '1px solid rgba(99,102,241,0.4)' : '1px solid var(--border)',
          background: value.preset === 'custom' ? 'rgba(99,102,241,0.12)' : 'var(--surface-2)',
          color: value.preset === 'custom' ? 'var(--accent-hover)' : 'var(--text-secondary)',
          fontSize: 12,
          cursor: 'pointer',
          transition: 'all var(--transition)',
        }}
      >
        Custom
      </button>

      {value.preset === 'custom' && (
        <>
          <input
            type="date"
            value={customFrom}
            onChange={(e) => setCustomFrom(e.target.value)}
            style={inputStyle}
          />
          <span style={{ color: 'var(--text-tertiary)', fontSize: 12 }}>→</span>
          <input
            type="date"
            value={customTo}
            onChange={(e) => setCustomTo(e.target.value)}
            style={inputStyle}
          />
          <button
            onClick={handleCustomApply}
            style={{
              padding: '5px 10px',
              borderRadius: 'var(--radius-md)',
              border: '1px solid rgba(99,102,241,0.4)',
              background: 'rgba(99,102,241,0.15)',
              color: 'var(--accent-hover)',
              fontSize: 12,
              cursor: 'pointer',
            }}
          >
            Apply
          </button>
        </>
      )}
    </div>
  )
}

export { presetRange, toDateStr }
