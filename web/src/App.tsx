import { useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { LayoutDashboard, GitBranch, Settings, Search, Webhook } from 'lucide-react'
import Dashboard from './pages/Dashboard'
import TraceView from './pages/TraceView'
import CommandPalette from './components/CommandPalette'
import { useUIStore } from './stores/useUIStore'
import './index.css'

type Page = 'dashboard' | 'traces' | 'settings'

const NAV = [
  { id: 'dashboard' as Page, label: 'Dashboard', icon: LayoutDashboard },
  { id: 'traces'    as Page, label: 'Traces',    icon: GitBranch },
  { id: 'settings'  as Page, label: 'Settings',  icon: Settings },
]

export default function App() {
  const [page, setPage] = useState<Page>('dashboard')
  const openCommandPalette = useUIStore((s) => s.openCommandPalette)

  return (
    <div style={{ display: 'flex', height: '100vh', background: 'var(--bg)', minWidth: 1100 }}>
      {/* Sidebar */}
      <aside style={{
        width: 220,
        background: 'var(--surface)',
        borderRight: '1px solid var(--border)',
        display: 'flex',
        flexDirection: 'column',
        padding: '20px 12px',
        gap: 2,
        flexShrink: 0,
      }}>
        {/* Logo */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '4px 8px', marginBottom: 20 }}>
          <div style={{
            width: 28, height: 28, background: 'var(--accent)',
            borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <Webhook size={14} color="white" strokeWidth={2} />
          </div>
          <span style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-primary)', letterSpacing: '-0.01em' }}>
            HookWatch
          </span>
        </div>

        {/* Search button */}
        <button
          onClick={openCommandPalette}
          style={{
            display: 'flex', alignItems: 'center', gap: 8,
            padding: '7px 10px', marginBottom: 8,
            background: 'var(--surface-2)', border: '1px solid var(--border)',
            borderRadius: 6, cursor: 'pointer', width: '100%',
            color: 'var(--text-tertiary)', fontSize: 12,
          }}
        >
          <Search size={12} strokeWidth={1.5} />
          <span style={{ flex: 1, textAlign: 'left' }}>Search…</span>
          <kbd style={{
            fontSize: 10, background: 'rgba(255,255,255,0.05)',
            border: '1px solid var(--border)', borderRadius: 4,
            padding: '1px 5px', color: 'var(--text-tertiary)',
          }}>⌘K</kbd>
        </button>

        {/* Nav items */}
        <nav style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
          {NAV.map(({ id, label, icon: Icon }) => {
            const active = page === id
            return (
              <button
                key={id}
                onClick={() => setPage(id)}
                style={{
                  display: 'flex', alignItems: 'center', gap: 9,
                  padding: '7px 10px', borderRadius: 6, border: 'none',
                  cursor: 'pointer', width: '100%', textAlign: 'left',
                  background: active ? 'rgba(99,102,241,0.12)' : 'transparent',
                  color: active ? 'var(--accent-hover)' : 'var(--text-secondary)',
                  fontSize: 13, fontWeight: active ? 500 : 400,
                  transition: 'all 0.15s ease',
                }}
                onMouseEnter={(e) => { if (!active) (e.currentTarget as HTMLButtonElement).style.background = 'var(--surface-2)' }}
                onMouseLeave={(e) => { if (!active) (e.currentTarget as HTMLButtonElement).style.background = 'transparent' }}
              >
                <Icon size={14} strokeWidth={1.5} />
                {label}
              </button>
            )
          })}
        </nav>

        {/* Version tag at bottom */}
        <div style={{ marginTop: 'auto', padding: '0 8px' }}>
          <span style={{ fontSize: 11, color: 'var(--text-tertiary)' }}>v0.1.0-alpha</span>
        </div>
      </aside>

      {/* Main content */}
      <main style={{ flex: 1, overflow: 'auto', position: 'relative' }}>
        <AnimatePresence mode="wait">
          <motion.div
            key={page}
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15, ease: 'easeOut' }}
            style={{ height: '100%' }}
          >
            {page === 'dashboard' && <Dashboard />}
            {page === 'traces'    && <TraceView />}
            {page === 'settings'  && <SettingsPage />}
          </motion.div>
        </AnimatePresence>
      </main>

      <CommandPalette />
    </div>
  )
}

function SettingsPage() {
  return (
    <div style={{ padding: '40px 48px' }}>
      <h1 style={{ fontSize: 20, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 8 }}>Settings</h1>
      <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>Configuration coming soon.</p>
    </div>
  )
}
