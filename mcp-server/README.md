# Compositor MCP Server (AI Agent Vision Bridge)

Official Model Context Protocol (MCP) server equipping AI coding assistants (Antigravity, Cursor, Claude Code, Cline) with native visual capabilities for Jetpack Compose.

## Exposed Tools

| Tool | Description | Input Schema |
| :--- | :--- | :--- |
| `list_previews` | Discover all `@Preview` composables in the workspace with metadata. | `filter?: string` |
| `render_preview` | Request targeted on-demand render, waiting for completion. | `previewId: string`, `theme?: "light" \| "dark"`, `fontScale?: number`, `timeoutMs?: number` |
| `get_preview_image` | Fetch high-resolution base64 PNG formatted as native MCP `ImageContent`. | `previewId: string` |
| `inspect_layout_tree` | Retrieve structured layout bounds, padding, and semantics. | `previewId: string` |
| `compare_previews` | Visual regression diff against baseline snapshot or between previews. | `previewId: string`, `baselineTimestamp?: number`, `compareWithPreviewId?: string`, `saveCurrentAsBaseline?: boolean` |

## AI Assistant Configuration

Add the server to your assistant's MCP configuration (e.g. `claude_desktop_config.json`, `.cursor/mcp.json`):

```json
{
  "mcpServers": {
    "compositor": {
      "command": "node",
      "args": ["<absolute-path-to-compositor>/mcp-server/dist/server.js"],
      "env": {
        "COMPOSITOR_DAEMON_URL": "http://127.0.0.1:3001"
      }
    }
  }
}
```

## Architecture & Protocol Integrity
- Communicates strictly over standard input/output (`stdio`).
- All diagnostic and operational logs are routed exclusively to `stderr`, preserving stdout integrity for JSON-RPC messages.
- Communicates with the local Compositor daemon (`http://127.0.0.1:3001`).

## Development & Verification

```bash
# Build TypeScript
npm run build

# Run unit and integration tests
npm test

# Run stdio JSON-RPC smoke test
npm run smoke-test
```
