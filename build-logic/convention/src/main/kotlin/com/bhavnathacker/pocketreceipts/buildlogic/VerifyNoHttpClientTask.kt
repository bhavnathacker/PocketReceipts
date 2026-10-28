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
 * Fails the build when an HTTP client is on this module's compile classpath,
 * unless the module is [PrivacyRules.NETWORK_MODULE_PATH]. The compile classpath
 * (not the runtime one) is checked: `:app` legitimately gets the client at runtime
 * through the network module's `implementation` dependency, while a direct
 * dependency or an `api` leak puts it on the compile classpath.
 */
@CacheableTask
abstract class VerifyNoHttpClientTask : DefaultTask() {

    @get:Input
    abstract val projectPath: Property<String>

    @get:Input
    abstract val componentIds: ListProperty<String>

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val violations = PrivacyRules.httpClientViolations(projectPath.get(), componentIds.get().distinct())
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Privacy check failed:\n" + violations.joinToString("\n") { "  - $it" },
            )
        }
        reportFile.get().asFile.writeText("OK\n")
    }
}

private val COMPILE_CLASSPATHS = setOf("compileClasspath", "debugCompileClasspath", "releaseCompileClasspath")

/**
 * Registers `verifyNoHttpClient` and makes `check` depend on it.
 */
internal fun Project.registerHttpClientVerification() {
    val modulePath = path
    val verify = tasks.register<VerifyNoHttpClientTask>("verifyNoHttpClient") {
        group = "verification"
        description = "Checks that no HTTP client is on this module's compile classpath."
        projectPath.set(modulePath)
        reportFile.set(layout.buildDirectory.file("reports/privacy/verifyNoHttpClient.txt"))
    }
    configurations.matching { it.name in COMPILE_CLASSPATHS }.configureEach {
        val ids = incoming.resolutionResult.rootComponent.map { it.moduleComponentIds() }
        verify.configure { componentIds.addAll(ids) }
    }
    tasks.matching { it.name == "check" }.configureEach { dependsOn(verify) }
}
