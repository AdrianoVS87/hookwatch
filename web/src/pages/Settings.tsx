import { useState } from 'react'
import {
  Settings2, Cpu, Eye, ShieldAlert, Tag, Plug, RotateCcw,
  CheckCircle2, AlertCircle, Loader2, ExternalLink,
} from 'lucide-react'
import { useSettingsStore, SETTINGS_DEFAULTS } from '../stores/useSettingsStore'
import type { EnvProfile, AutoRefresh, DefaultRange } from '../stores/useSettingsStore'

// ── design tokens ─────────────────────────────────────────────────────────────

const S = {
  page: {
    padding: '28px 40px',
    display: 'flex',
    flexDirection: 'column' as const,
    gap: 24,
    maxWidth: 720,
  },
  card: {
    background: 'var(--surface)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-lg)',
    padding: '20px 24px',
  },
  sectionTitle: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    marginBottom: 4,
    fontSize: 13,
    fontWeight: 600,
    color: 'var(--text-primary)',
  },
  sectionSub: {
    fontSize: 11,
    color: 'var(--text-tertiary)',
    marginBottom: 16,
  },
  row: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '10px 0',
    borderBottom: '1px solid var(--border)',
  },
  label: {
    fontSize: 12,
    color: 'var(--text-secondary)',
    fontWeight: 500,
  },
  helper: {
    fontSize: 11,
    color: 'var(--text-tertiary)',
    marginTop: 2,
  },
  select: {
    background: 'var(--surface-2)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-md)',
    color: 'var(--text-primary)',
    fontSize: 12,
    padding: '5px 10px',
    cursor: 'pointer',
    outline: 'none',
  },
  input: {
    background: 'var(--surface-2)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-md)',
    color: 'var(--text-primary)',
    fontSize: 12,
    padding: '5px 10px',
    outline: 'none',
    width: 90,
  },
  inputWide: {
    background: 'var(--surface-2)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-md)',
    color: 'var(--text-primary)',
    fontSize: 12,
    padding: '5px 10px',
    outline: 'none',
    width: 220,
  },
}

// ── toggle component ──────────────────────────────────────────────────────────

function Toggle({ value, onChange }: { value: boolean; onChange: (v: boolean) => void }) {
  return (
    <button
      onClick={() => onChange(!value)}
      style={{
        width: 36, height: 20,
        borderRadius: 10,
        background: value ? 'var(--accent)' : 'var(--surface-2)',
        border: '1px solid ' + (value ? 'var(--accent)' : 'var(--border)'),
        cursor: 'pointer',
        position: 'relative',
        transition: 'all var(--transition)',
        flexShrink: 0,
      }}
    >
      <span style={{
        position: 'absolute',
        top: 2, left: value ? 18 : 2,
        width: 14, height: 14,
        borderRadius: 7,
        background: 'white',
        transition: 'left var(--transition)',
      }} />
    </button>
  )
}

// ── section card wrapper ──────────────────────────────────────────────────────

function Section({
  icon, title, subtitle, children,
}: {
  icon: React.ReactNode; title: string; subtitle: string; children: React.ReactNode
}) {
  return (
    <div style={S.card}>
      <div style={S.sectionTitle}>{icon} {title}</div>
      <div style={S.sectionSub}>{subtitle}</div>
      {children}
    </div>
  )
}

function Row({
  label, helper, children,
}: {
  label: string; helper?: string; children: React.ReactNode
}) {
  return (
    <div style={S.row}>
      <div>
        <div style={S.label}>{label}</div>
        {helper && <div style={S.helper}>{helper}</div>}
      </div>
      {children}
    </div>
  )
}

// ── connectivity checker ──────────────────────────────────────────────────────

type ConnStatus = 'idle' | 'checking' | 'ok' | 'error'

interface ConnResult {
  url: string
  label: string
  status: ConnStatus
  latencyMs?: number
  code?: number
}

const ENDPOINTS = [
  { url: 'https://hookwatch.adrianovs.net/api/v1/health', label: 'API Health' },
  { url: 'https://hookwatch.adrianovs.net/api/v1/openapi.json', label: 'OpenAPI Spec' },
  { url: 'https://hookwatch.adrianovs.net/swagger-ui/index.html', label: 'Swagger UI' },
]

