# Step 01: Multi-Module Preview Sidebar & Search

*Phase*: 2 — Production Web Viewer & Interactive Canvas  
*Status*: Ready for Implementation  
*Target Module*: `web-viewer`

---

## 1. Objective
Create a responsive, collapsible navigation sidebar that fetches the preview catalog from the daemon API (`GET /api/previews`), organizes previews into a hierarchical tree (Module -> File -> Composable), and provides real-time search and tag filtering.

---

## 2. Functional Requirements
* **Catalog Fetch & Live Sync**: Query `/api/previews` on mount and update dynamically via WebSocket events.
* **Hierarchical Tree View**: Group previews by Gradle module and package/source file.
* **Instant Search**: Filter previews by composable function name, parameter name, or enclosing class.
* **Tag & Group Filters**: Quick chips to filter by `@Preview(group = "...")` tags.
* **Selection State**: Highlight active selection and update browser URL hash or query state for deep-linking.
* **Status Badges**: Display visual indicators for render state (`Rendered`, `Pending`, `Error`).

---

## 3. High-Level Architectural Guidance
* Encapsulate state in a dedicated React hook (e.g. `usePreviewCatalog`).
* Ensure virtualized or optimized list rendering if a project contains hundreds of `@Preview` annotations.
* Provide clean keyboard navigation (up/down arrow keys, Enter to select).
* Maintain a collapsible state so developers can maximize canvas area.

---

## 4. Verification & Quality Gates
* **Component Unit Tests**: Render sidebar with mock preview catalog; verify search filtering, group selection, and active item highlighting.
* **Linter & Typecheck**: Pass `npm run lint` and `npm run typecheck` in `web-viewer`.
