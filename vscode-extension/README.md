# Compositor for VS Code & Cursor

Live, headless Jetpack Compose preview and inspector directly inside VS Code and Cursor.

## Features

- 🎨 **Live Sidebar Canvas**: Preview Jetpack Compose UI side-by-side with code in the primary or secondary sidebar.
- 🪟 **Side-by-Side Editor Tab**: Open an adjacent preview tab that synchronizes with your active Kotlin file.
- 🎯 **Targeted Re-rendering**: Automatically resolves the `@Preview` function under your cursor and triggers instant rasterization.
- 📊 **Real-Time Status Bar**: Monitor daemon status and loaded preview counts with a single click quick menu.
- 🚀 **Zero-Config Lifecycle**: Auto-detects Android project roots, manages background preview daemons, and configures JDK 21 environment.

## Commands

- `Compositor: Open Preview to the Side` — Dock live Compose preview in an adjacent editor tab.
- `Compositor: Focus Preview Sidebar` — Focus the preview canvas in the sidebar.
- `Compositor: Re-render Active Preview` — Trigger immediate rasterization of the composable under your cursor.
- `Compositor: Toggle Preview Theme (Light/Dark)` — Switch between light and dark themes.
- `Compositor: Start Daemon` — Start the background daemon process.
- `Compositor: Stop Daemon` — Gracefully stop the background daemon process.
- `Compositor: Restart Daemon` — Restart the background daemon process.
- `Compositor: Show Actions Menu` — Quick pick menu for all Compositor actions.

## Configuration Settings

- `compositor.autoStartDaemon`: Auto-start daemon upon opening Android workspace (default: `true`).
- `compositor.serverPort`: Preview server HTTP port (default: `3001`).
- `compositor.wsPort`: Real-time preview WebSocket port (default: `3002`).
- `compositor.jdkPath`: Explicit JDK 21 path override.
- `compositor.androidSdkPath`: Explicit Android SDK path override.
