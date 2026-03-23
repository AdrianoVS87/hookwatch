import { useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { LayoutDashboard, Activity, Settings2, Search } from 'lucide-react'
import Dashboard from './pages/Dashboard'
import TraceView from './pages/TraceView'
import Settings from './pages/Settings'
import CommandPalette from './components/CommandPalette'
import { useUIStore } from './stores/useUIStore'
import './index.css'

type Page = 'dashboard' | 'traces' | 'settings'

const NAV: { id: Page; label: string; icon: typeof LayoutDashboard }[] = [
  { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { id: 'traces',    label: 'Traces',    icon: Activity },
  { id: 'settings',  label: 'Settings',  icon: Settings2 },
]

export default function App() {
  const [page, setPage] = useState<Page>('dashboard')
  const openCommandPalette = useUIStore((s) => s.openCommandPalette)

  return (
    <div className="flex h-screen bg-[#0f172a] min-w-[1280px]">
      <aside className="w-56 bg-slate-900 border-r border-slate-800 flex flex-col p-4 gap-1 shrink-0">
        <div className="text-indigo-400 font-bold text-xl mb-4 px-2">🪝 HookWatch</div>
        <button
          onClick={openCommandPalette}
          className="flex items-center gap-2 px-3 py-2 mb-2 text-slate-400 hover:text-white bg-slate-800 hover:bg-slate-700 rounded-lg text-xs transition-colors"
        >
          <Search size={12} />
          Search…
          <kbd className="ml-auto text-xs bg-slate-700 px-1 rounded">⌘K</kbd>
        </button>
        {NAV.map(({ id, label, icon: Icon }) => (
          <button key={id} onClick={() => setPage(id)}
            className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors ${
              page === id ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:bg-slate-800 hover:text-white'
            }`}
          >
            <Icon size={16} />{label}
          </button>
        ))}
      </aside>
      <main className="flex-1 overflow-auto relative">
        <AnimatePresence mode="wait">
          <motion.div
            key={page}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15 }}
            className="h-full"
          >
            {page === 'dashboard' && <Dashboard />}
            {page === 'traces'    && <TraceView />}
            {page === 'settings'  && <Settings />}
          </motion.div>
        </AnimatePresence>
      </main>
      <CommandPalette />
    </div>
  )
}
