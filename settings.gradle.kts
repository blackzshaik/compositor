pluginManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "compositor"

include(":core-renderer")
include(":plugin")
include(":web-viewer")

val mavenLocalPlugin = file("${System.getProperty("user.home")}/.m2/repository/io/compositor/plugin")
val isPublishing = gradle.startParameter.taskNames.any { it.contains("publishToMavenLocal", ignoreCase = true) }
if (mavenLocalPlugin.exists() && !isPublishing) {
    include(":samples:sample-app")
}