function ConnectivityCheck() {
  const [results, setResults] = useState<ConnResult[]>(
    ENDPOINTS.map((e) => ({ ...e, status: 'idle' }))
  )
  const [checking, setChecking] = useState(false)

  const runChecks = async () => {
    setChecking(true)
    setResults(ENDPOINTS.map((e) => ({ ...e, status: 'checking' })))

    const updated = await Promise.all(
      ENDPOINTS.map(async (e): Promise<ConnResult> => {
        const t0 = Date.now()
        try {
          const res = await fetch(e.url, { method: 'GET', mode: 'cors' })
          return { ...e, status: res.ok ? 'ok' : 'error', latencyMs: Date.now() - t0, code: res.status }
        } catch {
          return { ...e, status: 'error', latencyMs: Date.now() - t0 }
        }
      })
    )
    setResults(updated)
    setChecking(false)
  }

  return (
    <div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 14 }}>
        {results.map((r) => (
          <div key={r.url} style={{
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            padding: '8px 12px',
            background: 'var(--surface-2)',
            border: '1px solid var(--border)',
            borderRadius: 'var(--radius-md)',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              {r.status === 'checking' && <Loader2 size={13} color="var(--text-tertiary)" style={{ animation: 'spin 1s linear infinite' }} />}
              {r.status === 'ok' && <CheckCircle2 size={13} color="var(--success)" />}
              {r.status === 'error' && <AlertCircle size={13} color="var(--error)" />}
              {r.status === 'idle' && <span style={{ width: 13, height: 13, borderRadius: '50%', background: 'var(--border)', display: 'inline-block' }} />}
              <span style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{r.label}</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              {r.latencyMs !== undefined && (
                <span style={{ fontSize: 11, color: 'var(--text-tertiary)' }}>{r.latencyMs}ms</span>
              )}
              {r.code !== undefined && (
                <span style={{ fontSize: 11, color: r.status === 'ok' ? 'var(--success)' : 'var(--error)' }}>
                  {r.code}
                </span>
              )}
              <a
                href={r.url} target="_blank" rel="noopener noreferrer"
                style={{ color: 'var(--text-tertiary)', display: 'flex' }}
              >
                <ExternalLink size={11} />
              </a>
            </div>
          </div>
        ))}
      </div>
      <button
        onClick={runChecks}
        disabled={checking}
        style={{
          padding: '7px 14px',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--border)',
          background: checking ? 'var(--surface-2)' : 'var(--accent)',
          color: checking ? 'var(--text-tertiary)' : 'white',
          fontSize: 12, fontWeight: 500,
          cursor: checking ? 'not-allowed' : 'pointer',
        }}
      >
        {checking ? 'Checking…' : 'Run connectivity checks'}
      </button>
    </div>
  )
}

// ── config health badge ───────────────────────────────────────────────────────

