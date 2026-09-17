import { vi } from 'vitest';

export class Uri {
  public scheme: string;
  public path: string;
  public fsPath: string;

  constructor(pathStr: string, scheme = 'file') {
    this.fsPath = pathStr;
    this.path = pathStr.replace(/\\/g, '/');
    this.scheme = scheme;
  }

  public static file(filePath: string): Uri {
    return new Uri(filePath, 'file');
  }

  public static parse(val: string): Uri {
    return new Uri(val, 'parsed');
  }

  public static joinPath(base: Uri, ...segments: string[]): Uri {
    const joined = [base.fsPath, ...segments].join('/');
    return new Uri(joined, base.scheme);
  }

  public toString(): string {
    return `${this.scheme}://${this.path}`;
  }
}

export class Disposable {
  private readonly _callOnDispose: () => unknown;

  constructor(callOnDispose: () => unknown) {
    this._callOnDispose = callOnDispose;
  }

  public dispose(): void {
    this._callOnDispose();
  }

  public static from(...disposables: { dispose(): unknown }[]): Disposable {
    return new Disposable(() => disposables.forEach((d) => d.dispose()));
  }
}

export const StatusBarAlignment = {
  Left: 1,
  Right: 2,
} as const;

export const ViewColumn = {
  One: 1,
  Two: 2,
  Three: 3,
  Beside: -2,
} as const;

export const window = {
  createOutputChannel: vi.fn((name: string) => ({
    name,
    appendLine: vi.fn(),
    append: vi.fn(),
    clear: vi.fn(),
    show: vi.fn(),
    hide: vi.fn(),
    dispose: vi.fn(),
  })),
  createStatusBarItem: vi.fn(() => ({
    text: '',
    tooltip: '',
    command: '',
    backgroundColor: undefined,
    show: vi.fn(),
    hide: vi.fn(),
    dispose: vi.fn(),
  })),
  showInformationMessage: vi.fn(),
  showWarningMessage: vi.fn(),
  showErrorMessage: vi.fn(),
  showQuickPick: vi.fn(),
  registerWebviewViewProvider: vi.fn(),
  createWebviewPanel: vi.fn(() => ({
    webview: {
      html: '',
      options: {},
      asWebviewUri: (uri: Uri) => ({ toString: () => `vscode-resource://${uri.path}` }),
      onDidReceiveMessage: vi.fn(),
      postMessage: vi.fn(),
      cspSource: 'vscode-webview:',
    },
    reveal: vi.fn(),
    onDidDispose: vi.fn(),
    dispose: vi.fn(),
  })),
  onDidChangeActiveTextEditor: vi.fn(() => new Disposable(() => {})),
  activeTextEditor: undefined,
};

export const workspace = {
  workspaceFolders: [],
  getConfiguration: vi.fn(() => ({
    get: vi.fn((_key: string, defaultVal: unknown) => defaultVal),
  })),
  onDidChangeConfiguration: vi.fn(() => new Disposable(() => {})),
};

export const commands = {
  registerCommand: vi.fn((cmd: string, _callback: (...args: unknown[]) => unknown) => ({
    dispose: vi.fn(),
    command: cmd,
  })),
  executeCommand: vi.fn(),
};

export interface CancellationToken {
  isCancellationRequested: boolean;
  onCancellationRequested: (listener: () => unknown) => Disposable;
}

export interface WebviewViewResolveContext {
  state?: unknown;
}

export interface WebviewView {
  description?: string;
  badge?: unknown;
  visible: boolean;
  webview: {
    html: string;
    options: unknown;
    asWebviewUri(uri: Uri): { toString(): string };
    postMessage(message: unknown): Thenable<boolean>;
    onDidReceiveMessage: (listener: (e: unknown) => unknown) => Disposable;
    cspSource: string;
  };
  onDidChangeVisibility: (listener: () => unknown) => Disposable;
  onDidDispose: (listener: () => unknown) => Disposable;
}

export interface WebviewViewProvider {
  resolveWebviewView(
    webviewView: WebviewView,
    context: WebviewViewResolveContext,
    token: CancellationToken
  ): Thenable<void> | void;
}
