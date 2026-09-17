export const COMMANDS = {
  OPEN_PREVIEW_TO_SIDE: 'compositor.openPreviewToSide',
  FOCUS_SIDEBAR: 'compositor.focusSidebar',
  RERENDER_ACTIVE_PREVIEW: 'compositor.rerenderActivePreview',
  TOGGLE_THEME: 'compositor.toggleTheme',
  START_DAEMON: 'compositor.startDaemon',
  STOP_DAEMON: 'compositor.stopDaemon',
  RESTART_DAEMON: 'compositor.restartDaemon',
  SHOW_QUICK_MENU: 'compositor.showQuickMenu',
} as const;

export const VIEWS = {
  PREVIEW_VIEW: 'compositor.previewView',
} as const;

export const CONFIG_KEYS = {
  AUTO_START_DAEMON: 'compositor.autoStartDaemon',
  SERVER_PORT: 'compositor.serverPort',
  WS_PORT: 'compositor.wsPort',
  JDK_PATH: 'compositor.jdkPath',
  ANDROID_SDK_PATH: 'compositor.androidSdkPath',
} as const;

export const DEFAULTS = {
  HTTP_PORT: 3001,
  WS_PORT: 3002,
  OUTPUT_CHANNEL_NAME: 'Compositor Output',
  RECONNECT_INTERVAL_MS: 3000,
  STATUS_POLL_INTERVAL_MS: 5000,
} as const;
