# Phase 3: Shippable Compositor Gradle Plugin (`io.compositor`)

## Mission
Package Compositor into a standalone, publishable Android Gradle Plugin (`id("io.compositor")`). This transforms Compositor from an internal sample experiment into a plug-and-play tool that any developer can add to their existing external Android project with one line of code in `build.gradle.kts`.

---

## Architectural Workflow
```
[ Developer's Existing Android Project ]
               │
               ▼ (Applies plugin: id("io.compositor"))
┌────────────────────────────────────────────────────────┐
│ Compositor Gradle Plugin (plugin/)                     │
│  ├── 1. Hooks into AGP Artifact Collections            │
│  │   • Automatically resolves compile classpath        │
│  │   • Resolves merged Android resources (R.jar, res/) │
│  │   • Resolves target Android SDK platform            │
│  │                                                     │
│  ├── 2. Registers Tasks:                               │
│  │   • ./gradlew compositor (Launches daemon & browser)│
│  │   • ./gradlew compositorRender (Headless CLI batch) │
│  │                                                     │
│  └── 3. Bundles Compositor Engine & Embedded Web UI    │
│      • Fully self-contained; zero external setup       │
└────────────────────────────────────────────────────────┘
```

---

## Step Progression

| Step | File | Scope |
| :--- | :--- | :--- |
| **01** | [`step-01-gradle-plugin-scaffolding.md`](./step-01-gradle-plugin-scaffolding.md) | Scaffold Gradle Plugin project (`plugin/`) with Gradle Plugin Portal publishing metadata. |
| **02** | [`step-02-agp-classpath-resource-resolution.md`](./step-02-agp-classpath-resource-resolution.md) | Hook into Android Gradle Plugin (AGP) API to extract classpaths and merged resources. |
| **03** | [`step-03-compositor-tasks-registration.md`](./step-03-compositor-tasks-registration.md) | Register the `./gradlew compositor` and `./gradlew compositorRender` tasks. |
| **04** | [`step-04-external-project-verification.md`](./step-04-external-project-verification.md) | Verify applying the plugin to a separate external Android app with zero configuration. |

---

## Protocol for AI Agents
1. Ensure Phases 1 and 2 are functional.
2. Adhere to Gradle Plugin best practices: lazy task configuration, configuration cache compatibility, and clean AGP lifecycle integration.
3. Keep instructions high-level without hardcoding brittle source code.
