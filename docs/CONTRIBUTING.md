# Contributing to Compositor

Thank you for your interest in contributing to **Compositor**! We welcome contributions from developers of all skill levels.

---

## 1. Code of Conduct

We are committed to providing a friendly, safe, and welcoming environment for all contributors, regardless of experience level, gender identity, sexual orientation, disability, personal appearance, race, ethnicity, or religion.

---

## 2. Development Setup

### Prerequisites
* **JDK 21**: Android Gradle Plugin and Compose tooling require Java 21.
* **Android SDK**: With platform tools and build-tools installed (`ANDROID_HOME` or `local.properties`).
* **Node.js (v24 LTS)**: For the web viewer and MCP server.

### Initializing the Project
```bash
# Clone the repository
git clone https://github.com/<your-username>/compositor.git
cd compositor

# Install web viewer dependencies
cd web-viewer
npm install
cd ..
```

---

## 3. Making Changes

1. **Create a branch**:
   ```bash
   git checkout -b feat/your-feature-name
   # or
   git checkout -b fix/issue-description
   ```
2. **Follow Coding Standards**:
   Read [CODING_STANDARDS.md](file:///c:/Users/jahab/Documents/antigravity/bold-raman/docs/CODING_STANDARDS.md) before writing code.
3. **Commit Messages**:
   We use Conventional Commits:
   * `feat: add support for font scaling preview`
   * `fix: handle missing R.string in headless preview`
   * `docs: update quickstart instructions`
   * `test: add unit tests for preview parser`

---

## 4. Pull Request Process

1. Ensure all tests pass:
   * Kotlin: `./gradlew check`
   * Web: `npm run test` inside `web-viewer/`
2. Update relevant documentation in `docs/` if your change introduces new behavior.
3. Open a Pull Request with a clear description of the problem solved and screenshots/GIFs if modifying the web viewer UI.
