export interface ResolvedPreviewTarget {
  packageName: string;
  functionName: string;
  previewId: string;
  previewName?: string;
  startLine: number;
  endLine: number;
  hasPreviewAnnotation: boolean;
}

/**
 * Parses Kotlin source text to extract package name and Composable/Preview functions
 * with their respective line ranges.
 */
export function extractComposableFunctions(sourceText: string): {
  packageName: string;
  functions: ResolvedPreviewTarget[];
} {
  const lines = sourceText.split(/\r?\n/);
  let packageName = '';

  // 1. Extract package name
  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed.startsWith('package ')) {
      const match = trimmed.match(/^package\s+([a-zA-Z0-9_.]+)/);
      if (match) {
        packageName = match[1];
        break;
      }
    }
  }

  const functions: ResolvedPreviewTarget[] = [];

  // 2. Track annotations and function declarations
  let pendingAnnotations: string[] = [];
  let annotationStartLine = -1;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const trimmed = line.trim();

    if (trimmed.startsWith('@')) {
      if (pendingAnnotations.length === 0) {
        annotationStartLine = i;
      }
      pendingAnnotations.push(trimmed);
      continue;
    }

    const funMatch = trimmed.match(/(?:(?:public|private|internal|protected)\s+)?fun\s+([A-Za-z0-9_]+)\s*\(/);
    if (funMatch) {
      const functionName = funMatch[1];
      const hasPreview = pendingAnnotations.some((a) => a.includes('@Preview'));
      const hasComposable = pendingAnnotations.some((a) => a.includes('@Composable'));

      if (hasPreview || hasComposable) {
        // Extract preview name parameter if present, e.g., @Preview(name = "Light Mode")
        let previewName: string | undefined;
        for (const ann of pendingAnnotations) {
          const nameMatch = ann.match(/name\s*=\s*"([^"]+)"/);
          if (nameMatch) {
            previewName = nameMatch[1];
            break;
          }
        }

        // Determine end of function by tracking braces
        let braceCount = 0;
        let foundOpenBrace = false;
        let endLine = i;

        for (let j = i; j < lines.length; j++) {
          const checkLine = lines[j];
          for (const char of checkLine) {
            if (char === '{') {
              braceCount++;
              foundOpenBrace = true;
            } else if (char === '}') {
              braceCount--;
            }
          }

          if (foundOpenBrace && braceCount <= 0) {
            endLine = j;
            break;
          }
          endLine = j;
        }

        const startLine = annotationStartLine >= 0 ? annotationStartLine : i;
        const previewId = packageName ? `${packageName}.${functionName}` : functionName;

        functions.push({
          packageName,
          functionName,
          previewId,
          previewName,
          startLine,
          endLine,
          hasPreviewAnnotation: hasPreview,
        });
      }

      pendingAnnotations = [];
      annotationStartLine = -1;
      continue;
    }

    if (trimmed.length > 0 && !trimmed.startsWith('//') && !trimmed.startsWith('/*')) {
      // Non-annotation, non-function line resets pending annotations if not an ongoing annotation block
      if (!trimmed.endsWith(')') && !trimmed.startsWith('@')) {
        pendingAnnotations = [];
        annotationStartLine = -1;
      }
    }
  }

  return { packageName, functions };
}

/**
 * Resolves the Composable / Preview target closest to or enclosing the specified cursor line (0-indexed).
 */
export function resolvePreviewAtCursor(
  sourceText: string,
  cursorLine: number
): ResolvedPreviewTarget | null {
  const { functions } = extractComposableFunctions(sourceText);
  if (functions.length === 0) {
    return null;
  }

  // 1. Direct enclosing match: cursor is strictly within the function body
  const enclosing = functions.find(
    (fn) => cursorLine >= fn.startLine && cursorLine <= fn.endLine
  );
  if (enclosing) {
    return enclosing;
  }

  // 2. Nearest function preference: find closest function to cursor
  let closestFn: ResolvedPreviewTarget = functions[0];
  let minDistance = Number.MAX_SAFE_INTEGER;

  for (const fn of functions) {
    // Distance from cursor to function range
    let distance = 0;
    if (cursorLine < fn.startLine) {
      distance = fn.startLine - cursorLine;
    } else if (cursorLine > fn.endLine) {
      distance = cursorLine - fn.endLine;
    }

    // Prefer @Preview over plain @Composable if distances are equal
    const isPreferredPreview = distance === minDistance &&
      fn.hasPreviewAnnotation &&
      !closestFn.hasPreviewAnnotation;
    if (distance < minDistance || isPreferredPreview) {
      minDistance = distance;
      closestFn = fn;
    }
  }

  return closestFn;
}
