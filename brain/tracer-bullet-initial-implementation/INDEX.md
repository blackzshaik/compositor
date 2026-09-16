# Tracer Bullet Initial Implementation: Master Orchestrator

This directory contains the step-by-step, deterministic micro-implementation specifications for the **Tracer Bullet (Thin Vertical Slice)** of Compositor.

---

## 🎯 The Mission

Prove the complete end-to-end Compositor pipeline with the thinnest possible vertical slice:
1. Render a standard Jetpack Compose `@Preview` composable (`GreetingPreview`) headlessly on the host JVM into a high-resolution `preview.png`.
2. Serve that image via a lightweight local HTTP server and push update signals over WebSockets.
3. Display the rendered UI inside a sleek device frame in the `web-viewer` browser canvas with hot-reload.

```
[Greeting.kt (@Preview)] 
       │
       ▼ (LayoutLib on JVM via Gradle Task)
[preview.png (Disk)] 
       │
       ▼ (HTTP / WebSocket Push)
[Local Preview Server (localhost:3000)]
       │
       ▼ (Browser Canvas)
[Live Phone Mockup in Chrome / Edge]
```

---

## 🤖 Protocol for AI Agents: "Build from Brain"

When a user in a fresh session prompts you with:
> **"Build from brain: tracer-bullet-initial-implementation"** (or simply **"Build from brain"**)

**You must follow this exact execution protocol:**

1. **Check State**: Read [`brain/STATE.md`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/brain/STATE.md) to identify the current active step.
2. **Open Step Specification**: Open the active `step-XX-*.md` file in this directory.
3. **Inspect Anti-Patterns**: Check [`brain/MISTAKES_AND_CORRECTIONS.md`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/brain/MISTAKES_AND_CORRECTIONS.md) to ensure you avoid known traps (such as Java 26 vs 21 LTS, or path syntax issues).
4. **Execute Deterministically**: Execute the instructions in the **Agent Action Prompt** section of the step file. Create all specified files with exact code content.
5. **Run Verification Gate**: Execute the verification command listed in the step. Do NOT mark a step complete unless the verification command exits with code 0 and meets all acceptance criteria.
6. **Log Before/After**: Fill out the **Execution Log** section at the bottom of the step file with actual output timestamps and file artifacts.
7. **Commit Changes**: Create a conventional git commit (e.g. `feat: implement tracer bullet step 01 - sample composable`).
8. **Advance State**: Update `brain/STATE.md` marking the current step complete and pointing to the next step.

---

## 📋 Micro-Implementation Steps

| Step | File | Status | Objective |
| :--- | :--- | :--- | :--- |
| **01** | [`step-01-sample-composable.md`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/brain/tracer-bullet-initial-implementation/step-01-sample-composable.md) | ⚪ Ready | Scaffold minimal Android Jetpack Compose app with `GreetingPreview`. |
| **02** | [`step-02-headless-render-spike.md`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/brain/tracer-bullet-initial-implementation/step-02-headless-render-spike.md) | ⚪ Ready | Configure headless JVM LayoutLib rendering task outputting `preview.png`. |
| **03** | [`step-03-local-preview-server.md`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/brain/tracer-bullet-initial-implementation/step-03-local-preview-server.md) | ⚪ Ready | Build local server streaming preview frame and WebSocket change events. |
| **04** | [`step-04-web-canvas-display.md`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/brain/tracer-bullet-initial-implementation/step-04-web-canvas-display.md) | ⚪ Ready | Connect web viewer to stream and render phone frame mockup. |
| **05** | [`step-05-end-to-end-verification.md`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/brain/tracer-bullet-initial-implementation/step-05-end-to-end-verification.md) | ⚪ Ready | Execute end-to-end hot reload test, benchmark latency (< 2.5s), and log results. |
