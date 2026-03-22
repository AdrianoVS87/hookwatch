import { Webhook } from 'lucide-react'

export default function Dashboard() {
  return (
    <div className="p-8">
      <div className="flex items-center gap-3 mb-8">
        <Webhook className="text-indigo-400" size={32} />
        <h1 className="text-3xl font-bold text-white">HookWatch</h1>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
          <p className="text-slate-400 text-sm">Total Webhooks</p>
          <p className="text-white text-2xl font-semibold mt-1">—</p>
        </div>
        <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
          <p className="text-slate-400 text-sm">Delivered (24h)</p>
          <p className="text-white text-2xl font-semibold mt-1">—</p>
        </div>
        <div className="bg-slate-800 rounded-xl p-6 border border-slate-700">
          <p className="text-slate-400 text-sm">Failed (24h)</p>
          <p className="text-white text-2xl font-semibold mt-1">—</p>
        </div>
      </div>
    </div>
  )
}
