package com.bhavnathacker.pocketreceipts.buildlogic

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipOutputStream

/**
 * Runs `verifyNoHttpClient` on small JVM fixture builds. HTTP clients come from a
 * local file repository so the tests need no network.
 */
class VerifyNoHttpClientFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @BeforeEach
    fun setUp() {
        publishFakeArtifact(group = "com.squareup.okhttp3", name = "okhttp", version = "9.9.9")
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            dependencyResolutionManagement {
                repositories { maven { url = uri("${projectDir.resolve("repo").toURI()}") } }
            }
            include(":data:receipts", ":data:model-download")
            """.trimIndent(),
        )
    }

    @Test
    fun `test that verifyNoHttpClient fails when a non-allowed module depends on an HTTP client`() {
        module(":data:receipts", dependencies = """implementation("com.squareup.okhttp3:okhttp:9.9.9")""")
        module(":data:model-download")

        val result = runner(":data:receipts:verifyNoHttpClient").buildAndFail()

        assertThat(result.output).contains(
            ":data:receipts must not depend on HTTP client com.squareup.okhttp3:okhttp:9.9.9",
        )
    }

    @Test
    fun `test that verifyNoHttpClient passes when the model download module depends on an HTTP client`() {
        module(":data:receipts")
        module(":data:model-download", dependencies = """implementation("com.squareup.okhttp3:okhttp:9.9.9")""")

        val result = runner(":data:model-download:verifyNoHttpClient").build()

        assertThat(result.task(":data:model-download:verifyNoHttpClient")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `test that verifyNoHttpClient passes when the client stays an implementation detail of the allowed module`() {
        module(":data:receipts", dependencies = """implementation(project(":data:model-download"))""")
        module(":data:model-download", dependencies = """implementation("com.squareup.okhttp3:okhttp:9.9.9")""")

        val result = runner(":data:receipts:verifyNoHttpClient").build()

        assertThat(result.task(":data:receipts:verifyNoHttpClient")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `test that verifyNoHttpClient fails and names the consumer when the allowed module leaks the client as api`() {
        module(":data:receipts", dependencies = """implementation(project(":data:model-download"))""")
        module(":data:model-download", dependencies = """api("com.squareup.okhttp3:okhttp:9.9.9")""")

        val result = runner(":data:receipts:verifyNoHttpClient").buildAndFail()

        assertThat(result.output).contains(
            ":data:receipts must not depend on HTTP client com.squareup.okhttp3:okhttp:9.9.9",
        )
    }

    private fun module(path: String, dependencies: String = "") {
        val dir = projectDir.resolve(path.removePrefix(":").replace(':', '/'))
        dir.mkdirs()
        dir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                `java-library`
                id("pocketreceipts.privacy")
            }
            dependencies {
                $dependencies
            }
            """.trimIndent(),
        )
    }

    private fun runner(task: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(task, "--configuration-cache", "--stacktrace")

    private fun publishFakeArtifact(group: String, name: String, version: String) {
        val dir = projectDir.resolve("repo/${group.replace('.', '/')}/$name/$version")
        dir.mkdirs()
        dir.resolve("$name-$version.pom").writeText(
            """
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>
              <groupId>$group</groupId>
              <artifactId>$name</artifactId>
              <version>$version</version>
            </project>
            """.trimIndent(),
        )
        ZipOutputStream(dir.resolve("$name-$version.jar").outputStream()).close()
    }
}
