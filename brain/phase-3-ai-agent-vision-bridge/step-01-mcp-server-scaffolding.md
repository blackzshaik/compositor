# Step 01: MCP Server Initialization & Transport

*Phase*: 3 — AI Agent Vision Bridge  
*Status*: Ready for Implementation  
*Target Module*: `mcp-server`

---

## 1. Objective
Scaffold the standalone Model Context Protocol (MCP) server under `mcp-server/` using the official TypeScript SDK, establishing stdio (standard input/output) transport to interface directly with AI agent clients (such as Cursor or Antigravity).

---

## 2. Functional Requirements
* **MCP Server Initialization**:
  * Set up `package.json`, `tsconfig.json`, and ESLint configurations in `mcp-server/`.
  * Depend on `@modelcontextprotocol/sdk`.
  * Implement server lifecycle: start, stop, handshake, and capability advertisement (tools).
* **Transport Layer**:
  * Support `StdioServerTransport` for local CLI / IDE integration.
  * Optionally support Stream/SSE for remote daemon architectures.
* **Daemon Client**:
  * Implement a lightweight HTTP/IPC client communicating with the local Compositor daemon (`http://localhost:3001`).

---

## 3. High-Level Architectural Guidance
* Ensure the MCP server process outputs strictly valid JSON-RPC frames to stdout; all diagnostic logs must be directed to `stderr` or a log file to avoid corrupting the protocol stream.
* Handle daemon unavailability gracefully: if the Compositor daemon is offline, return helpful diagnostic messages instructing the agent to start the daemon.

---

## 4. Verification & Quality Gates
* **Smoke Test**: Launch the MCP server via script, send an `initialize` JSON-RPC request over stdin, and verify valid server capabilities response over stdout.
* **Lint & Build**: Ensure `npm run build` and `npm run lint` pass cleanly.
