export function SkeletonRow() {
  return (
    <div style={{
      display: 'flex', gap: 16, padding: '12px 16px',
      borderBottom: '1px solid var(--border)',
    }}>
      <div className="skeleton" style={{ height: 16, width: 80 }} />
      <div className="skeleton" style={{ height: 16, width: 32 }} />
      <div className="skeleton" style={{ height: 16, width: 64 }} />
      <div className="skeleton" style={{ height: 16, width: 64 }} />
      <div className="skeleton" style={{ height: 16, width: 64 }} />
      <div className="skeleton" style={{ height: 16, flex: 1 }} />
    </div>
  )
}

export function SkeletonMetricsBar() {
  return (
    <div style={{
      display: 'flex', gap: 24, padding: '12px 24px',
      background: 'var(--surface)', borderBottom: '1px solid var(--border)',
    }}>
      {[...Array(5)].map((_, i) => (
        <div key={i} style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <div className="skeleton" style={{ height: 12, width: 64 }} />
          <div className="skeleton" style={{ height: 16, width: 48 }} />
        </div>
      ))}
    </div>
  )
}
