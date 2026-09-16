# Compositor MCP Server (AI Agent Vision Bridge)

Model Context Protocol (MCP) server that exposes Compose preview inspection tools to AI assistants (Antigravity, Cursor, Claude Code, Cline).

## Exposed Tools
- `list_previews()`: Discover all `@Preview` composables in the active project.
- `render_preview(composableName)`: Request an on-demand render.
- `get_preview_image(composableName)`: Fetch high-resolution image data for multimodal vision models.
- `get_layout_hierarchy(composableName)`: Retrieve layout bounds and semantics.

## Status
Under development (Phase 4).
