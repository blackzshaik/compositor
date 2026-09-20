# Step 04: Workspace Auto-Detection & Daemon Lifecycle

*Phase*: 5 — IDE Extensions & Plugin Ecosystem  
*Status*: Complete ✅  
*Target Module*: `vscode-extension`

---

## 1. Objective
Enable seamless zero-configuration startup by automatically detecting Android project roots, locating Android SDK and JDK 21 paths, and managing the Compositor daemon as a managed background child process.

---

## 2. Functional Requirements
* **Workspace Detection**:
  * Inspect workspace folders for Android root markers (`build.gradle.kts`, `settings.gradle.kts`, `AndroidManifest.xml`).
  * Verify presence of Android SDK (`ANDROID_HOME`, `local.properties`) and JDK 21.
* **Managed Daemon Process**:
  * Provide option to auto-spawn the Compositor daemon (`cli/src/server.ts`) as a child process when an Android workspace opens.
  * Stream daemon stdout/stderr directly into a dedicated VS Code Output Channel (`Compositor Output`).
  * Gracefully terminate daemon process when VS Code / Cursor closes.
* **Port Conflict Handling**:
  * Detect if port 3001/3002 is occupied and negotiate alternative ports or prompt user.

---

## 3. High-Level Architectural Guidance
* Respect user configuration: provide settings (`compositor.autoStartDaemon: boolean`, `compositor.serverPort: number`, `compositor.jdkPath: string`).
* Ensure child process cleanup: attach process exit hooks to prevent orphaned Node/JVM daemon processes when the editor exits unexpectedly.

---

## 4. Verification & Quality Gates
* **Integration Tests**: Verify workspace root detection logic with mock project fixtures, ensuring accurate detection of Android vs non-Android folders.
