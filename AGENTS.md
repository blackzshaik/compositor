# Agent Workflow & Efficiency Guidelines

## 1. Triage & Investigation Protocol
- **Check Git Status First**: Always run `git status` and `git diff` as step 1 when investigating an issue or bug. If uncommitted edits or recent commits exist in related files, focus there immediately.
- **Targeted Scope**: Never run repository-wide recursive file searches (`Get-ChildItem -Recurse`), full directory trees, or broad `git grep` unless a targeted lookup yields nothing.
- **Isolate by Subproject**: Keep investigations strictly within the relevant subproject:
  - Web UI issues: `web-viewer/`
  - Renderer/pipeline issues: `core-renderer/`
  - VS Code issues: `vscode-extension/`
  - Gradle plugin issues: `plugin/`
- **No Speculative System Diagnostics**: Do NOT query OS processes (`Get-Process`), TCP ports (`Get-NetTCPConnection`), or unrelated subprojects (e.g. MCP server, VS Code extension) unless an explicit error trace specifies a port or process conflict.

## 2. Minimal & Targeted Verification
- **Run Only Scoped Tasks**: Run only the specific Gradle task needed for the target module (e.g. `:web-viewer:wasmJsBrowserDistribution`), not multi-module chains or full builds.
- **No Full Test Suites Unless Requested**: Do NOT run heavy end-to-end test suites (such as `:core-renderer:test`, which spins up Android LayoutLib and sample builds taking 5+ minutes) unless specifically instructed by the user or relevant unit tests exist for the changed file.
- **Batch Verification**: When verification is required, run lint/detekt and compilation together in a single targeted command rather than launching consecutive 3-minute background tasks.

## 3. Token & Tool Call Conservation
- Do not repeat identical checks (e.g. calling `git status` or `git diff` 5+ times in a single turn).
- Avoid viewing full 200+ line files when only a 10-line block is relevant.
- Make targeted edits directly once the root cause is understood.
