import * as vscode from 'vscode';
import { ChildProcess, spawn } from 'node:child_process';
import path from 'node:path';
import fs from 'node:fs';
import { WebSocket } from 'ws';
import { DEFAULTS } from '../constants.js';

export interface DaemonConfig {
  workspaceRoot: string;
  httpPort: number;
  wsPort: number;
  jdkPath?: string;
  androidSdkPath?: string;
}

export interface DaemonStatusInfo {
  status: string;
  totalPreviews: number;
  uptimeSeconds: number;
}

export type DaemonEventListener = (event: string, payload: Record<string, unknown>) => void;

export class DaemonManager {
  private _process: ChildProcess | null = null;
  private _wsClient: WebSocket | null = null;
  private _outputChannel: vscode.OutputChannel;
  private _config: DaemonConfig;
  private _statusPollTimer: NodeJS.Timeout | null = null;
  private _isDisposed = false;
  private _listeners: DaemonEventListener[] = [];
  private _totalPreviews = 0;
  private _isConnected = false;

  constructor(config: DaemonConfig, outputChannel?: vscode.OutputChannel) {
    this._config = config;
    this._outputChannel = outputChannel ?? vscode.window.createOutputChannel(DEFAULTS.OUTPUT_CHANNEL_NAME);
  }

  public addListener(listener: DaemonEventListener): vscode.Disposable {
    this._listeners.push(listener);
    return {
      dispose: () => {
        this._listeners = this._listeners.filter((l) => l !== listener);
      },
    };
  }

  public get isConnected(): boolean {
    return this._isConnected;
  }

  public get totalPreviews(): number {
    return this._totalPreviews;
  }

  public async start(): Promise<boolean> {
    this._outputChannel.appendLine(`[Compositor] Checking for running daemon on port ${this._config.httpPort}...`);

    // 1. Check if daemon is already running (e.g. CLI daemon launched externally)
    const isAlreadyRunning = await this.checkHealth();
    if (isAlreadyRunning) {
      this._outputChannel.appendLine(`[Compositor] Detected active preview daemon on port ${this._config.httpPort}.`);
      this._isConnected = true;
      this.connectWebSocket();
      this.startStatusPolling();
      return true;
    }

    // 2. Spawn daemon child process
    return this.spawnDaemonProcess();
  }

  public async stop(): Promise<void> {
    this.stopStatusPolling();

    if (this._wsClient) {
      try {
        this._wsClient.close();
      } catch {
        // Ignored
      }
      this._wsClient = null;
    }

    if (this._process) {
      this._outputChannel.appendLine('[Compositor] Stopping preview daemon process...');
      this._process.kill('SIGTERM');
      this._process = null;
    }

    this._isConnected = false;
    this._totalPreviews = 0;
    this.notifyListeners('DAEMON_STOPPED', {});
  }

  public async restart(): Promise<boolean> {
    await this.stop();
    return this.start();
  }

  public async triggerRender(previewId: string): Promise<boolean> {
    try {
      const url = `http://localhost:${this._config.httpPort}/api/previews/${encodeURIComponent(previewId)}/render`;
      const res = await fetch(url, { method: 'POST' });
      return res.ok;
    } catch (err) {
      this._outputChannel.appendLine(`[Compositor] Failed to trigger render for ${previewId}: ${String(err)}`);
      return false;
    }
  }

  public async checkHealth(): Promise<boolean> {
    try {
      const res = await fetch(`http://localhost:${this._config.httpPort}/api/daemon/status`, {
        signal: AbortSignal.timeout(1500),
      });
      if (res.ok) {
        const data = (await res.json()) as DaemonStatusInfo;
        this._totalPreviews = data.totalPreviews ?? 0;
        return true;
      }
      return false;
    } catch {
      return false;
    }
  }

