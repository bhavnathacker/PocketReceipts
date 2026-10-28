package com.bhavnathacker.pocketreceipts.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

/**
 * Fails the build when this module declares a project dependency that
 * [ModuleDependencyRules] forbids.
 */
@CacheableTask
abstract class VerifyModuleDependenciesTask : DefaultTask() {

    @get:Input
    abstract val projectPath: Property<String>

    @get:Input
    abstract val dependencyPaths: ListProperty<String>

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val violations = ModuleDependencyRules.violations(projectPath.get(), dependencyPaths.get())
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Module dependency rules violated:\n" + violations.joinToString("\n") { "  - $it" },
            )
        }
        reportFile.get().asFile.writeText("OK\n")
    }
}

private val MAIN_DEPENDENCY_CONFIGURATIONS = setOf("api", "implementation", "compileOnly", "runtimeOnly")

/**
 * Registers `verifyModuleDependencies` and makes `check` depend on it.
 */
internal fun Project.registerModuleDependencyVerification() {
    val modulePath = path
    val declaredProjectDependencies = provider {
        configurations
            .filter { it.name in MAIN_DEPENDENCY_CONFIGURATIONS }
            .flatMap { it.dependencies.withType<ProjectDependency>() }
            .map { it.path }
            .distinct()
            .sorted()
    }
    val verify = tasks.register<VerifyModuleDependenciesTask>("verifyModuleDependencies") {
        group = "verification"
        description = "Checks this module's project dependencies against the module dependency rules."
        projectPath.set(modulePath)
        dependencyPaths.set(declaredProjectDependencies)
        reportFile.set(layout.buildDirectory.file("reports/verifyModuleDependencies.txt"))
    }
    tasks.named("check").configure { dependsOn(verify) }
}
