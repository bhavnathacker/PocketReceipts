package com.bhavnathacker.pocketreceipts.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * The root `libs` version catalog.
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.library(alias: String) = findLibrary(alias).get()

/**
 * A Kotlin module name unique across the build, derived from the project path
 * (e.g. `:feature:receipts` -> `feature-receipts`), so modules sharing a last
 * path segment don't produce clashing `.kotlin_module` files in `META-INF`.
 */
internal val Project.kotlinModuleName: String
    get() = path.removePrefix(":").replace(':', '-')

/**
 * Sets the JVM target and a path-based module name on every Kotlin compile task.
 * The suffix keeps one name per compilation, matching Kotlin's defaults:
 * `compileKotlin` -> `domain`, `compileDebugUnitTestKotlin` -> `feature-receipts_debugUnitTest`.
 */
internal fun Project.configureKotlinCompilations() {
    val moduleName = kotlinModuleName
    tasks.withType<KotlinCompile>().configureEach {
        val compilation = name.removePrefix("compile").removeSuffix("Kotlin")
            .replaceFirstChar { it.lowercase() }
        compilerOptions {
            jvmTarget.set(BuildConstants.JVM_TARGET)
            this.moduleName.set(if (compilation.isEmpty()) moduleName else "${moduleName}_$compilation")
        }
    }
}

/**
 * Android namespace derived from the project path, e.g.
 * `:data:model-download` -> `com.bhavnathacker.pocketreceipts.data.modeldownload`.
 */
internal val Project.defaultNamespace: String
    get() = "com.bhavnathacker.pocketreceipts." +
        path.removePrefix(":").split(':').joinToString(".") { it.replace("-", "") }

internal fun Project.useJUnitPlatform() {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

/**
 * Adds the JVM unit-test stack (JUnit 5, Mockito, Truth, Turbine and the shared
 * `:core:test` helpers) to the `testImplementation` configuration.
 */
internal fun Project.addUnitTestDependencies() {
    dependencies.apply {
        add("testImplementation", dependencies.platform(libs.library("junit-bom")))
        add("testImplementation", libs.library("junit-jupiter"))
        add("testRuntimeOnly", libs.library("junit-platform-launcher"))
        if (path != ":core:test") {
            add("testImplementation", project(":core:test"))
            add("testImplementation", libs.library("mockito-kotlin"))
            add("testImplementation", libs.library("truth"))
            add("testImplementation", libs.library("turbine"))
        }
    }
}
