import { describe, it, expect } from 'vitest';
import { extractComposableFunctions, resolvePreviewAtCursor } from '../commands/cursorResolver.js';

describe('cursorResolver', () => {
  const sampleKotlinCode = `
package com.example.sampleapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Light Mode")
@Composable
fun GreetingPreview() {
    Greeting("Android")
}

@Preview(name = "Dark Mode")
@Composable
fun GreetingDarkPreview() {
    Greeting("Android Dark")
}

@Composable
fun RegularHelper() {
    println("helper")
}
`.trim();

  it('extracts package name and all composable / preview functions', () => {
    const { packageName, functions } = extractComposableFunctions(sampleKotlinCode);
    expect(packageName).toBe('com.example.sampleapp');
    expect(functions).toHaveLength(3);

    expect(functions[0].functionName).toBe('GreetingPreview');
    expect(functions[0].previewName).toBe('Light Mode');
    expect(functions[0].previewId).toBe('com.example.sampleapp.GreetingPreview');
    expect(functions[0].hasPreviewAnnotation).toBe(true);

    expect(functions[1].functionName).toBe('GreetingDarkPreview');
    expect(functions[1].previewName).toBe('Dark Mode');
    expect(functions[1].previewId).toBe('com.example.sampleapp.GreetingDarkPreview');
    expect(functions[1].hasPreviewAnnotation).toBe(true);

    expect(functions[2].functionName).toBe('RegularHelper');
    expect(functions[2].hasPreviewAnnotation).toBe(false);
  });

  it('resolves GreetingPreview when cursor is inside GreetingPreview body', () => {
    const target = resolvePreviewAtCursor(sampleKotlinCode, 8);
    expect(target).not.toBeNull();
    expect(target?.functionName).toBe('GreetingPreview');
    expect(target?.previewId).toBe('com.example.sampleapp.GreetingPreview');
  });

  it('resolves GreetingDarkPreview when cursor is inside GreetingDarkPreview body', () => {
    const target = resolvePreviewAtCursor(sampleKotlinCode, 14);
    expect(target).not.toBeNull();
    expect(target?.functionName).toBe('GreetingDarkPreview');
  });

  it('resolves nearest preview when cursor is outside functions', () => {
    const target = resolvePreviewAtCursor(sampleKotlinCode, 0);
    expect(target).not.toBeNull();
    expect(target?.functionName).toBe('GreetingPreview');
  });

  it('handles files with no previews or composables', () => {
    const code = 'package com.test\n\nfun helper() = 42\n';
    const { functions } = extractComposableFunctions(code);
    expect(functions).toHaveLength(0);

    const target = resolvePreviewAtCursor(code, 2);
    expect(target).toBeNull();
  });
});
