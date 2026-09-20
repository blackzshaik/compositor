# Step 01: VS Code Extension Scaffolding

*Phase*: 5 — IDE Extensions & Plugin Ecosystem  
*Status*: Complete ✅  
*Target Module*: `vscode-extension`

---

## 1. Objective
Scaffold the extension package under `vscode-extension/` with modern TypeScript tooling, defining extension metadata, lazy activation events, and view container contributions for VS Code and Cursor.

---

## 2. Functional Requirements
* **Extension Metadata**:
  * Set up `package.json` with publisher, description, icon, and engine target (`^1.90.0`).
  * Configure build scripts using `esbuild` or `tsc` for fast extension packaging.
* **Activation Events**:
  * Lazy-load the extension on `onLanguage:kotlin` or `workspaceContains:**/build.gradle.kts` to prevent slowing down general editor startup.
* **Contribution Points**:
  * Register a custom Activity Bar icon (`compositor-icon`).
  * Register a View Container and View (`compositor.previewView`).

---

## 3. High-Level Architectural Guidance
* Keep extension bundle size minimal; bundle dependencies using tree-shaking.
* Ensure full compatibility with both VS Code and Cursor (Cursor uses the identical VS Code extension API).
* Adhere to strict TypeScript compilation checks (`noImplicitAny`, `strictNullChecks`).

---

## 4. Verification & Quality Gates
* **Packaging & Compile Check**: Run `npm run build` inside `vscode-extension/`; assert clean compilation and generation of `dist/extension.js`.
* **Linting**: Pass `npm run lint` with zero errors.
