pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

buildscript {
    dependencyLocking.lockFile.set(file("gradle/locks/settings/gradle.lockfile"))
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { mavenCentral() }
}

rootProject.name = "spoolin"
include(":contracts:grpc", ":libraries:backend-support")
include(":services:core", ":services:catalog", ":services:media", ":tests:integration")

// Empty module directories acquire their owner sources in later tasks.
rootProject.children.forEach { parent ->
    parent.children.forEach { module -> module.projectDir.mkdirs() }
}
