package com.bhavnathacker.pocketreceipts.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * The `:app` module.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("pocketreceipts.privacy")
            pluginManager.apply("org.jetbrains.kotlinx.kover")

            extensions.configure<ApplicationExtension> {
                defaultConfig.targetSdk = BuildConstants.TARGET_SDK
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                configureAndroidCommon(this)
            }
            registerTrackingSdkVerification()
            registerInternetPermissionVerification()
        }
    }
}
