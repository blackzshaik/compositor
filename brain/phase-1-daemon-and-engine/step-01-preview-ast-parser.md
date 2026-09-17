# Step 01: Preview AST Parser

*Phase*: 1 — Auto-Watch Daemon & Incremental Engine  
*Status*: Complete ✅  
*Target Module*: `cli`

---

## 1. Objective
Build an automated parser capable of scanning Kotlin source files (`.kt`) and extracting all `@Preview` annotated composable functions along with their configured parameters.

---

## 2. Functional Requirements
* Detect functions annotated with `androidx.compose.ui.tooling.preview.Preview` (or unqualified `@Preview`).
* Support multiple `@Preview` annotations on a single composable (multipreview).
* Extract preview parameters:
  * `name`: Custom preview label or default to the function name.
  * `group`: Logical grouping tag.
  * `widthDp`, `heightDp`: Explicit layout dimensions if specified.
  * `uiMode`: Configuration flags (e.g. Night mode / Dark theme).
  * `fontScale`: Typography scaling factor.
  * `showBackground`: Background flag.
* Extract metadata:
  * Fully qualified class/package name.
  * Composable function identifier.
  * Source file path and line numbers.

---

## 3. High-Level Architectural Guidance
* Implement as a lightweight parser utility in TypeScript (inside `cli/`) or Kotlin (inside `core-renderer/`).
* Parse file content incrementally without requiring full Gradle compilation passes to maintain sub-second speed.
* Handle multi-line annotation declarations and default values gracefully.
* Return a strongly-typed collection of `PreviewDefinition` objects.

---

## 4. Verification & Quality Gates
* **Unit Tests**: Test against diverse sample composables (single preview, multiple previews on one function, previews with complex parameter values, previews in nested packages).
* **Static Analysis**: Pass Detekt (if Kotlin) or ESLint (if TypeScript) with zero warnings.