  private spawnDaemonProcess(): boolean {
    const cliServerTs = path.join(this._config.workspaceRoot, 'cli', 'src', 'server.ts');
    if (!fs.existsSync(cliServerTs)) {
      this._outputChannel.appendLine(
        `[Compositor Error] Daemon entrypoint not found at ${cliServerTs}. Cannot start daemon.`
      );
      return false;
    }

    this._outputChannel.appendLine(`[Compositor] Launching Compositor daemon process...`);

    // Prepare process environment with JDK 21 enforcement
    const env: NodeJS.ProcessEnv = { ...process.env };
    if (this._config.jdkPath) {
      env.JAVA_HOME = this._config.jdkPath;
      const binPath = path.join(this._config.jdkPath, 'bin');
      env.PATH = `${binPath}${path.delimiter}${env.PATH ?? ''}`;
    }

    // Spawn via npx tsx cli/src/server.ts inside workspace root
    const isWindows = process.platform === 'win32';
    const npmCmd = isWindows ? 'npx.cmd' : 'npx';

    this._process = spawn(npmCmd, ['tsx', 'cli/src/server.ts'], {
      cwd: this._config.workspaceRoot,
      env,
      shell: isWindows,
    });

    this._process.stdout?.on('data', (chunk: Buffer) => {
      const text = chunk.toString().trimEnd();
      this._outputChannel.appendLine(`[Daemon stdout] ${text}`);
    });

    this._process.stderr?.on('data', (chunk: Buffer) => {
      const text = chunk.toString().trimEnd();
      this._outputChannel.appendLine(`[Daemon stderr] ${text}`);
    });

    this._process.on('error', (err) => {
      this._outputChannel.appendLine(`[Compositor Process Error] ${err.message}`);
      this._isConnected = false;
      this.notifyListeners('DAEMON_ERROR', { error: err.message });
    });

    this._process.on('exit', (code, signal) => {
      this._outputChannel.appendLine(`[Compositor] Daemon exited with code ${code}, signal ${signal}.`);
      this._isConnected = false;
      this.notifyListeners('DAEMON_STOPPED', { code, signal });
    });

    // Wait and verify health
    setTimeout(async () => {
      const isHealthy = await this.checkHealth();
      if (isHealthy) {
        this._isConnected = true;
        this.connectWebSocket();
        this.startStatusPolling();
        this.notifyListeners('DAEMON_STARTED', { port: this._config.httpPort });
      }
    }, 2500);

    return true;
  }

  private connectWebSocket(): void {
    if (this._isDisposed) return;

    try {
      this._wsClient = new WebSocket(`ws://localhost:${this._config.wsPort}`);

      this._wsClient.on('open', () => {
        this._outputChannel.appendLine(`[Compositor] WebSocket sync connected to ws://localhost:${this._config.wsPort}`);
        this.notifyListeners('WS_CONNECTED', {});
      });

      this._wsClient.on('message', (data: Buffer | string) => {
        try {
          const parsed = JSON.parse(data.toString()) as { event: string; payload: Record<string, unknown> };
          this.notifyListeners(parsed.event, parsed.payload);
        } catch {
          // Ignored
        }
      });

      this._wsClient.on('close', () => {
        if (!this._isDisposed && this._isConnected) {
          setTimeout(() => this.connectWebSocket(), DEFAULTS.RECONNECT_INTERVAL_MS);
        }
      });

      this._wsClient.on('error', () => {
        // Ignored
      });
    } catch (err) {
      this._outputChannel.appendLine(`[Compositor] Failed to connect WebSocket: ${String(err)}`);
    }
  }

  private startStatusPolling(): void {
    this.stopStatusPolling();
    this._statusPollTimer = setInterval(async () => {
      if (this._isDisposed) return;
      const healthy = await this.checkHealth();
      if (healthy !== this._isConnected) {
        this._isConnected = healthy;
        this.notifyListeners(healthy ? 'DAEMON_ONLINE' : 'DAEMON_OFFLINE', {
          totalPreviews: this._totalPreviews,
        });
      }
    }, DEFAULTS.STATUS_POLL_INTERVAL_MS);
  }

  private stopStatusPolling(): void {
    if (this._statusPollTimer) {
      clearInterval(this._statusPollTimer);
      this._statusPollTimer = null;
    }
  }

  private notifyListeners(event: string, payload: Record<string, unknown>): void {
    for (const listener of this._listeners) {
      try {
        listener(event, payload);
      } catch (err) {
        console.error('[Compositor DaemonManager] Listener error:', err);
      }
    }
  }

  public dispose(): void {
    this._isDisposed = true;
    this.stop();
  }
}
