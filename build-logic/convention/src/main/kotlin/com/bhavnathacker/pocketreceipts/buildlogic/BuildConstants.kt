package com.bhavnathacker.pocketreceipts.buildlogic

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * SDK levels and JVM target shared by every module.
 */
internal object BuildConstants {
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 37
    const val MIN_SDK = 26
    val JAVA_VERSION = JavaVersion.VERSION_11
    val JVM_TARGET = JvmTarget.JVM_11
}
