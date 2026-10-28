package com.bhavnathacker.pocketreceipts.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Hilt with KSP for an Android application or library module.
 */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("com.google.dagger.hilt.android")

            dependencies.apply {
                add("implementation", libs.library("hilt-android"))
                add("ksp", libs.library("hilt-compiler"))
            }
        }
    }
}
