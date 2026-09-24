# Compositor Web Viewer (React 19 + TypeScript + Vite)

> **Primary Native Interface** for the Compositor preview daemon and VS Code extension.

## Tech Stack
* **Framework**: React 19 + TypeScript 5.7
* **Bundler & Dev Server**: Vite 6
* **Styling**: Tailwind CSS v4
* **Icons**: Lucide React
* **Testing**: Vitest 3 + React Testing Library (36 unit & component tests)

## Features
* 📱 **Realistic Device Frames**: Pixel 8, Galaxy S24 Ultra, Foldable, 10" Tablet, Frameless.
* ⚡ **Live WebSocket Sync**: Real-time hot-reloading when Jetpack Compose code changes (`/ws`).
* 🌗 **Theme Switching**: Light / Dark mode toggle.
* 🔠 **Typography Inspector**: 4-scale font comparison (0.85x – 1.5x).
* 🎯 **Element Inspector**: Bounds overlay powered by LayoutLib view bounds metadata.
* 🗂️ **Matrix View Grid**: Compare light vs dark, typography scales, or group variants side-by-side in responsive CSS Grid.
* 🔌 **Dual Hosting**: Runs embedded inside Ktor daemon (port 3001) or as a native VS Code Webview Panel/Sidebar.

## Commands
* `npm run dev` — Local development server with instant HMR (port 5173).
* `npm run build` — Production bundle (~338 KB total, builds in ~2.7s).
* `npm test` — Run Vitest component test suite (36 tests).
