# ADR-0005: React Flow + Dagre for span graph visualization

**Status:** Accepted  
**Date:** 2026-03-23  
**Deciders:** Adriano Viera dos Santos

---

## Context

The core UI value of HookWatch is the **span graph** — a visual representation
of how an AI agent's execution unfolds as a directed acyclic graph (DAG) of
spans. Each span is a node; parent-child relationships are edges.

Requirements:
- Render 2–100 nodes with edges
- Auto-layout (users should not place nodes manually)
- Interactive: click node → open detail panel
- Dark theme, custom node appearance
- TypeScript support

Options evaluated:

| Library | Stars | Auto-layout | Custom nodes | TS | Notes |
|---------|-------|-------------|--------------|-----|-------|
| React Flow (`@xyflow/react`) | 24k | Via dagre/elkjs plugin | ✅ | ✅ | Industry standard for flow diagrams |
| D3.js | 108k | Manual | Manual | Partial | Low-level, high effort |
| Cytoscape.js | 10k | Built-in | ✅ | ✅ | More graph-analysis focused |
| Mermaid | 67k | Built-in | ❌ | N/A | Static rendering, not interactive |

---

## Decision

Use **`@xyflow/react`** (React Flow v12) with **`@dagrejs/dagre`** for automatic
top-to-bottom DAG layout.

Layout algorithm: `rankdir: 'TB'` (top to bottom), `nodesep: 60`, `ranksep: 80`.
Spans without a `parentSpanId` become root nodes; spans with `parentSpanId`
connect as children via directed edges.

Node color coding by `SpanType`:
- `LLM_CALL` → indigo (`#6366F1`) — the primary reasoning operation
- `TOOL_CALL` → emerald (`#10B981`) — external tool invocations
- `RETRIEVAL` → amber (`#F59E0B`) — vector/knowledge base lookups
- `CUSTOM` → slate (`#8B95A1`) — user-defined spans

Status overrides:
- `FAILED` → red border regardless of type (failure is the most important signal)
- `RUNNING` → CSS `animate-pulse` (live execution visible at a glance)

---

## Consequences

- **Positive:** React Flow handles pan/zoom, minimap, edge routing, and node
  selection out of the box. Zero custom canvas code.
- **Positive:** Dagre produces deterministic layouts — same span order = same
  visual graph = reproducible screenshots.
- **Positive:** The `NodeTypes` system allows arbitrarily rich custom node
  components (`SpanNode.tsx`) without fighting the library.
- **Negative:** Bundle size impact: `@xyflow/react` adds ~200KB gzipped.
  Acceptable for a developer tool; could be code-split if needed.
- **Negative:** Dagre does not handle very large graphs (1000+ nodes) well.
  Acceptable — a trace with 1000 spans indicates a problem in the agent, not
  in the visualization.
