# Step 02: Preview Catalog & Execution Tools

*Phase*: 3 — AI Agent Vision Bridge  
*Status*: Blocked by Step 01  
*Target Module*: `mcp-server`

---

## 1. Objective
Implement and expose the core discovery and rendering tools (`list_previews` and `render_preview`) to the AI assistant via standard MCP tool schemas.

---

## 2. Tool Specifications

### 2.1. Tool: `list_previews`
* **Description**: Returns all discovered `@Preview` composable functions in the Android workspace, including module name, file path, preview parameters, and current render status.
* **Input Schema**: Optional filter string (by module name or composable name).
* **Return Payload**: JSON array of preview summaries with unique preview IDs.

### 2.2. Tool: `render_preview`
* **Description**: Triggers an on-demand re-render of a specific composable preview, waiting for completion and returning status or compilation errors.
* **Input Schema**:
  * `previewId` (string, required): The target preview identifier.
  * `theme` (string, optional: `"light"` or `"dark"`).
  * `fontScale` (number, optional: `0.85` to `1.5`).
* **Return Payload**: Success confirmation, render latency in milliseconds, image artifact path, or detailed compilation error diagnostics.

---

## 3. High-Level Architectural Guidance
* Define input parameters using strict Zod schemas compatible with the MCP SDK.
* If a render request fails due to Kotlin syntax or unresolved references, format the tool output with explicit error text and line numbers so the AI model can immediately self-correct the code.

---

## 4. Verification & Quality Gates
* **Integration Tests**: Execute tool calls through an in-memory MCP client; verify `list_previews` returns the sample app's `GreetingPreview` and `render_preview` returns valid completion metadata.
