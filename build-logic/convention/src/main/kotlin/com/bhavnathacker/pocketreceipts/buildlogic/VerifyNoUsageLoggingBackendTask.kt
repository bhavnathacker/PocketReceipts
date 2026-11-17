package com.bhavnathacker.pocketreceipts.buildlogic

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

/**
 * Fails the build when a variant's merged manifest registers a usage-logging
 * upload backend (for example the one ML Kit pulls in), so usage logs stay on
 * the device.
 */
@CacheableTask
abstract class VerifyNoUsageLoggingBackendTask : DefaultTask() {

    @get:Input
    abstract val variantName: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val violations = PrivacyRules.usageLoggingBackendViolations(
            variantName.get(),
            mergedManifest.get().asFile.readText(),
        )
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Privacy check failed:\n" + violations.joinToString("\n") { "  - $it" },
            )
        }
        reportFile.get().asFile.writeText("OK\n")
    }
}

/**
 * Registers `verifyNoUsageLoggingBackend<Variant>` for every application variant,
 * plus an aggregate `verifyNoUsageLoggingBackend` that `check` depends on.
 */
internal fun Project.registerUsageLoggingBackendVerification() {
    val aggregate = tasks.register("verifyNoUsageLoggingBackend") {
        group = "verification"
        description = "Checks that no variant registers a usage-logging upload backend."
    }
    extensions.getByType<ApplicationAndroidComponentsExtension>().onVariants { variant ->
        val name = variant.name
        val verify = tasks.register<VerifyNoUsageLoggingBackendTask>(
            "verifyNoUsageLoggingBackend${name.replaceFirstChar { it.uppercase() }}",
        ) {
            group = "verification"
            description = "Checks that the $name merged manifest registers no usage-logging backend."
            variantName.set(name)
            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            reportFile.set(layout.buildDirectory.file("reports/privacy/verifyNoUsageLoggingBackend-$name.txt"))
        }
        aggregate.configure { dependsOn(verify) }
    }
    tasks.matching { it.name == "check" }.configureEach { dependsOn(aggregate) }
}
