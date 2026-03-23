import { Activity } from 'lucide-react'

export default function TraceView() {
  return (
    <div className="p-8">
      <div className="flex items-center gap-3 mb-8">
        <Activity className="text-indigo-400" size={28} />
        <h2 className="text-2xl font-bold text-white">Trace View</h2>
      </div>
      <div className="bg-slate-800 rounded-xl p-12 border border-slate-700 flex items-center justify-center">
        <p className="text-slate-500">Webhook trace visualization coming soon</p>
      </div>
    </div>
  )
}
