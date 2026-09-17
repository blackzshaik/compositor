import { PreviewDefinition, PreviewParameters } from '../models/preview.js';

interface RawPreviewAnnotation {
  parametersRaw: string;
  line: number;
}

/**
 * Parses raw argument string from inside `@Preview(...)` into strongly typed `PreviewParameters`.
 */
export function parsePreviewParameters(rawArgs: string): PreviewParameters {
  const params: PreviewParameters = {};
  if (!rawArgs || rawArgs.trim() === '') {
    return params;
  }

  let content = rawArgs.trim();
  if (content.startsWith('(') && content.endsWith(')')) {
    content = content.slice(1, -1).trim();
  }

  // Split content by commas that are NOT inside quotes
  const argTokens: string[] = [];
  let currentToken = '';
  let inQuotes = false;
  let quoteChar = '';

  for (let i = 0; i < content.length; i++) {
    const char = content[i];
    if ((char === '"' || char === "'") && (i === 0 || content[i - 1] !== '\\')) {
      if (!inQuotes) {
        inQuotes = true;
        quoteChar = char;
      } else if (quoteChar === char) {
        inQuotes = false;
      }
    }

    if (char === ',' && !inQuotes) {
      if (currentToken.trim()) {
        argTokens.push(currentToken.trim());
      }
      currentToken = '';
    } else {
      currentToken += char;
    }
  }
  if (currentToken.trim()) {
    argTokens.push(currentToken.trim());
  }

  for (const token of argTokens) {
    const eqIndex = token.indexOf('=');
    if (eqIndex === -1) {
      // Positional parameter: first positional argument in @Preview is `name`
      const val = token.trim();
      if ((val.startsWith('"') && val.endsWith('"')) || (val.startsWith("'") && val.endsWith("'"))) {
        params.name = val.slice(1, -1);
      }
      continue;
    }

    const key = token.slice(0, eqIndex).trim();
    const value = token.slice(eqIndex + 1).trim();

    switch (key) {
      case 'name':
        params.name = value.replace(/^["'](.*)["']$/, '$1');
        break;
      case 'group':
        params.group = value.replace(/^["'](.*)["']$/, '$1');
        break;
      case 'widthDp':
        params.widthDp = parseInt(value, 10);
        break;
      case 'heightDp':
        params.heightDp = parseInt(value, 10);
        break;
      case 'uiMode':
        params.uiMode = value;
        break;
      case 'fontScale':
        params.fontScale = parseFloat(value.replace(/f$/i, ''));
        break;
      case 'showBackground':
        params.showBackground = value === 'true';
        break;
      case 'backgroundColor':
        params.backgroundColor = value;
        break;
    }
  }

  return params;
}

/**
 * Extracts all Preview definitions from a Kotlin source code string.
 */
export function parseKotlinPreviews(source: string, filePath = ''): PreviewDefinition[] {
  const definitions: PreviewDefinition[] = [];
  const lines = source.split(/\r?\n/);

  let packageName = '';
  const currentClasses: string[] = [];

  // Match package statement
  const packageMatch = source.match(/^[ \t]*package\s+([a-zA-Z0-9_.]+)/m);
  if (packageMatch) {
    packageName = packageMatch[1];
  }

  let pendingAnnotations: RawPreviewAnnotation[] = [];
  let inMultiLineAnnotation = false;
  let currentAnnotationText = '';
  let annotationStartLine = 1;

  for (let i = 0; i < lines.length; i++) {
    const lineNum = i + 1;
    const line = lines[i];
    const trimmed = line.trim();

    // Skip empty lines and full-line comments
    if (trimmed.startsWith('//')) {
      continue;
    }

    // Class/Object hierarchy tracking
    const classMatch = trimmed.match(/^(?:(?:public|private|internal|sealed|data)\s+)*(?:class|object|interface)\s+([a-zA-Z0-9_]+)/);
    if (classMatch) {
      currentClasses.push(classMatch[1]);
    }

    // Handle multiline @Preview(...)
    if (inMultiLineAnnotation) {
      currentAnnotationText += ' ' + trimmed;
      if (trimmed.includes(')')) {
        inMultiLineAnnotation = false;
        const innerMatch = currentAnnotationText.match(/@(?:androidx\.compose\.ui\.tooling\.preview\.)?Preview\s*(\([^)]*\))?/s);
        if (innerMatch) {
          pendingAnnotations.push({
            parametersRaw: innerMatch[1] ?? '',
            line: annotationStartLine,
          });
        }
        currentAnnotationText = '';
      }
      continue;
    }

    // Check if line contains a function declaration: `fun <name>(...)`
    const funMatch = trimmed.match(/^(?:(?:public|private|internal|protected)\s+)*(?:@Composable\s+)?fun\s+([a-zA-Z0-9_]+)\s*\(/);
    if (funMatch) {
      const functionName = funMatch[1];
      if (pendingAnnotations.length > 0) {
        for (const annotation of pendingAnnotations) {
          const parsedParams = parsePreviewParameters(annotation.parametersRaw);
          definitions.push({
            functionName,
            enclosingClass: currentClasses.length > 0 ? currentClasses[currentClasses.length - 1] : undefined,
            packageName,
            parameters: parsedParams,
            line: annotation.line,
            filePath,
          });
        }
        pendingAnnotations = [];
      }
      continue;
    }

    // Look for @Preview or @androidx.compose.ui.tooling.preview.Preview
    const previewRegex = /@(?:androidx\.compose\.ui\.tooling\.preview\.)?Preview(?:\s*(\([^)]*\))|\s*(\([^)]*$)|(?!\w))/g;
    let match: RegExpExecArray | null;
    let hasPreviewOnLine = false;

    while ((match = previewRegex.exec(trimmed)) !== null) {
      hasPreviewOnLine = true;
      if (match[2] !== undefined) {
        // Multi-line annotation started
        inMultiLineAnnotation = true;
        annotationStartLine = lineNum;
        currentAnnotationText = match[0];
        break;
      } else {
        pendingAnnotations.push({
          parametersRaw: match[1] ?? '',
          line: lineNum,
        });
      }
    }

    if (hasPreviewOnLine) {
      continue;
    }

    if (
      !trimmed.startsWith('@') &&
      pendingAnnotations.length > 0 &&
      !trimmed.startsWith('/*') &&
      !trimmed.startsWith('*') &&
      !trimmed.endsWith('*/')
    ) {
      if (!trimmed.includes('fun ') && !trimmed.startsWith('fun')) {
        pendingAnnotations = [];
      }
    }
  }

  return definitions;
}
