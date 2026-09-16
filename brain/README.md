# 🧠 Brain: Collective Intelligence & Memory for AI Agents

Welcome to **`brain/`**. This directory serves as the persistent cognitive memory bank for all Large Language Models (LLMs) and AI agents collaborating on the **Compositor** project.

---

## 🎯 Purpose

AI coding assistants are stateless by default across isolated sessions. When a new agent or turn begins, context from past architectural decisions, debugging struggles, and edge-case discoveries is often lost.

`brain/` solves this by acting as **that one senior engineer on the team who knows every single nuance, decision, pitfall, and standard of the codebase.**

---

## 📂 Structure

| File | Purpose | When to Update |
| :--- | :--- | :--- |
| **[`DECISIONS.md`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/brain/DECISIONS.md)** | Architectural Decision Records (ADRs). Documents *why* a technology, pattern, or constraint was chosen. | Whenever introducing a new library, altering system architecture, or choosing between competing designs. |
| **[`MISTAKES_AND_CORRECTIONS.md`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/brain/MISTAKES_AND_CORRECTIONS.md)** | Post-mortem catalog of pitfalls, runtime quirks, failed attempts, and their solutions. | Immediately after resolving a tricky bug, build failure, platform quirk, or wrong architectural assumption. |
| **[`STATE.md`](file:///c:/Users/jahab/Documents/antigravity/bold-raman/brain/STATE.md)** | Living snapshot of project milestones, current progress, verified features, and immediate next steps. | At the start and completion of major milestones or tasks. |

---

## 📜 Rules of Engagement for AI Agents

1. **Read First**: Every AI agent MUST review `brain/STATE.md` and `brain/MISTAKES_AND_CORRECTIONS.md` before starting any coding task to avoid re-inventing the wheel or falling into known traps.
2. **Log Every Lesson**: If you hit a build error, a Gradle issue, or a platform incompatibility, document it in `MISTAKES_AND_CORRECTIONS.md` with:
   - **Symptom / Error**
   - **Root Cause**
   - **Fix / Prevention Rule**
3. **Record Architectural Decisions**: If a design choice is made (e.g. choice of serialization library, RPC protocol, preview extraction mechanism), write an ADR in `DECISIONS.md`.
4. **Keep `STATE.md` Honest**: Update what is actually working and verified vs what is planned. Never report an unverified feature as complete.
