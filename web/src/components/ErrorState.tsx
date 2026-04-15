import { AlertCircle } from 'lucide-react'

interface Props {
  message?: string
  onRetry?: () => void
}

/**
 * Reusable error state with optional retry button.
 * Displayed when an API call fails.
 */
export default function ErrorState({ message = 'Something went wrong', onRetry }: Props) {
  return (
    <div style={{
      display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
      gap: 14, padding: '48px 24px', color: 'var(--text-tertiary)',
    }}>
      <AlertCircle size={28} strokeWidth={1.2} style={{ color: 'var(--error)', opacity: 0.7 }} />
      <p style={{ margin: 0, fontSize: 13, color: 'var(--text-secondary)', fontWeight: 500 }}>
        {message}
      </p>
      {onRetry && (
        <button
          onClick={onRetry}
          style={{
            padding: '7px 18px',
            borderRadius: 'var(--radius-md)',
            border: '1px solid var(--border)',
            background: 'var(--surface-2)',
            color: 'var(--text-primary)',
            fontSize: 12,
            fontWeight: 500,
            cursor: 'pointer',
            transition: 'all var(--transition)',
          }}
          onMouseEnter={(e) => { (e.currentTarget as HTMLButtonElement).style.background = 'var(--surface)' }}
          onMouseLeave={(e) => { (e.currentTarget as HTMLButtonElement).style.background = 'var(--surface-2)' }}
        >
          Retry
        </button>
      )}
    </div>
  )
}