function ConfigHealth() {
  const settings = useSettingsStore((s) => s.settings)
  const issues: string[] = []

  if (settings.costGuardrails.warnCostPerTraceUsd <= 0) issues.push('warnCostPerTraceUsd must be > 0')
  if (settings.costGuardrails.warnTokensPerTrace <= 0) issues.push('warnTokensPerTrace must be > 0')
  if (settings.costGuardrails.warnErrorRatePercent <= 0) issues.push('warnErrorRatePercent must be > 0')
  if (settings.taggingPolicy.requiredPrefixes.length === 0) issues.push('No required tag prefixes defined')

  const healthy = issues.length === 0

  return (
    <div style={{
      display: 'flex', alignItems: 'flex-start', gap: 8,
      padding: '10px 14px',
      background: healthy ? 'rgba(16,185,129,0.07)' : 'rgba(245,158,11,0.07)',
      border: `1px solid ${healthy ? 'rgba(16,185,129,0.2)' : 'rgba(245,158,11,0.2)'}`,
      borderRadius: 'var(--radius-md)',
      marginBottom: 16,
    }}>
      {healthy
        ? <CheckCircle2 size={14} color="var(--success)" style={{ marginTop: 1, flexShrink: 0 }} />
        : <AlertCircle size={14} color="var(--warning)" style={{ marginTop: 1, flexShrink: 0 }} />}
      <div>
        <div style={{ fontSize: 12, fontWeight: 600, color: healthy ? 'var(--success)' : 'var(--warning)' }}>
          Config {healthy ? 'healthy' : `has ${issues.length} warning${issues.length > 1 ? 's' : ''}`}
        </div>
        {!healthy && (
          <ul style={{ margin: '4px 0 0', padding: '0 0 0 16px' }}>
            {issues.map((i) => (
              <li key={i} style={{ fontSize: 11, color: 'var(--text-tertiary)' }}>{i}</li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}

// ── main page ─────────────────────────────────────────────────────────────────

export default function Settings() {
  const { settings, update, updateGuardrails, updateTaggingPolicy, reset } = useSettingsStore()
  const [saved, setSaved] = useState(false)
  const [confirmReset, setConfirmReset] = useState(false)

  const handleUpdate = <T,>(key: keyof typeof settings, value: T) => {
    update({ [key]: value } as Partial<typeof settings>)
    setSaved(true)
    setTimeout(() => setSaved(false), 1500)
  }

  const handleReset = () => {
    if (confirmReset) {
      reset()
      setConfirmReset(false)
    } else {
      setConfirmReset(true)
      setTimeout(() => setConfirmReset(false), 3000)
    }
  }

  return (
    <div style={S.page}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <Settings2 size={18} color="var(--accent)" strokeWidth={1.5} />
          <h1 style={{ margin: 0, fontSize: 20, fontWeight: 600, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
            Settings
          </h1>
        </div>
        {saved && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 11, color: 'var(--success)' }}>
            <CheckCircle2 size={12} />
            Saved
          </div>
        )}
      </div>

      {/* Config health */}
      <ConfigHealth />

      {/* A. Runtime Profile */}
      <Section
        icon={<Cpu size={14} color="var(--accent)" />}
        title="Runtime Profile"
        subtitle="Controls environment label, polling frequency, and default time ranges."
      >
        <Row label="Environment" helper="Labels all traces with this environment context">
          <select
            style={S.select}
            value={settings.envProfile}
            onChange={(e) => handleUpdate('envProfile', e.target.value as EnvProfile)}
          >
            <option value="production">Production</option>
            <option value="staging">Staging</option>
            <option value="development">Development</option>
          </select>
        </Row>
        <Row label="Dashboard auto-refresh" helper="How often the trace list refreshes automatically">
          <select
            style={S.select}
            value={settings.autoRefreshSeconds}
            onChange={(e) => handleUpdate('autoRefreshSeconds', Number(e.target.value) as AutoRefresh)}
          >
            <option value={10}>Every 10s</option>
            <option value={30}>Every 30s</option>
            <option value={60}>Every 60s</option>
            <option value={120}>Every 2min</option>
          </select>
        </Row>
        <Row label="Analytics default range" helper="Preselected date range when opening Analytics">
          <select
            style={S.select}
            value={settings.analyticsDefaultRange}
            onChange={(e) => handleUpdate('analyticsDefaultRange', e.target.value as DefaultRange)}
          >
            <option value="7d">Last 7 days</option>
            <option value="30d">Last 30 days</option>
            <option value="90d">Last 90 days</option>
          </select>
        </Row>
      </Section>

      {/* B. Display & UX */}
      <Section
        icon={<Eye size={14} color="var(--accent)" />}
        title="Display & UX"
        subtitle="Visual preferences persisted in your browser."
      >
        <Row label="Show model metadata" helper="Show model name in trace rows">
          <Toggle value={settings.showModelMetadata} onChange={(v) => handleUpdate('showModelMetadata', v)} />
        </Row>
        <Row label="Compact density" helper="Reduce row height in tables">
          <Toggle value={settings.compactDensity} onChange={(v) => handleUpdate('compactDensity', v)} />
        </Row>
        <div style={{ ...S.row, borderBottom: 'none' }}>
          <div>
            <div style={S.label}>Relative timestamps</div>
            <div style={S.helper}>Show "2h ago" instead of absolute ISO dates</div>
          </div>
          <Toggle value={settings.relativeTime} onChange={(v) => handleUpdate('relativeTime', v)} />
        </div>
      </Section>

      {/* C. Cost Guardrails */}
      <Section
        icon={<ShieldAlert size={14} color="var(--accent)" />}
        title="Cost Guardrails"
        subtitle="Thresholds that trigger visual warnings on expensive or anomalous traces."
      >
        <Row label="Warn if cost per trace exceeds" helper="In USD, e.g. 0.50">
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span style={{ fontSize: 12, color: 'var(--text-tertiary)' }}>$</span>
            <input
              type="number"
              style={S.input}
              value={settings.costGuardrails.warnCostPerTraceUsd}
              min={0.001}
              step={0.01}
              onChange={(e) => updateGuardrails({ warnCostPerTraceUsd: parseFloat(e.target.value) || SETTINGS_DEFAULTS.costGuardrails.warnCostPerTraceUsd })}
            />
          </div>
        </Row>
        <Row label="Warn if tokens per trace exceed" helper="e.g. 50000">
          <input
            type="number"
            style={S.input}
            value={settings.costGuardrails.warnTokensPerTrace}
            min={100}
            step={1000}
            onChange={(e) => updateGuardrails({ warnTokensPerTrace: parseInt(e.target.value) || SETTINGS_DEFAULTS.costGuardrails.warnTokensPerTrace })}
          />
        </Row>
        <div style={{ ...S.row, borderBottom: 'none' }}>
          <div>
            <div style={S.label}>Warn if error rate exceeds</div>
            <div style={S.helper}>Percentage, e.g. 10 = 10%</div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <input
              type="number"
              style={S.input}
              value={settings.costGuardrails.warnErrorRatePercent}
              min={0.1}
              max={100}
              step={1}
              onChange={(e) => updateGuardrails({ warnErrorRatePercent: parseFloat(e.target.value) || SETTINGS_DEFAULTS.costGuardrails.warnErrorRatePercent })}
            />
            <span style={{ fontSize: 12, color: 'var(--text-tertiary)' }}>%</span>
          </div>
        </div>
      </Section>

      {/* D. Tagging Policy */}
      <Section
        icon={<Tag size={14} color="var(--accent)" />}
        title="Tagging Policy"
        subtitle="Enforce tag naming conventions across all agents. Mirrors LangSmith and Langfuse best practices."
      >
        <Row
          label="Required tag prefixes"
          helper={`Comma-separated. Warn if trace has no tag matching one of these. Example: env:, feature:, owner:`}
        >
          <input
            type="text"
            style={S.inputWide}
            value={settings.taggingPolicy.requiredPrefixes.join(', ')}
            onChange={(e) =>
              updateTaggingPolicy({
                requiredPrefixes: e.target.value.split(',').map((s) => s.trim()).filter(Boolean),
              })
            }
          />
        </Row>
        <Row label="Blocked tags" helper="Comma-separated. These tags will be stripped on ingest.">
          <input
            type="text"
            style={S.inputWide}
            value={settings.taggingPolicy.blockedTags.join(', ')}
            placeholder="e.g. test, tmp, debug"
            onChange={(e) =>
              updateTaggingPolicy({
                blockedTags: e.target.value.split(',').map((s) => s.trim()).filter(Boolean),
              })
            }
          />
        </Row>
        <div style={{ ...S.row, borderBottom: 'none' }}>
          <div>
            <div style={S.label}>Suggest from recent traces</div>
            <div style={S.helper}>Autocomplete tag chips from tags used in the last 7 days</div>
          </div>
          <Toggle
            value={settings.taggingPolicy.suggestFromRecent}
            onChange={(v) => updateTaggingPolicy({ suggestFromRecent: v })}
          />
        </div>
      </Section>

      {/* E. Integrations & API */}
      <Section
        icon={<Plug size={14} color="var(--accent)" />}
        title="Integrations & API"
        subtitle="Verify backend connectivity and access API documentation."
      >
        <div style={{ marginBottom: 16, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {[
            { label: 'Swagger UI', url: 'https://hookwatch.adrianovs.net/swagger-ui/index.html' },
            { label: 'OpenAPI JSON', url: 'https://hookwatch.adrianovs.net/api/v1/openapi.json' },
          ].map((link) => (
            <a
              key={link.url}
              href={link.url}
              target="_blank"
              rel="noopener noreferrer"
              style={{
                display: 'inline-flex', alignItems: 'center', gap: 5,
                padding: '5px 12px',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border)',
                background: 'var(--surface-2)',
                color: 'var(--accent-hover)',
                fontSize: 12, textDecoration: 'none',
              }}
            >
              {link.label} <ExternalLink size={11} />
            </a>
          ))}
        </div>
        <ConnectivityCheck />
      </Section>

      {/* F. Danger zone */}
      <Section
        icon={<RotateCcw size={14} color="var(--error)" />}
        title="Reset"
        subtitle="Restore all settings to factory defaults. This only affects local browser storage."
      >
        <button
          onClick={handleReset}
          style={{
            padding: '7px 16px',
            borderRadius: 'var(--radius-md)',
            border: `1px solid ${confirmReset ? 'var(--error)' : 'var(--border)'}`,
            background: confirmReset ? 'rgba(239,68,68,0.1)' : 'var(--surface-2)',
            color: confirmReset ? 'var(--error)' : 'var(--text-secondary)',
            fontSize: 12, fontWeight: 500,
            cursor: 'pointer',
            transition: 'all var(--transition)',
          }}
        >
          {confirmReset ? 'Click again to confirm reset' : 'Reset to defaults'}
        </button>
      </Section>
    </div>
  )
}
