# Step 02: Kotlin PSI Preview Discovery & Metadata Parser

*Phase*: 1 — Native LayoutLib Engine & Pure-Kotlin Ktor Daemon  
*Status*: Blocked by Step 01  
*Target Module*: `core-renderer`

---

## 1. Objective
Implement an automated preview discovery engine in Kotlin using the official **Kotlin Compiler Embedded PSI (Program Structure Interface)**. This scans `.kt` files to extract `@Preview` annotated functions and parameters with 100% AST accuracy without needing to compile the files.

---

## 2. Architectural Design & Responsibilities
* **Source Tree Traversal**:
  * Traverse Kotlin source directories (`src/main/java`, `src/main/kotlin`).
  * Initialize an in-memory Kotlin CoreApplicationEnvironment and PSI parser.
* **AST Visitor & Annotation Inspection**:
  * Traverse AST nodes to identify top-level and companion `@Composable` functions.
  * Detect `@Preview` annotations (both qualified and unqualified).
  * Parse parameter expressions from annotation value arguments:
    * `name`: Custom preview label.
    * `group`: Organizational grouping tag.
    * `widthDp` / `heightDp`: Custom dimensions.
    * `uiMode`: Theme configuration (night mode flags).
    * `fontScale`: Font scaling factor.
    * `showBackground`: Background rendering flag.
* **Catalog Registry Serialization**:
  * Produce a domain model: `PreviewItem` and `PreviewCatalog`.
  * Serialize and atomically persist the catalog to `.compositor/previews.json`.

---

## 3. High-Level Integration Guidance
* Implement as a reusable Kotlin class (e.g. `KotlinPsiPreviewScanner`).
* Leverage `kotlin-compiler-embeddable` dependencies to avoid pulling in heavy IDE plugins.
* Ensure execution completes in under 100 milliseconds even across projects with dozens of files.

---

## 4. Verification & Quality Gates
* **Unit Tests**: Pass source strings and file paths containing single previews, multipreviews, and parameterized previews; assert the returned catalog matches expected function names and parameter values.
* **Quality Gate**: Code must pass `./gradlew detekt` and comply with the 120-character line limit.
