# Step 02: Preview Catalog & Execution Tools

*Phase*: 4 — AI Agent Vision Bridge  
*Status*: Complete ✅  
*Target Module*: `:core-renderer`

---

## 1. Objective
Implement and expose the core discovery and rendering tools (`list_previews` and `render_preview`) to AI assistants using `io.modelcontextprotocol.kotlin.sdk.RegisteredTool` and strict JSON Schema definitions.

---

## 2. Tool Specifications

### 2.1. Tool: `list_previews`
* **Description**: Returns all discovered `@Preview` composable functions in the Android workspace, including module name, file path, preview parameters, and current render status.
* **Input Schema**: Optional `filter` string (by module name or composable name).
* **Return Payload**: JSON array of preview summaries with unique preview IDs, composable names, qualified class names, and render states.

### 2.2. Tool: `render_preview`
* **Description**: Triggers an on-demand re-render of a specific composable preview, executing through `CompositorPipeline` or cached rendering, returning render latency and image path.
* **Input Schema**:
  * `previewId` (string, required): The target preview identifier.
  * `theme` (string, optional: `"light"` or `"dark"`).
  * `fontScale` (number, optional: `0.85` to `1.5`).
* **Return Payload**: JSON object confirming render status, render latency in milliseconds, image artifact path, or detailed compilation error diagnostics.

---

## 3. High-Level Architectural Guidance
* Tool schemas are declared via `Tool.Input(properties = buildJsonObject { ... }, required = listOf(...))` adhering to MCP standards.
* Rendering exceptions return clear, actionable diagnostics within standard `CallToolResult(content = listOf(TextContent(...)))` to allow autonomous self-correction.

---

## 4. Verification & Quality Gates
* **Integration Tests**: `CompositorMcpServerTest` validates `list_previews` returns active composables and `render_preview` executes and returns JSON metadata.
