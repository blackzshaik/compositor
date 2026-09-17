import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { DaemonClient, CompositorDaemonError } from '../client/daemon-client.js';
import { BaselineManager, comparePngBuffers } from '../diff/visual-diff.js';

export interface RegisterToolsOptions {
  daemonClient: DaemonClient;
  baselineManager?: BaselineManager;
}

export function registerCompositorTools(
  server: McpServer,
  options: RegisterToolsOptions
): void {
  const { daemonClient } = options;
  const baselineManager = options.baselineManager ?? new BaselineManager();

  // 1. Tool: list_previews
  server.tool(
    'list_previews',
    'Discovers all @Preview composables in the Android project, returning their IDs, file paths, and render statuses.',
    {
      filter: z
        .string()
        .optional()
        .describe('Optional search query to filter by composable name, package, module, or group.'),
    },
    async (args) => {
      try {
        const previews = await daemonClient.listPreviews(args.filter);
        const summaries = previews.map((item) => ({
          id: item.id,
          module: item.module,
          functionName: item.definition.functionName,
          packageName: item.definition.packageName,
          filePath: item.definition.filePath,
          line: item.definition.line,
          status: item.status,
          parameters: item.definition.parameters,
          lastRenderedAt: item.lastRenderedAt,
          durationMs: item.durationMs,
        }));

        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify(
                {
                  totalCount: summaries.length,
                  previews: summaries,
                },
                null,
                2
              ),
            },
          ],
        };
      } catch (err: unknown) {
        return {
          isError: true,
          content: [
            {
              type: 'text',
              text: err instanceof CompositorDaemonError ? err.message : String(err),
            },
          ],
        };
      }
    }
  );

  // 2. Tool: render_preview
  server.tool(
    'render_preview',
    'Triggers an on-demand render of a composable preview, returning status, duration, and any compilation errors.',
    {
      previewId: z
        .string()
        .describe('The unique preview identifier (e.g. com.compositor.sample.GreetingPreview).'),
      theme: z
        .enum(['light', 'dark'])
        .optional()
        .describe('Optional theme mode override (light or dark).'),
      fontScale: z
        .number()
        .min(0.5)
        .max(2.5)
        .optional()
        .describe('Optional font scale factor (e.g. 1.0, 1.25).'),
      timeoutMs: z
        .number()
        .optional()
        .describe('Maximum milliseconds to wait for render completion (default 20000).'),
    },
    async (args) => {
      try {
        const result = await daemonClient.renderPreview(args.previewId, {
          theme: args.theme,
          fontScale: args.fontScale,
          timeoutMs: args.timeoutMs,
        });

        if (!result.success) {
          return {
            isError: true,
            content: [
              {
                type: 'text',
                text:
                  `Failed to render preview "${args.previewId}".\n` +
                  `Status: ${result.status}\n` +
                  `Error Details: ${result.error ?? 'Unknown render error'}`,
              },
            ],
          };
        }

        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify(
                {
                  success: true,
                  previewId: result.previewId,
                  status: result.status,
                  durationMs: result.durationMs,
                  imageUrl: result.imageUrl,
                },
                null,
                2
              ),
            },
          ],
        };
      } catch (err: unknown) {
        return {
          isError: true,
          content: [
            {
              type: 'text',
              text: err instanceof CompositorDaemonError ? err.message : String(err),
            },
          ],
        };
      }
    }
  );

  // 3. Tool: get_preview_image
  server.tool(
    'get_preview_image',
    'Retrieves the rendered PNG image for a composable preview as an image content block for multimodal LLMs.',
    {
      previewId: z
        .string()
        .describe('The preview identifier whose rendered image is requested.'),
    },
    async (args) => {
      try {
        const base64Data = await daemonClient.getPreviewImageBase64(args.previewId);

        return {
          content: [
            {
              type: 'image',
              data: base64Data,
              mimeType: 'image/png',
            },
            {
              type: 'text',
              text: `Rendered preview bitmap for "${args.previewId}".`,
            },
          ],
        };
      } catch (err: unknown) {
        return {
          isError: true,
          content: [
            {
              type: 'text',
              text: err instanceof CompositorDaemonError ? err.message : String(err),
            },
          ],
        };
      }
    }
  );

  // 4. Tool: inspect_layout_tree
  server.tool(
    'inspect_layout_tree',
    'Retrieves the parsed layout hierarchy, coordinate bounds, padding, and semantics for spatial inspection.',
    {
      previewId: z
        .string()
        .describe('The preview identifier whose layout tree is requested.'),
    },
    async (args) => {
      try {
        const hierarchy = await daemonClient.getLayoutHierarchy(args.previewId);

        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify(hierarchy, null, 2),
            },
          ],
        };
      } catch (err: unknown) {
        return {
          isError: true,
          content: [
            {
              type: 'text',
              text: err instanceof CompositorDaemonError ? err.message : String(err),
            },
          ],
        };
      }
    }
  );

  // 5. Tool: compare_previews
  server.tool(
    'compare_previews',
    'Compares a rendered preview against a baseline snapshot or another preview, reporting visual diff percentage.',
    {
      previewId: z
        .string()
        .describe('Primary preview identifier to compare.'),
      baselineTimestamp: z
        .number()
        .optional()
        .describe('Optional timestamp of a historical baseline snapshot to compare against.'),
      compareWithPreviewId: z
        .string()
        .optional()
        .describe('Optional second preview ID to compare against instead of a baseline snapshot.'),
      saveCurrentAsBaseline: z
        .boolean()
        .optional()
        .describe('If true, updates the baseline snapshot with the current render.'),
    },
    async (args) => {
      try {
        const currentBuffer = await daemonClient.getPreviewImageBuffer(args.previewId);

        let baselineBuffer: Buffer | null = null;
        let baselineSourceDesc = '';

        if (args.compareWithPreviewId) {
          baselineBuffer = await daemonClient.getPreviewImageBuffer(args.compareWithPreviewId);
          baselineSourceDesc = `preview "${args.compareWithPreviewId}"`;
        } else {
          baselineBuffer = baselineManager.loadBaseline(args.previewId, args.baselineTimestamp);
          baselineSourceDesc = args.baselineTimestamp
            ? `baseline snapshot from ${new Date(args.baselineTimestamp).toISOString()}`
            : 'default baseline snapshot';
        }

        if (!baselineBuffer) {
          // No baseline existed yet - save current render as the new initial baseline
          baselineManager.saveBaseline(args.previewId, currentBuffer);
          return {
            content: [
              {
                type: 'text',
                text: JSON.stringify(
                  {
                    hasVisualDifferences: false,
                    differencePercentage: 0,
                    summary:
                      `No existing baseline was found for preview "${args.previewId}". ` +
                      `The current render has been established and saved as the initial baseline.`,
                    baselineEstablished: true,
                  },
                  null,
                  2
                ),
              },
            ],
          };
        }

        const diffResult = comparePngBuffers(currentBuffer, baselineBuffer);

        if (args.saveCurrentAsBaseline) {
          baselineManager.saveBaseline(args.previewId, currentBuffer);
        }

        const resultPayload = {
          previewId: args.previewId,
          comparedAgainst: baselineSourceDesc,
          hasVisualDifferences: diffResult.hasVisualDifferences,
          differencePercentage: diffResult.differencePercentage,
          pixelsDifferent: diffResult.pixelCountDifferent,
          totalPixels: diffResult.totalPixels,
          summary: diffResult.summary,
        };

        const responseContent: (
          | { type: 'text'; text: string }
          | { type: 'image'; data: string; mimeType: 'image/png' }
        )[] = [
          {
            type: 'text',
            text: JSON.stringify(resultPayload, null, 2),
          },
        ];

        if (diffResult.hasVisualDifferences && diffResult.diffImageBase64) {
          responseContent.push({
            type: 'image',
            data: diffResult.diffImageBase64,
            mimeType: 'image/png',
          });
        }

        return {
          content: responseContent,
        };
      } catch (err: unknown) {
        return {
          isError: true,
          content: [
            {
              type: 'text',
              text: err instanceof CompositorDaemonError ? err.message : String(err),
            },
          ],
        };
      }
    }
  );
}
