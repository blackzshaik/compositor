import { describe, it, expect } from 'vitest';
import { parseKotlinPreviews, parsePreviewParameters } from '../parser/preview-ast-parser.js';

describe('Preview AST Parser', () => {
  it('parses raw preview parameter string accurately', () => {
    const raw = '(name = "Dark Mode", widthDp = 360, heightDp = 640, showBackground = true, fontScale = 1.5f)';
    const params = parsePreviewParameters(raw);

    expect(params.name).toBe('Dark Mode');
    expect(params.widthDp).toBe(360);
    expect(params.heightDp).toBe(640);
    expect(params.showBackground).toBe(true);
    expect(params.fontScale).toBe(1.5);
  });

  it('parses positional string argument as name', () => {
    const raw = '("Compact Button")';
    const params = parsePreviewParameters(raw);
    expect(params.name).toBe('Compact Button');
  });

  it('extracts preview from Greeting.kt sample source', () => {
    const kotlinSource = `
package com.compositor.sample

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun Greeting(name: String) {
    Text(text = "Hello, $name!")
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Greeting("Android Developer")
}
`;

    const previews = parseKotlinPreviews(kotlinSource, 'samples/sample-app/src/main/Greeting.kt');
    expect(previews).toHaveLength(1);
    expect(previews[0].functionName).toBe('GreetingPreview');
    expect(previews[0].packageName).toBe('com.compositor.sample');
    expect(previews[0].parameters.showBackground).toBe(true);
    expect(previews[0].filePath).toBe('samples/sample-app/src/main/Greeting.kt');
  });

  it('supports multipreview with multiple @Preview annotations on a single composable', () => {
    const multiPreviewSource = `
package com.example.ui

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable

@Preview(name = "Light Mode", showBackground = true)
@Preview(
    name = "Dark Mode",
    uiMode = "Configuration.UI_MODE_NIGHT_YES",
    showBackground = false
)
@Preview(name = "Large Font", fontScale = 2.0f)
@Composable
fun ComplexButtonPreview() {
    // Composable implementation
}
`;

    const previews = parseKotlinPreviews(multiPreviewSource, 'Button.kt');
    expect(previews).toHaveLength(3);

    expect(previews[0].functionName).toBe('ComplexButtonPreview');
    expect(previews[0].parameters.name).toBe('Light Mode');
    expect(previews[0].parameters.showBackground).toBe(true);

    expect(previews[1].functionName).toBe('ComplexButtonPreview');
    expect(previews[1].parameters.name).toBe('Dark Mode');
    expect(previews[1].parameters.showBackground).toBe(false);

    expect(previews[2].functionName).toBe('ComplexButtonPreview');
    expect(previews[2].parameters.name).toBe('Large Font');
    expect(previews[2].parameters.fontScale).toBe(2.0);
  });

  it('handles files with no previews or normal non-preview composables', () => {
    const plainSource = `
package com.example.model

data class User(val id: String, val name: String)

fun regularFunction() {
    println("No composables here")
}
`;
    const previews = parseKotlinPreviews(plainSource, 'User.kt');
    expect(previews).toHaveLength(0);
  });
});
