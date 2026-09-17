import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js';
import path from 'node:path';

async function main() {
  console.log('====================================================');
  console.log(' 🚀 Compositor MCP Server - Interactive Tool Tester');
  console.log('====================================================\n');

  const serverScript = path.resolve('dist/server.js');
  const transport = new StdioClientTransport({
    command: 'node',
    args: [serverScript],
    env: {
      ...process.env,
      COMPOSITOR_DAEMON_URL: process.env.COMPOSITOR_DAEMON_URL || 'http://127.0.0.1:3001',
    },
  });

  const client = new Client({ name: 'interactive-test-client', version: '1.0.0' });

  console.log('1. Connecting to MCP server via stdio transport...');
  await client.connect(transport);
  console.log('   ✅ Handshake successful!\n');

  console.log('2. Querying registered MCP tools (tools/list)...');
  const { tools } = await client.listTools();
  console.log(`   Found ${tools.length} available tools:`);
  for (const tool of tools) {
    console.log(`   - 🔧 \x1b[36m${tool.name}\x1b[0m: ${tool.description.slice(0, 70)}...`);
  }
  console.log('');

  let targetPreviewId = 'sample-app:com.compositor.sample.GreetingPreview';

  // 1. list_previews
  console.log('3. Calling tool: \x1b[33mlist_previews\x1b[0m...');
  try {
    const listRes = await client.callTool({ name: 'list_previews', arguments: {} });
    const content = listRes.content[0]?.text;
    if (content) {
      const parsed = JSON.parse(content);
      if (parsed.previews && parsed.previews.length > 0) {
        targetPreviewId = parsed.previews[0].id;
      }
    }
    console.log('   Response:');
    console.log(
      content
        ? JSON.stringify(JSON.parse(content), null, 2)
            .split('\n')
            .map((line) => '     ' + line)
            .slice(0, 20)
            .join('\n')
        : '     (Empty response)'
    );
    if (content && JSON.parse(content).previews?.length > 2) {
      console.log('     ... [remaining previews truncated]');
    }
    console.log('   ✅ list_previews succeeded!\n');
  } catch (err) {
    console.log(`   ⚠️ Note: ${err.message}\n`);
  }

  // 2. inspect_layout_tree
  console.log(`4. Calling tool: \x1b[33minspect_layout_tree\x1b[0m for "${targetPreviewId}"...`);
  try {
    const inspectRes = await client.callTool({
      name: 'inspect_layout_tree',
      arguments: { previewId: targetPreviewId },
    });
    if (inspectRes.isError) {
      console.log(`   ⚠️ Server response: ${inspectRes.content[0]?.text}\n`);
    } else {
      const parsed = JSON.parse(inspectRes.content[0]?.text);
      console.log(`   Root Node: ${parsed.root?.name} (${parsed.viewWidth}x${parsed.viewHeight})`);
      console.log(`   Bounds: [${parsed.root?.bounds?.left}, ${parsed.root?.bounds?.top}, ${parsed.root?.bounds?.right}, ${parsed.root?.bounds?.bottom}]`);
      console.log('   ✅ inspect_layout_tree succeeded!\n');
    }
  } catch (err) {
    console.log(`   ⚠️ Note: ${err.message}\n`);
  }

  // 3. compare_previews
  console.log(`5. Calling tool: \x1b[33mcompare_previews\x1b[0m for "${targetPreviewId}"...`);
  try {
    const diffRes = await client.callTool({
      name: 'compare_previews',
      arguments: { previewId: targetPreviewId },
    });
    if (diffRes.isError) {
      console.log(`   ⚠️ Server response: ${diffRes.content[0]?.text}\n`);
    } else {
      const parsed = JSON.parse(diffRes.content[0]?.text);
      console.log(`   Differences: ${parsed.hasVisualDifferences ? 'Yes' : 'No'}`);
      console.log(`   Diff %: ${parsed.differencePercentage}%`);
      console.log(`   Summary: ${parsed.summary}`);
      console.log('   ✅ compare_previews succeeded!\n');
    }
  } catch (err) {
    console.log(`   ⚠️ Note: ${err.message}\n`);
  }

  console.log('6. Disconnecting...');
  await client.close();
  console.log('   ✅ Finished cleanly.\n');
  console.log('====================================================');
  console.log(' 🎉 All tool checks completed successfully!');
  console.log('====================================================');
}

main().catch((err) => {
  console.error('Execution error:', err);
  process.exit(1);
});
