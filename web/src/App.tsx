import { useState, useEffect, lazy, Suspense } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { LayoutDashboard, GitBranch, GitCompareArrows, Settings, Search, Webhook, ChevronLeft, ChevronRight, BarChart3, AlertTriangle } from 'lucide-react'
import Dashboard from './pages/Dashboard'
import CommandPalette from './components/CommandPalette'
import GlobalContextBar from './components/GlobalContextBar'
import { useUIStore } from './stores/useUIStore'
import './index.css'

const TraceView = lazy(() => import('./pages/TraceView'))
const CompareView = lazy(() => import('./pages/CompareView'))
const AnalyticsView = lazy(() => import('./pages/AnalyticsView'))
const FingerprintView = lazy(() => import('./pages/FingerprintView'))
const SettingsView = lazy(() => import('./pages/Settings'))

type Page = 'dashboard' | 'traces' | 'analytics' | 'fingerprints' | 'compare' | 'settings'

const NAV = [
  { id: 'dashboard' as Page, label: 'Dashboard', icon: LayoutDashboard },
  { id: 'traces'    as Page, label: 'Traces',    icon: GitBranch },
  { id: 'analytics' as Page, label: 'Analytics', icon: BarChart3 },
  { id: 'fingerprints' as Page, label: 'Fingerprints', icon: AlertTriangle },
  { id: 'compare'   as Page, label: 'Compare',   icon: GitCompareArrows },
  { id: 'settings'  as Page, label: 'Settings',  icon: Settings },
]

