# Contributing to Compositor

Thank you for your interest in contributing to **Compositor**! We welcome contributions from developers of all skill levels.

---

## 1. Code of Conduct

All contributors and participants are expected to follow our [Code of Conduct](../CODE_OF_CONDUCT.md). Please report any violations through our repository issue tracker or private contact.

---

## 2. Project Architecture Overview

Compositor consists of several coordinated subprojects:

| Subproject | Language / Stack | Purpose |
| :--- | :--- | :--- |
| **`core-renderer`** | Kotlin / JVM (Java 21) | Headless LayoutLib engine, AST preview parser, Ktor Netty daemon, embedded Kotlin MCP SDK. |
| **`plugin`** | Gradle Plugin (Kotlin) | AGP classpath & resource resolution, tasks (`compositor`, `compositorRender`, `compositorMcp`). |
| **`web-viewer`** | Kotlin / Wasm (Compose Multiplatform) | Production hardware-accelerated Canvas web viewer. |
| **`web-viewer-react`** | TypeScript / React / Vite | Ultra-fast native web viewer frontend embedded into daemon resources. |
| **`vscode-extension`** | TypeScript / VS Code API | Extension for VS Code and Cursor editors with sidebar webview & workspace auto-detection. |
| **`samples/sample-app`** | Kotlin / Jetpack Compose | Reference Android Compose app for verification and testing. |

---

## 3. Development Setup

### Prerequisites
* **JDK 21**: Android Gradle Plugin and Compose tooling require Java 21.
* **Android SDK**: With platform tools and API 34/35 installed (`ANDROID_HOME` or `local.properties`).
* **Node.js (v20+ LTS)**: Required for building the React web viewer and VS Code extension.

### Initializing the Project
```bash
# Clone the repository
git clone https://github.com/compositor-org/compositor.git
cd compositor

# Install dependencies for React viewer and VS Code extension
npm --prefix web-viewer-react install
npm --prefix vscode-extension install
```

---

## 4. Building and Testing

### Kotlin & Gradle
```bash
# Run Detekt static analysis
./gradlew detekt

# Run core renderer unit tests
./gradlew :core-renderer:test

# Run sample app live daemon with Web Viewer
./gradlew :samples:sample-app:compositor

# Run headless batch render to PNG
./gradlew :samples:sample-app:compositorRender
```

### Web Viewer (React)
```bash
cd web-viewer-react
npm run build
npm test
```

### VS Code Extension
```bash
cd vscode-extension
npm run build
npm test
```

---

## 5. Making Changes

1. **Create a branch**:
   ```bash
   git checkout -b feat/your-feature-name
   # or
   git checkout -b fix/issue-description
   ```
2. **Follow Coding Standards**:
   Read [CODING_STANDARDS.md](CODING_STANDARDS.md) before writing code.
3. **Commit Messages**:
   We follow [Conventional Commits](https://www.conventionalcommits.org/):
   * `feat: add support for font scaling preview`
   * `fix: handle missing R.string in headless preview`
   * `docs: update quickstart instructions`
   * `test: add unit tests for preview parser`

---

## 6. Pull Request Process

1. Ensure code passes Detekt analysis and tests:
   ```bash
   ./gradlew detekt
   ./gradlew test
   ```
2. Update relevant documentation in `docs/` if your change introduces new behavior or tasks.
3. Open a Pull Request referencing any linked issues, providing a clear summary and screenshots or screen recordings for UI changes.
