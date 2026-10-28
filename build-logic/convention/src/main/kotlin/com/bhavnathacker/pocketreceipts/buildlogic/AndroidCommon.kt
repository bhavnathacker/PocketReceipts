package com.bhavnathacker.pocketreceipts.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project

/**
 * Settings shared by Android application and library modules.
 */
internal fun Project.configureAndroidCommon(extension: CommonExtension) {
    extension.apply {
        compileSdk = BuildConstants.COMPILE_SDK
        defaultConfig.minSdk = BuildConstants.MIN_SDK
        compileOptions.sourceCompatibility = BuildConstants.JAVA_VERSION
        compileOptions.targetCompatibility = BuildConstants.JAVA_VERSION
        testOptions.unitTests.all { it.useJUnitPlatform() }
    }
    configureKotlinCompilations()
    addUnitTestDependencies()
}
