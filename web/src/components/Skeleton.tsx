export function SkeletonRow() {
  return (
    <div className="flex gap-4 px-4 py-3 border-b border-slate-800 animate-pulse">
      <div className="h-4 bg-slate-700 rounded w-20" />
      <div className="h-4 bg-slate-700 rounded w-8" />
      <div className="h-4 bg-slate-700 rounded w-16" />
      <div className="h-4 bg-slate-700 rounded w-16" />
      <div className="h-4 bg-slate-700 rounded w-16" />
      <div className="h-4 bg-slate-700 rounded flex-1" />
    </div>
  )
}

export function SkeletonMetricsBar() {
  return (
    <div className="flex gap-6 px-6 py-3 bg-slate-800 border-b border-slate-700 animate-pulse">
      {[...Array(5)].map((_, i) => (
        <div key={i} className="flex flex-col gap-1">
          <div className="h-3 bg-slate-700 rounded w-16" />
          <div className="h-4 bg-slate-600 rounded w-12" />
        </div>
      ))}
    </div>
  )
}
