# Step 03: Multimodal Vision & Layout Inspection Tools

*Phase*: 3 — AI Agent Vision Bridge  
*Status*: Complete ✅  
*Target Module*: `mcp-server`

---

## 1. Objective
Equip multimodal AI agents with the ability to "see" Compose UI layouts by providing tools that return high-resolution base64 PNG images and structured semantic layout trees with coordinate bounds.

---

## 2. Tool Specifications

### 2.1. Tool: `get_preview_image`
* **Description**: Retrieves the latest rendered image for a preview formatted specifically for multimodal LLMs (as a base64 encoded image content block).
* **Input Schema**:
  * `previewId` (string, required): The target preview identifier.
* **Return Payload**: Standard MCP image content block (`type: "image"`, `mimeType: "image/png"`, `data: "<base64>"`).

### 2.2. Tool: `inspect_layout_tree`
* **Description**: Retrieves the parsed layout bounds, composable hierarchy, and semantic nodes (text contents, clickable roles, accessibility labels) for spatial debugging.
* **Input Schema**:
  * `previewId` (string, required): The target preview identifier.
* **Return Payload**: Structured JSON tree representing UI nodes with `[x, y, width, height]` coordinates and semantic tags.

---

## 3. High-Level Architectural Guidance
* Format the image output using MCP's native `ImageContent` object so clients like Claude or Cursor automatically attach the image to the multimodal model context.
* Keep image payloads optimized (e.g. compress or clamp dimensions if larger than 4K) to stay within LLM token budget.
* The layout tree should allow text-based reasoning models to calculate padding and overlap issues without needing full vision processing.

---

## 4. Verification & Quality Gates
* **Integration Tests**: Verify `get_preview_image` returns a valid base64 PNG header (`data:image/png;base64,...`) and `inspect_layout_tree` returns valid coordinate bounds for the sample composable.
