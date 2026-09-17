# Phase 3: AI Agent Vision Bridge (Model Context Protocol - MCP)

## Mission
Equip AI coding agents (such as Antigravity, Cursor, Claude Code, Cline) with native visual capabilities for Jetpack Compose. By implementing an official Model Context Protocol (MCP) server, agents can discover available previews, trigger targeted renders, inspect base64 bitmaps with vision models, retrieve semantic layout trees, and verify visual diffs.

---

## Architecture Overview
```
┌────────────────────────────────────────────────────────┐
│ AI Coding Agent (Cursor / Claude / Antigravity)        │
└──────────────────────────┬─────────────────────────────┘
                           │ (MCP Protocol via stdio / SSE)
                           ▼
┌────────────────────────────────────────────────────────┐
│ Compositor MCP Server (mcp-server/)                    │
│  ├── Tool: list_previews()                             │
│  ├── Tool: render_preview(name, overrides?)            │
│  ├── Tool: get_preview_image(name) [Base64 PNG]        │
│  ├── Tool: inspect_layout_tree(name) [Bounds & Semantics]
│  └── Tool: compare_previews(before, after) [Diff Overlay]
└──────────────────────────┬─────────────────────────────┘
                           │ (REST / IPC)
                           ▼
┌────────────────────────────────────────────────────────┐
│ Compositor Daemon & Engine (cli/ & core-renderer/)     │
└────────────────────────────────────────────────────────┘
```

---

## Step Progression

| Step | File | Scope |
| :--- | :--- | :--- |
| **01** | [`step-01-mcp-server-scaffolding.md`](./step-01-mcp-server-scaffolding.md) | Initialize MCP server package with official SDK and stdio transport. |
| **02** | [`step-02-preview-catalog-and-render-tools.md`](./step-02-preview-catalog-and-render-tools.md) | Implement `list_previews` and `render_preview` agent tools. |
| **03** | [`step-03-vision-image-and-layout-tools.md`](./step-03-vision-image-and-layout-tools.md) | Implement `get_preview_image` (Base64) and `inspect_layout_tree` tools. |
| **04** | [`step-04-visual-regression-diff-tool.md`](./step-04-visual-regression-diff-tool.md) | Build `compare_previews` visual regression and layout diffing tool. |

---

## Protocol for AI Agents
1. Ensure the daemon (Phase 1) is operational before running the MCP server.
2. Follow standard Model Context Protocol schemas (`@modelcontextprotocol/sdk`).
3. Ensure tools return clear, structured error responses if a preview fails to compile or render.
