# Step 01: MCP Server Initialization & Transport

*Phase*: 4 — AI Agent Vision Bridge  
*Status*: Complete ✅  
*Target Module*: `:core-renderer`, `:plugin`

---

## 1. Objective
Scaffold the Model Context Protocol (MCP) server directly into `:core-renderer` and `:plugin` using the official Kotlin MCP SDK (`io.modelcontextprotocol:kotlin-sdk`), establishing both stdio (standard input/output) transport and embedded Ktor daemon SSE endpoints (`/mcp`, `/sse`) without external Node.js dependencies.

---

## 2. Functional Requirements
* **MCP Server Initialization**:
  * Integrate `io.modelcontextprotocol:kotlin-sdk:0.5.0` in `gradle/libs.versions.toml` and `core-renderer/build.gradle.kts`.
  * Instantiate `Server(Implementation("compositor-mcp-server", "0.1.0"), ServerOptions(capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = true))))`.
  * Implement server lifecycle, handshake, and tool capability advertisement.
* **Transport Layer**:
  * **CLI Stdio**: `StdioServerTransport` connected to `System.`in`` and `System.out` within `CompositorCli` (`mode = "mcp"`). All diagnostics strictly routed to `System.err`.
  * **Daemon SSE**: Ktor `SSE` plugin installed in `CompositorDaemon`, serving `/mcp` and `/sse` endpoints using `mcpEndpoint(server)`.
* **Gradle Integration**:
  * `CompositorMcpTask` registered in `:plugin` as `./gradlew compositorMcp`, executing `CompositorCli` in an isolated JavaExec fork with complete runtime classpath.

---

## 3. High-Level Architectural Guidance
* The MCP server process must output strictly valid JSON-RPC frames to stdout; all diagnostic logs must be directed to `stderr` to prevent protocol stream corruption.
* Single-binary, pure Kotlin architecture preserves cross-platform compatibility across Windows, macOS, and Linux without requiring Node/npm.

---

## 4. Verification & Quality Gates
* **Automated Unit & Integration Tests**: `CompositorMcpServerTest` validates tool registration, execution, and protocol handshakes.
* **Static Analysis**: 100% Detekt compliance with 0 code smells.
* **Build Verification**: `./gradlew test` and `./gradlew publishToMavenLocal` succeed cleanly.
