package com.bhavnathacker.pocketreceipts.buildlogic

import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.DeviceTestBuilder
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Android library module. The namespace defaults to one derived from the project path.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("pocketreceipts.privacy")
            pluginManager.apply("org.jetbrains.kotlinx.kover")

            extensions.configure<LibraryExtension> {
                namespace = defaultNamespace
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                configureAndroidCommon(this)
            }
            // A module without instrumented tests gets no androidTest APK, so
            // `./gradlew connectedAndroidTest` doesn't fail on it with "No tests found".
            if (!file("src/androidTest").exists()) {
                extensions.configure<LibraryAndroidComponentsExtension> {
                    beforeVariants { variant ->
                        variant.deviceTests[DeviceTestBuilder.ANDROID_TEST_TYPE]?.enable = false
                    }
                }
            }
            registerModuleDependencyVerification()
        }
    }
}
