import { spawn } from 'node:child_process';
import path from 'node:path';

const serverScript = path.resolve('dist/server.js');
const srv = spawn('node', [serverScript], {
  cwd: process.cwd(),
  stdio: ['pipe', 'pipe', 'pipe'],
});

let stdoutData = '';
let stderrData = '';

srv.stdout.on('data', (d) => {
  stdoutData += d.toString();
});
srv.stderr.on('data', (d) => {
  stderrData += d.toString();
});

const initMsg =
  JSON.stringify({
    jsonrpc: '2.0',
    id: 1,
    method: 'initialize',
    params: {
      protocolVersion: '2024-11-05',
      capabilities: {},
      clientInfo: { name: 'smoke-test', version: '1.0.0' },
    },
  }) + '\n';

srv.stdin.write(initMsg);

setTimeout(() => {
  srv.kill();
  console.log('--- STDOUT RECEIVED ---');
  console.log(stdoutData.trim());
  console.log('--- STDERR RECEIVED ---');
  console.log(stderrData.trim());

  if (
    stdoutData.includes('"serverInfo"') &&
    stdoutData.includes('"compositor-mcp"')
  ) {
    console.log('\n[SUCCESS] Stdio JSON-RPC smoke test passed cleanly!');
    process.exit(0);
  } else {
    console.error('\n[FAILURE] Did not receive valid JSON-RPC initialization handshake.');
    process.exit(1);
  }
}, 1200);
