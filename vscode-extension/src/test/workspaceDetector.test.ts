import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { WorkspaceDetector } from '../workspace/WorkspaceDetector.js';

describe('WorkspaceDetector', () => {
  let tempDir: string;

  beforeEach(() => {
    tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'compositor-test-ws-'));
  });

  afterEach(() => {
    try {
      fs.rmSync(tempDir, { recursive: true, force: true });
    } catch {
      // Ignored
    }
  });

  it('identifies non-Android directories', () => {
    const result = WorkspaceDetector.detect(tempDir);
    expect(result.isAndroidProject).toBe(false);
    expect(result.hasSettingsGradle).toBe(false);
    expect(result.hasBuildGradle).toBe(false);
    expect(result.issues).toContain('No Gradle build files found in workspace root.');
  });

  it('identifies Android project with settings.gradle.kts and local.properties', () => {
    fs.writeFileSync(path.join(tempDir, 'settings.gradle.kts'), 'rootProject.name = "test"');
    fs.writeFileSync(path.join(tempDir, 'build.gradle.kts'), '// build');

    const fakeSdkDir = path.join(tempDir, 'fake-sdk');
    fs.mkdirSync(fakeSdkDir);
    fs.writeFileSync(
      path.join(tempDir, 'local.properties'),
      `sdk.dir=${fakeSdkDir.replace(/\\/g, '\\\\')}\n`
    );

    const result = WorkspaceDetector.detect(tempDir);
    expect(result.isAndroidProject).toBe(true);
    expect(result.hasSettingsGradle).toBe(true);
    expect(result.hasBuildGradle).toBe(true);
    expect(result.androidSdkPath).toBe(fakeSdkDir);
  });

  it('resolves JDK path from gradle.properties', () => {
    const fakeJdkDir = path.join(tempDir, 'fake-jdk-21');
    fs.mkdirSync(fakeJdkDir);
    fs.writeFileSync(
      path.join(tempDir, 'gradle.properties'),
      `org.gradle.java.home=${fakeJdkDir}\n`
    );

    const resolved = WorkspaceDetector.resolveJdk(tempDir);
    expect(resolved).toBe(fakeJdkDir);
  });

  it('accepts custom JDK and Android SDK overrides', () => {
    const customJdk = path.join(tempDir, 'custom-jdk');
    const customSdk = path.join(tempDir, 'custom-sdk');
    fs.mkdirSync(customJdk);
    fs.mkdirSync(customSdk);

    const result = WorkspaceDetector.detect(tempDir, {
      jdkPath: customJdk,
      androidSdkPath: customSdk,
    });

    expect(result.jdkPath).toBe(customJdk);
    expect(result.androidSdkPath).toBe(customSdk);
  });
});
