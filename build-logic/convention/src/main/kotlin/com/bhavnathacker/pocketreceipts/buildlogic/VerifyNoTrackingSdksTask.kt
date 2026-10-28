package com.bhavnathacker.pocketreceipts.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register

/**
 * Fails the build when an analytics, advertising or crash-reporting SDK is on
 * the app's runtime classpath. Applied to `:app`, whose runtime classpath
 * includes every module.
 */
@CacheableTask
abstract class VerifyNoTrackingSdksTask : DefaultTask() {

    @get:Input
    abstract val projectPath: Property<String>

    @get:Input
    abstract val componentIds: ListProperty<String>

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val violations = PrivacyRules.trackingSdkViolations(projectPath.get(), componentIds.get().distinct())
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Privacy check failed:\n" + violations.joinToString("\n") { "  - $it" },
            )
        }
        reportFile.get().asFile.writeText("OK\n")
    }
}

private val RUNTIME_CLASSPATHS = setOf("debugRuntimeClasspath", "releaseRuntimeClasspath")

/**
 * Registers `verifyNoTrackingSdks` and makes `check` depend on it.
 */
internal fun Project.registerTrackingSdkVerification() {
    val modulePath = path
    val verify = tasks.register<VerifyNoTrackingSdksTask>("verifyNoTrackingSdks") {
        group = "verification"
        description = "Checks that no analytics, advertising or crash-reporting SDK is in the app."
        projectPath.set(modulePath)
        reportFile.set(layout.buildDirectory.file("reports/privacy/verifyNoTrackingSdks.txt"))
    }
    configurations.matching { it.name in RUNTIME_CLASSPATHS }.configureEach {
        val ids = incoming.resolutionResult.rootComponent.map { it.moduleComponentIds() }
        verify.configure { componentIds.addAll(ids) }
    }
    tasks.matching { it.name == "check" }.configureEach { dependsOn(verify) }
}
