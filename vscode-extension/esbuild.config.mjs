import * as esbuild from 'esbuild';

const isWatch = process.argv.includes('--watch');

const buildOptions = {
  entryPoints: ['src/extension.ts'],
  bundle: true,
  outfile: 'dist/extension.js',
  external: ['vscode'],
  format: 'cjs',
  platform: 'node',
  target: 'node20',
  sourcemap: true,
  logLevel: 'info',
};

import fs from 'node:fs';
import path from 'node:path';

function copyWebViewerDist() {
  const src = path.resolve('..', 'web-viewer-react', 'dist');
  const dst = path.resolve('dist', 'web-viewer');
  if (fs.existsSync(src)) {
    fs.mkdirSync(dst, { recursive: true });
    fs.cpSync(src, dst, { recursive: true });
    console.info('[Compositor Extension] Bundled web-viewer-react/dist into dist/web-viewer');
  }
}

async function run() {
  if (isWatch) {
    const ctx = await esbuild.context(buildOptions);
    await ctx.watch();
    console.info('[Compositor Extension] Watching for source file changes...');
  } else {
    await esbuild.build(buildOptions);
    copyWebViewerDist();
    console.info('[Compositor Extension] Build completed successfully.');
  }
}

run().catch((err) => {
  console.error('[Compositor Extension] Build failed:', err);
  process.exit(1);
});
