package com.bhavnathacker.pocketreceipts.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register

/**
 * Fails the build when any manifest other than the network module's adds the
 * INTERNET permission: the app's own manifest, or the manifest of any project or
 * library on the app's runtime classpath.
 */
@CacheableTask
abstract class VerifyInternetPermissionSourcesTask : DefaultTask() {

    @get:Input
    abstract val projectPath: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val appManifest: RegularFileProperty

    /** Dependency manifests, tracked for up-to-date checks. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val dependencyManifestFiles: ConfigurableFileCollection

    /** The same manifests paired with the component that contributed them. */
    @get:Internal
    abstract val dependencyManifests: SetProperty<ResolvedArtifactResult>

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val manifestsBySource = buildMap {
            put("project ${projectPath.get()}", appManifest.get().asFile.readText())
            dependencyManifests.get().forEach { artifact ->
                put(artifact.id.componentIdentifier.displayName, artifact.file.readText())
            }
        }
        val violations = PrivacyRules.internetPermissionViolations(manifestsBySource)
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Privacy check failed:\n" + violations.joinToString("\n") { "  - $it" },
            )
        }
        reportFile.get().asFile.writeText(manifestsBySource.keys.sorted().joinToString("\n", postfix = "\n"))
    }
}

private val RUNTIME_CLASSPATHS = setOf("debugRuntimeClasspath", "releaseRuntimeClasspath")
private val ARTIFACT_TYPE = Attribute.of("artifactType", String::class.java)

/**
 * Registers `verifyInternetPermissionSources` and makes `check` depend on it.
 */
internal fun Project.registerInternetPermissionVerification() {
    val modulePath = path
    val verify = tasks.register<VerifyInternetPermissionSourcesTask>("verifyInternetPermissionSources") {
        group = "verification"
        description = "Checks that only the network module adds the INTERNET permission."
        projectPath.set(modulePath)
        appManifest.set(layout.projectDirectory.file("src/main/AndroidManifest.xml"))
        reportFile.set(layout.buildDirectory.file("reports/privacy/verifyInternetPermissionSources.txt"))
    }
    configurations.matching { it.name in RUNTIME_CLASSPATHS }.configureEach {
        // AGP exposes every Android dependency's manifest under this artifact type;
        // lenient so plain JARs, which have no manifest, are skipped.
        val manifests = incoming.artifactView {
            lenient(true)
            attributes { attribute(ARTIFACT_TYPE, "android-manifest") }
        }.artifacts
        verify.configure {
            dependencyManifestFiles.from(manifests.artifactFiles)
            dependencyManifests.addAll(manifests.resolvedArtifacts)
        }
    }
    tasks.matching { it.name == "check" }.configureEach { dependsOn(verify) }
}
