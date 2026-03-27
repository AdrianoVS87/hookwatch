import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type EnvProfile = 'production' | 'staging' | 'development'
export type AutoRefresh = 10 | 30 | 60 | 120
export type DefaultRange = '7d' | '30d' | '90d'

export interface CostGuardrails {
  warnCostPerTraceUsd: number
  warnTokensPerTrace: number
  warnErrorRatePercent: number
}

export interface TaggingPolicy {
  requiredPrefixes: string[]
  blockedTags: string[]
  suggestFromRecent: boolean
}

export interface Settings {
  _schemaVersion: number
  envProfile: EnvProfile
  autoRefreshSeconds: AutoRefresh
  analyticsDefaultRange: DefaultRange
  showModelMetadata: boolean
  compactDensity: boolean
  relativeTime: boolean
  costGuardrails: CostGuardrails
  taggingPolicy: TaggingPolicy
}

const DEFAULTS: Settings = {
  _schemaVersion: 1,
  envProfile: 'production',
  autoRefreshSeconds: 30,
  analyticsDefaultRange: '30d',
  showModelMetadata: true,
  compactDensity: false,
  relativeTime: true,
  costGuardrails: {
    warnCostPerTraceUsd: 0.5,
    warnTokensPerTrace: 50000,
    warnErrorRatePercent: 10,
  },
  taggingPolicy: {
    requiredPrefixes: ['env:', 'feature:', 'owner:'],
    blockedTags: [],
    suggestFromRecent: true,
  },
}

function sanitize(raw: unknown): Settings {
  if (!raw || typeof raw !== 'object') return { ...DEFAULTS }
  const r = raw as Record<string, unknown>

  const envProfiles: EnvProfile[] = ['production', 'staging', 'development']
  const autoRefreshOptions: AutoRefresh[] = [10, 30, 60, 120]
  const rangeOptions: DefaultRange[] = ['7d', '30d', '90d']

  const cg = (r.costGuardrails && typeof r.costGuardrails === 'object')
    ? r.costGuardrails as Record<string, unknown>
    : {}
  const tp = (r.taggingPolicy && typeof r.taggingPolicy === 'object')
    ? r.taggingPolicy as Record<string, unknown>
    : {}

  return {
    _schemaVersion: 1,
    envProfile: envProfiles.includes(r.envProfile as EnvProfile)
      ? (r.envProfile as EnvProfile) : DEFAULTS.envProfile,
    autoRefreshSeconds: autoRefreshOptions.includes(r.autoRefreshSeconds as AutoRefresh)
      ? (r.autoRefreshSeconds as AutoRefresh) : DEFAULTS.autoRefreshSeconds,
    analyticsDefaultRange: rangeOptions.includes(r.analyticsDefaultRange as DefaultRange)
      ? (r.analyticsDefaultRange as DefaultRange) : DEFAULTS.analyticsDefaultRange,
    showModelMetadata: typeof r.showModelMetadata === 'boolean' ? r.showModelMetadata : DEFAULTS.showModelMetadata,
    compactDensity: typeof r.compactDensity === 'boolean' ? r.compactDensity : DEFAULTS.compactDensity,
    relativeTime: typeof r.relativeTime === 'boolean' ? r.relativeTime : DEFAULTS.relativeTime,
    costGuardrails: {
      warnCostPerTraceUsd: typeof cg.warnCostPerTraceUsd === 'number' && cg.warnCostPerTraceUsd > 0
        ? cg.warnCostPerTraceUsd : DEFAULTS.costGuardrails.warnCostPerTraceUsd,
      warnTokensPerTrace: typeof cg.warnTokensPerTrace === 'number' && cg.warnTokensPerTrace > 0
        ? cg.warnTokensPerTrace : DEFAULTS.costGuardrails.warnTokensPerTrace,
      warnErrorRatePercent: typeof cg.warnErrorRatePercent === 'number' && cg.warnErrorRatePercent > 0
        ? cg.warnErrorRatePercent : DEFAULTS.costGuardrails.warnErrorRatePercent,
    },
    taggingPolicy: {
      requiredPrefixes: Array.isArray(tp.requiredPrefixes)
        ? (tp.requiredPrefixes as string[]).filter((x) => typeof x === 'string')
        : DEFAULTS.taggingPolicy.requiredPrefixes,
      blockedTags: Array.isArray(tp.blockedTags)
        ? (tp.blockedTags as string[]).filter((x) => typeof x === 'string')
        : DEFAULTS.taggingPolicy.blockedTags,
      suggestFromRecent: typeof tp.suggestFromRecent === 'boolean'
        ? tp.suggestFromRecent : DEFAULTS.taggingPolicy.suggestFromRecent,
    },
  }
}

interface SettingsState {
  settings: Settings
  update: (patch: Partial<Settings>) => void
  updateGuardrails: (patch: Partial<CostGuardrails>) => void
  updateTaggingPolicy: (patch: Partial<TaggingPolicy>) => void
  reset: () => void
}

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      settings: { ...DEFAULTS },
      update: (patch) =>
        set((s) => ({ settings: sanitize({ ...s.settings, ...patch }) })),
      updateGuardrails: (patch) =>
        set((s) => ({
          settings: sanitize({
            ...s.settings,
            costGuardrails: { ...s.settings.costGuardrails, ...patch },
          }),
        })),
      updateTaggingPolicy: (patch) =>
        set((s) => ({
          settings: sanitize({
            ...s.settings,
            taggingPolicy: { ...s.settings.taggingPolicy, ...patch },
          }),
        })),
      reset: () => set({ settings: { ...DEFAULTS } }),
    }),
    {
      name: 'hookwatch-settings-v1',
      onRehydrateStorage: () => (state) => {
        if (state) {
          state.settings = sanitize(state.settings)
        }
      },
    }
  )
)

export { DEFAULTS as SETTINGS_DEFAULTS }