export default function App() {
  const [page, setPage] = useState<Page>('dashboard')
  const openCommandPalette = useUIStore((s) => s.openCommandPalette)
  const [collapsed, setCollapsed] = useState(window.innerWidth < 1024)

  useEffect(() => {
    const handler = () => setCollapsed(window.innerWidth < 1024)
    window.addEventListener('resize', handler)
    return () => window.removeEventListener('resize', handler)
  }, [])

  const sidebarWidth = collapsed ? 52 : 220

  return (
    <div style={{ display: 'flex', height: '100vh', background: 'var(--bg)', minWidth: 360 }}>
      {/* Sidebar */}
      <aside style={{
        width: sidebarWidth,
        background: 'var(--surface)',
        borderRight: '1px solid var(--border)',
        display: 'flex',
        flexDirection: 'column',
        padding: collapsed ? '20px 6px' : '20px 12px',
        gap: 2,
        flexShrink: 0,
        transition: 'width var(--transition), padding var(--transition)',
        overflow: 'hidden',
      }}>
        {/* Logo */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: 10,
          padding: collapsed ? '4px 0' : '4px 8px',
          marginBottom: 20, justifyContent: collapsed ? 'center' : 'flex-start',
        }}>
          <div style={{
            width: 28, height: 28, background: 'var(--accent)',
            borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center',
            flexShrink: 0,
          }}>
            <Webhook size={14} color="white" strokeWidth={2} />
          </div>
          {!collapsed && (
            <span style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-primary)', letterSpacing: '-0.01em', whiteSpace: 'nowrap' }}>
              HookWatch
            </span>
          )}
        </div>

        {/* Search button */}
        {!collapsed && (
          <button
            onClick={openCommandPalette}
            style={{
              display: 'flex', alignItems: 'center', gap: 9,
              padding: '7px 10px', marginBottom: 8,
              background: 'var(--surface-2)', border: '1px solid var(--border)',
              borderRadius: 6, cursor: 'pointer', width: '100%',
              color: 'var(--text-tertiary)', fontSize: 12,
            }}
          >
            <Search size={14} strokeWidth={1.5} style={{ flexShrink: 0 }} />
            <span style={{ flex: 1, textAlign: 'left' }}>Search…</span>
            <kbd style={{
              fontSize: 10, background: 'rgba(255,255,255,0.05)',
              border: '1px solid var(--border)', borderRadius: 4,
              padding: '1px 5px', color: 'var(--text-tertiary)',
            }}>⌘K</kbd>
          </button>
        )}

        {/* Collapsed search icon */}
        {collapsed && (
          <button
            onClick={openCommandPalette}
            style={{
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              padding: 7, marginBottom: 8,
              background: 'var(--surface-2)', border: '1px solid var(--border)',
              borderRadius: 6, cursor: 'pointer', width: '100%',
              color: 'var(--text-tertiary)',
            }}
          >
            <Search size={14} strokeWidth={1.5} />
          </button>
        )}

        {/* Nav items */}
        <nav style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
          {NAV.map(({ id, label, icon: Icon }) => {
            const active = page === id
            return (
              <button
                key={id}
                onClick={() => setPage(id)}
                title={collapsed ? label : undefined}
                style={{
                  display: 'flex', alignItems: 'center', gap: 9,
                  padding: collapsed ? '7px 0' : '7px 10px',
                  justifyContent: collapsed ? 'center' : 'flex-start',
                  borderRadius: 6, border: 'none',
                  cursor: 'pointer', width: '100%', textAlign: 'left',
                  background: active ? 'rgba(99,102,241,0.12)' : 'transparent',
                  color: active ? 'var(--accent-hover)' : 'var(--text-secondary)',
                  fontSize: 13, fontWeight: active ? 500 : 400,
                  transition: 'all 0.15s ease',
                }}
                onMouseEnter={(e) => { if (!active) (e.currentTarget as HTMLButtonElement).style.background = 'var(--surface-2)' }}
                onMouseLeave={(e) => { if (!active) (e.currentTarget as HTMLButtonElement).style.background = 'transparent' }}
              >
                <Icon size={14} strokeWidth={1.5} style={{ flexShrink: 0 }} />
                {!collapsed && label}
              </button>
            )
          })}
        </nav>

        {/* Toggle + version at bottom */}
        <div style={{ marginTop: 'auto', display: 'flex', flexDirection: 'column', gap: 8, alignItems: collapsed ? 'center' : 'flex-start' }}>
          <button
            onClick={() => setCollapsed(c => !c)}
            style={{
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              width: 28, height: 28, borderRadius: 6,
              background: 'var(--surface-2)', border: '1px solid var(--border)',
              cursor: 'pointer', color: 'var(--text-tertiary)',
              transition: 'background var(--transition)',
            }}
            onMouseEnter={(e) => { (e.currentTarget as HTMLButtonElement).style.background = 'rgba(255,255,255,0.06)' }}
            onMouseLeave={(e) => { (e.currentTarget as HTMLButtonElement).style.background = 'var(--surface-2)' }}
          >
            {collapsed ? <ChevronRight size={13} strokeWidth={1.5} /> : <ChevronLeft size={13} strokeWidth={1.5} />}
          </button>
          {!collapsed && (
            <span style={{ fontSize: 11, color: 'var(--text-tertiary)', padding: '0 8px' }}>v0.1.0-alpha</span>
          )}
        </div>
      </aside>

      {/* Main content */}
      <main style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', position: 'relative' }}>
        {/* Global context bar — shown on all data pages */}
        {page !== 'settings' && <GlobalContextBar />}

        <div style={{ flex: 1, overflow: 'auto' }}>
        <AnimatePresence mode="wait">
          <motion.div
            key={page}
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15, ease: 'easeOut' }}
            style={{ height: '100%' }}
          >
            {page === 'dashboard' && <Dashboard onCompare={() => setPage('compare')} />}
            {page === 'traces' && (
              <Suspense fallback={<div style={{ padding: 40, color: 'var(--text-tertiary)' }}>Loading…</div>}>
                <TraceView />
              </Suspense>
            )}
            {page === 'analytics' && (
              <Suspense fallback={<div style={{ padding: 40, color: 'var(--text-tertiary)' }}>Loading…</div>}>
                <AnalyticsView />
              </Suspense>
            )}
            {page === 'fingerprints' && (
              <Suspense fallback={<div style={{ padding: 40, color: 'var(--text-tertiary)' }}>Loading…</div>}>
                <FingerprintView />
              </Suspense>
            )}
            {page === 'compare' && (
              <Suspense fallback={<div style={{ padding: 40, color: 'var(--text-tertiary)' }}>Loading…</div>}>
                <CompareView onBack={() => setPage('dashboard')} />
              </Suspense>
            )}
            {page === 'settings' && (
              <Suspense fallback={<div style={{ padding: 40, color: 'var(--text-tertiary)' }}>Loading…</div>}>
                <SettingsView />
              </Suspense>
            )}
          </motion.div>
        </AnimatePresence>
        </div>
      </main>

      <CommandPalette />
    </div>
  )
}
