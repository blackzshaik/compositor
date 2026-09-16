# Compositor Coding Standards & Guidelines

This document outlines the engineering practices, coding standards, and quality gates for the Compositor project across all supported languages and modules.

---

## 1. General Engineering Principles

* **Clarity over Cleverness**: Write code that is easy to read, debug, and maintain.
* **Fail Fast, Fail Informatively**: Validate inputs at system boundaries. Throw descriptive exceptions with actionable diagnostic messages.
* **Separation of Concerns**: Keep I/O, network communication, compilation logic, and presentation rendering strictly decoupled.
* **No AI Slop**: Every piece of generated code must have a clear purpose, correct types, and test coverage. Do not check in dead code, speculative helper functions, or fake mock data.

---

## 2. Kotlin / JVM Standards (`core-renderer`, Kotlin CLI)

* **Target Version**: Kotlin 2.x, Java 21 bytecode target.
* **Style Guide**: Follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) and `.editorconfig`.
* **Null Safety**:
  * Never use `!!` (double bang) assertions in production code. Use `requireNotNull`, `checkNotNull`, or safe calls with meaningful fallbacks.
  * Avoid nullable collections (use `emptyList()` instead of `null`).
* **Immutability**:
  * Prefer `val` over `var`.
  * Prefer immutable collections (`List`, `Set`, `Map`) over mutable collections.
* **Coroutines & Concurrency**:
  * Always provide explicit `CoroutineDispatcher` via dependency injection; never hardcode `Dispatchers.IO` directly inside business logic.
  * Ensure structured concurrency: do not launch orphaned jobs in `GlobalScope`.
* **Error Handling**:
  * Model known failure states using sealed interfaces/classes (e.g. `sealed interface RenderResult { data class Success(...); data class Error(...); }`).
  * Only throw unhandled exceptions for truly unrecoverable program errors.

---

## 3. TypeScript / Web Standards (`web-viewer`, `mcp-server`)

* **Target Version**: TypeScript 5.x+, Node.js 24 LTS target.
* **Strict Typing**:
  * `"strict": true` must be enabled in `tsconfig.json`.
  * The `any` type is strictly forbidden. Use `unknown` with type narrowing or type guards.
* **Imports & Modularity**:
  * Use explicit ESM imports with file extensions where required.
  * Group imports logically: standard library, external packages, internal modules.
* **React / UI Standards (`web-viewer`)**:
  * Use functional components with hooks exclusively.
  * Define explicit prop interfaces for every component.
  * Style using Tailwind CSS utility classes; avoid inline styles except for dynamic coordinate calculations.
  * Isolate error boundaries around the preview canvas so a rendering error never crashes the entire inspector UI.

---

## 4. Automated Quality Gates

### 4.1. Kotlin Static Analysis & Coverage
* **Detekt**:
  * Run static analysis: `./gradlew detekt`
  * Configuration: [`config/detekt/detekt.yml`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/config/detekt/detekt.yml)
  * Enforces: Line length max 120 (including comments), cyclomatic complexity < 15, method length < 60, forbidden double-bang `!!`, no wildcard imports.
* **Kover (Code Coverage)**:
  * Generate HTML report: `./gradlew koverHtmlReport` (output: `build/reports/kover/html/index.html`)
  * Generate XML report: `./gradlew koverXmlReport`
  * Verify coverage thresholds: `./gradlew koverVerify`

### 4.2. TypeScript Static Analysis & Coverage
* **ESLint & Prettier**:
  * Run linter: `npm run lint` inside `web-viewer/`
  * Auto-fix style: `npm run lint:fix`
  * Check formatting: `npm run format:check`
  * Type check without emitting: `npm run typecheck`
* **Vitest Coverage**:
  * Run unit tests: `npm test`
  * Run coverage analysis: `npm run test:coverage` (enforces 80% coverage threshold across statements, lines, and functions).

---

## 5. Testing Requirements

* **Unit Tests**:
  * Every parser, utility, and state machine must have comprehensive unit tests.
  * Kotlin tests use JUnit 5 + MockK / Truth or Kotlin-test.
  * TypeScript tests use Vitest + Testing Library.
* **Integration Tests**:
  * Must verify end-to-end rendering against real sample composables in `samples/sample-app`.

---

## 6. Future Git Pre-Commit Hook Roadmap

> [!NOTE]
> **Breathing Space for Early Development**:
> During initial scaffolding and active feature bootstrapping, quality checks are run manually via `./gradlew detekt` and `npm run lint` rather than being hard-blocked in a local `pre-commit` hook. This ensures rapid developer velocity.
>
> **Future Hard Enforcement**:
> Once core pipelines stabilize, pre-commit checks will be enabled via Git hooks (or Lefthook) to block commits failing:
> 1. `./gradlew detekt`
> 2. `npm run lint` & `npm run typecheck`
> 3. Fast unit tests.
