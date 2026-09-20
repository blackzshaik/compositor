# Phase 4: AI Agent Vision Bridge (Model Context Protocol - MCP)

## Mission
Equip AI coding agents (such as Antigravity, Cursor, Claude Code, Cline) with native visual capabilities for Jetpack Compose. Using the official Kotlin MCP SDK embedded directly within the Ktor daemon, agents can discover available previews, trigger targeted renders, inspect base64 bitmaps with vision models, retrieve semantic layout trees, and verify visual diffs without running external server processes.

---

## Architecture Overview
```
┌────────────────────────────────────────────────────────┐
│ AI Coding Agent (Cursor / Claude / Antigravity)        │
└──────────────────────────┬─────────────────────────────┘
                           │ (MCP Protocol via stdio / SSE)
                           ▼
┌────────────────────────────────────────────────────────┐
│ Compositor Ktor Daemon (Embedded Kotlin MCP Server)    │
│  ├── Tool: list_previews()                             │
│  ├── Tool: render_preview(name, overrides?)            │
│  ├── Tool: get_preview_image(name) [Base64 PNG]        │
│  ├── Tool: inspect_layout_tree(name) [Bounds & Semantics]
│  └── Tool: compare_previews(before, after) [Diff Overlay]
└──────────────────────────┬─────────────────────────────┘
                           │ (In-Process Coroutines)
                           ▼
┌────────────────────────────────────────────────────────┐
│ Compositor Daemon & Engine (cli/ & core-renderer/)     │
└────────────────────────────────────────────────────────┘
```

---

## Step Progression

| Step | File | Scope | Status |
| :--- | :--- | :--- | :--- |
| **01** | [`step-01-mcp-server-scaffolding.md`](./step-01-mcp-server-scaffolding.md) | Initialize MCP server package with official SDK and stdio transport. | 🟢 Complete |
| **02** | [`step-02-preview-catalog-and-render-tools.md`](./step-02-preview-catalog-and-render-tools.md) | Implement `list_previews` and `render_preview` agent tools. | 🟢 Complete |
| **03** | [`step-03-vision-image-and-layout-tools.md`](./step-03-vision-image-and-layout-tools.md) | Implement `get_preview_image` (Base64) and `inspect_layout_tree` tools. | 🟢 Complete |
| **04** | [`step-04-visual-regression-diff-tool.md`](./step-04-visual-regression-diff-tool.md) | Build `compare_previews` visual regression and layout diffing tool. | 🟢 Complete |

---

## Protocol for AI Agents
1. Agents can interact via CLI stdio transport (`./gradlew compositorMcp` or `CompositorCli` in `mcp` mode) or via the embedded Ktor daemon SSE endpoints (`/mcp` and `/sse`).
2. Adhere to official Kotlin Model Context Protocol SDK specifications (`io.modelcontextprotocol:kotlin-sdk`).
3. Tool executions return structured text or image content blocks, formatting compilation or layout errors clearly for autonomous LLM self-correction.
