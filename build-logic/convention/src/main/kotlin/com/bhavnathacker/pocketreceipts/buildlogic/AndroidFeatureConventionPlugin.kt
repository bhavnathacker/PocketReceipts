package com.bhavnathacker.pocketreceipts.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * A `:feature:*` module: an Android library with Compose, Hilt and Navigation 3.
 * Features depend only on `:domain` and `:core:ui`; they never depend on data
 * modules or on other features.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("pocketreceipts.android.library")
            pluginManager.apply("pocketreceipts.android.compose")
            pluginManager.apply("pocketreceipts.hilt")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            dependencies.apply {
                add("implementation", project(":domain"))
                add("implementation", project(":core:ui"))
                add("implementation", libs.library("androidx-compose-material3"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-navigation3"))
                add("implementation", libs.library("androidx-navigation3-runtime"))
                add("implementation", libs.library("kotlinx-serialization-core"))
                add("androidTestImplementation", project(":core:ui-test"))
            }
        }
    }
}
