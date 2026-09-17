import fs from 'node:fs';
import path from 'node:path';

export interface WorkspaceDetectionResult {
  isAndroidProject: boolean;
  projectRoot: string;
  hasSettingsGradle: boolean;
  hasBuildGradle: boolean;
  androidSdkPath: string | null;
  jdkPath: string | null;
  issues: string[];
}

export class WorkspaceDetector {
  /**
   * Evaluates a project directory to determine if it is an Android project,
   * locating SDK and JDK paths.
   */
  public static detect(workspaceRoot: string, overrides?: {
    jdkPath?: string;
    androidSdkPath?: string;
  }): WorkspaceDetectionResult {
    const issues: string[] = [];

    const settingsGradleKts = path.join(workspaceRoot, 'settings.gradle.kts');
    const settingsGradle = path.join(workspaceRoot, 'settings.gradle');
    const hasSettingsGradle = fs.existsSync(settingsGradleKts) || fs.existsSync(settingsGradle);

    const buildGradleKts = path.join(workspaceRoot, 'build.gradle.kts');
    const buildGradle = path.join(workspaceRoot, 'build.gradle');
    const hasBuildGradle = fs.existsSync(buildGradleKts) || fs.existsSync(buildGradle);

    const isAndroidProject = hasSettingsGradle || hasBuildGradle;
    if (!isAndroidProject) {
      issues.push('No Gradle build files found in workspace root.');
    }

    // 1. Detect Android SDK
    const androidSdkPath = this.resolveAndroidSdk(workspaceRoot, overrides?.androidSdkPath);
    if (!androidSdkPath) {
      issues.push('Android SDK path could not be resolved (checked local.properties, ANDROID_HOME, and defaults).');
    }

    // 2. Detect JDK 21
    const jdkPath = this.resolveJdk(workspaceRoot, overrides?.jdkPath);
    if (!jdkPath) {
      issues.push('JDK 21 path could not be resolved (checked config, gradle.properties, and standard JDK 21 paths).');
    }

    return {
      isAndroidProject,
      projectRoot: workspaceRoot,
      hasSettingsGradle,
      hasBuildGradle,
      androidSdkPath,
      jdkPath,
      issues,
    };
  }

  /**
   * Resolves the Android SDK directory.
   */
  public static resolveAndroidSdk(workspaceRoot: string, overridePath?: string): string | null {
    if (overridePath && fs.existsSync(overridePath)) {
      return overridePath;
    }

    // 1. Check local.properties
    const localPropsPath = path.join(workspaceRoot, 'local.properties');
    if (fs.existsSync(localPropsPath)) {
      try {
        const content = fs.readFileSync(localPropsPath, 'utf-8');
        const match = content.match(/sdk\.dir\s*=\s*(.+)/);
        if (match) {
          const sdkDir = match[1].trim().replace(/\\\\/g, '\\');
          if (fs.existsSync(sdkDir)) {
            return sdkDir;
          }
        }
      } catch {
        // Fall through
      }
    }

    // 2. Check ANDROID_HOME / ANDROID_SDK_ROOT
    const envHome = process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT;
    if (envHome && fs.existsSync(envHome)) {
      return envHome;
    }

    // 3. Check Windows default user location
    const userProfile = process.env.USERPROFILE || process.env.HOME || '';
    if (userProfile) {
      const defaultWinSdk = path.join(userProfile, 'AppData', 'Local', 'Android', 'Sdk');
      if (fs.existsSync(defaultWinSdk)) {
        return defaultWinSdk;
      }
    }

    return null;
  }

  /**
   * Resolves JDK 21 installation path.
   */
  public static resolveJdk(workspaceRoot: string, overridePath?: string): string | null {
    if (overridePath && fs.existsSync(overridePath)) {
      return overridePath;
    }

    // 1. Check gradle.properties in project root
    const gradlePropsPath = path.join(workspaceRoot, 'gradle.properties');
    if (fs.existsSync(gradlePropsPath)) {
      try {
        const content = fs.readFileSync(gradlePropsPath, 'utf-8');
        const match = content.match(/org\.gradle\.java\.home\s*=\s*(.+)/);
        if (match) {
          const javaHome = match[1].trim();
          if (fs.existsSync(javaHome)) {
            return javaHome;
          }
        }
      } catch {
        // Fall through
      }
    }

    // 2. Check Android Studio bundled JDK 21 (Windows)
    const knownJdkPaths = [
      'C:\\Program Files\\Android\\openjdk\\jdk-21.0.8',
      'C:\\Program Files\\Android\\Android Studio\\jbr',
      process.env.JAVA_HOME,
    ];

    for (const candidate of knownJdkPaths) {
      if (candidate && fs.existsSync(candidate)) {
        return candidate;
      }
    }

    return null;
  }
}
