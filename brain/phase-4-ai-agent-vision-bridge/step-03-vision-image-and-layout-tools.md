# Step 03: Multimodal Vision & Layout Inspection Tools

*Phase*: 4 — AI Agent Vision Bridge  
*Status*: Complete ✅  
*Target Module*: `:core-renderer`

---

## 1. Objective
Equip multimodal AI agents with native visual perception and semantic inspection of Compose UI layouts by providing tools that return high-resolution base64 PNG images (`get_preview_image`) and structured semantic layout trees (`inspect_layout_tree`).

---

## 2. Tool Specifications

### 2.1. Tool: `get_preview_image`
* **Description**: Retrieves the rendered bitmap for a composable preview, encoded as Base64 PNG.
* **Input Schema**:
  * `previewId` (string, required): The target preview identifier.
* **Return Payload**: Base64 PNG image content block (`ImageContent(data = base64Data, mimeType = "image/png")`) or fallback `TextContent` with Base64 payload.

### 2.2. Tool: `inspect_layout_tree`
* **Description**: Retrieves parsed layout bounds, composable hierarchy, and semantic nodes (bounds, classes, and accessibility roles).
* **Input Schema**:
  * `previewId` (string, required): The target preview identifier.
* **Return Payload**: Structured JSON tree representing UI nodes with coordinate bounds `[x, y, width, height]` and semantic metadata.

---

## 3. High-Level Architectural Guidance
* Base64 encoding uses `java.util.Base64.getEncoder().encodeToString(bytes)` for maximum throughput without overhead.
* Resolves rendered artifacts from `.compositor/output/` and baseline directories deterministically.
* Semantic node hierarchy allows spatial verification and layout calculation for non-vision LLMs.

---

## 4. Verification & Quality Gates
* **Integration Tests**: `CompositorMcpServerTest` validates `get_preview_image` returns valid Base64 PNG data and `inspect_layout_tree` returns valid JSON tree bounds.
