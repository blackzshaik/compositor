import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { DaemonClient } from './client/daemon-client.js';
import { BaselineManager } from './diff/visual-diff.js';
import { registerCompositorTools } from './tools/preview-tools.js';

export interface ServerInstanceOptions {
  daemonClient?: DaemonClient;
  baselineManager?: BaselineManager;
}

export interface CompositorMcpInstance {
  server: McpServer;
  daemonClient: DaemonClient;
  baselineManager: BaselineManager;
}

/**
 * Creates and configures the Compositor MCP Server instance with all tools registered.
 */
export function createCompositorMcpServer(
  options: ServerInstanceOptions = {}
): CompositorMcpInstance {
  const daemonClient = options.daemonClient ?? new DaemonClient();
  const baselineManager = options.baselineManager ?? new BaselineManager();

  const server = new McpServer({
    name: 'compositor-mcp',
    version: '0.1.0',
  });

  registerCompositorTools(server, { daemonClient, baselineManager });

  return {
    server,
    daemonClient,
    baselineManager,
  };
}

/**
 * Launches the MCP server connected to stdio transport for IDE/agent integration.
 * Ensures all diagnostic logs are directed to stderr to protect JSON-RPC stdout integrity.
 */
export async function runStdioServer(): Promise<void> {
  // Guarantee stdout is strictly reserved for JSON-RPC messages
  // eslint-disable-next-line no-console
  const originalLog = console.log;
  // eslint-disable-next-line no-console
  console.log = (...args: unknown[]) => {
    console.error(...args);
  };
  console.info = (...args: unknown[]) => {
    console.error(...args);
  };

  console.error('[Compositor MCP] Initializing Compositor MCP Server...');

  const { server, daemonClient } = createCompositorMcpServer();

  // Non-blocking preflight check to log daemon status to stderr
  daemonClient
    .isOnline()
    .then((online) => {
      if (online) {
        console.error(`[Compositor MCP] Connected to Compositor Daemon at ${daemonClient.getBaseUrl()}`);
      } else {
        console.error(
          `[Compositor MCP] Warning: Compositor Daemon is offline at ${daemonClient.getBaseUrl()}.\n` +
            `[Compositor MCP] Tools will provide actionable start guidance until daemon is launched.`
        );
      }
    })
    .catch((err) => {
      console.error('[Compositor MCP] Daemon preflight warning:', err);
    });

  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error('[Compositor MCP] Server connected to stdio transport. Ready for JSON-RPC requests.');

  // Clean shutdown handlers
  const shutdown = async () => {
    console.error('[Compositor MCP] Shutting down...');
    try {
      await server.close();
    } catch {
      // ignore
    }
    // eslint-disable-next-line no-console
    console.log = originalLog;
    process.exit(0);
  };

  process.on('SIGINT', shutdown);
  process.on('SIGTERM', shutdown);
}

// Auto-run if executed as CLI entry point
if (process.argv[1] && (import.meta.url.endsWith(process.argv[1]) || process.argv[1].endsWith('server.ts') || process.argv[1].endsWith('server.js'))) {
  runStdioServer().catch((err) => {
    console.error('[Compositor MCP] Fatal error running stdio server:', err);
    process.exit(1);
  });
}
