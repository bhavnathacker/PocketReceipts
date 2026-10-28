pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") {
            content {
                includeGroup("com.github.leonard-palm")
            }
        }
    }
}

rootProject.name = "Pocket Receipts"

/**
 * Includes a module whose build file is named after its last path segment,
 * e.g. `:feature:receipts` -> `feature/receipts/receipts.gradle.kts`.
 */
fun includeModule(path: String) {
    include(path)
    project(path).buildFileName = "${path.substringAfterLast(':')}.gradle.kts"
}

include(":app")
includeModule(":core:test")
includeModule(":core:ui")
includeModule(":core:ui-test")
includeModule(":domain")
includeModule(":feature:receipts")
includeModule(":feature:capture")
includeModule(":feature:ask")
includeModule(":feature:modelsetup")
includeModule(":feature:settings")
includeModule(":data:receipts")
includeModule(":data:ai")
includeModule(":data:model-download")
 