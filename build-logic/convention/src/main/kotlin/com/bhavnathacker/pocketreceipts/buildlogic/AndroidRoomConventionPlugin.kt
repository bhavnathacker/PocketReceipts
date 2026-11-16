package com.bhavnathacker.pocketreceipts.buildlogic

import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Room for an Android library module: the Room Gradle plugin, KSP, the Room
 * runtime and compiler. Schemas are exported to `<module>/schemas` so they
 * can be committed and used to test migrations.
 */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("androidx.room")

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            dependencies.apply {
                add("implementation", libs.library("androidx-room-runtime"))
                add("implementation", libs.library("androidx-room-ktx"))
                add("ksp", libs.library("androidx-room-compiler"))
                add("androidTestImplementation", libs.library("androidx-room-testing"))
            }
        }
    }
}
