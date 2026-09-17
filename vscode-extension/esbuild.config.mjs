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

async function run() {
  if (isWatch) {
    const ctx = await esbuild.context(buildOptions);
    await ctx.watch();
    console.info('[Compositor Extension] Watching for source file changes...');
  } else {
    await esbuild.build(buildOptions);
    console.info('[Compositor Extension] Build completed successfully.');
  }
}

run().catch((err) => {
  console.error('[Compositor Extension] Build failed:', err);
  process.exit(1);
});
