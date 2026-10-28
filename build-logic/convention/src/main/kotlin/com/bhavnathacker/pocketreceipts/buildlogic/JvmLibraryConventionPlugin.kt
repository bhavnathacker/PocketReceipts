package com.bhavnathacker.pocketreceipts.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure

/**
 * Pure Kotlin/JVM module (e.g. `:domain`, `:core:test`).
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("pocketreceipts.privacy")
            pluginManager.apply("org.jetbrains.kotlinx.kover")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = BuildConstants.JAVA_VERSION
                targetCompatibility = BuildConstants.JAVA_VERSION
            }
            configureKotlinCompilations()
            useJUnitPlatform()
            addUnitTestDependencies()
        }
    }
}
