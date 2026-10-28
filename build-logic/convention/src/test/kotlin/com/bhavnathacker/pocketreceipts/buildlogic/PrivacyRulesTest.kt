package com.bhavnathacker.pocketreceipts.buildlogic

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class PrivacyRulesTest {

    @Test
    fun `test that httpClientViolations reports an HTTP client in a non-allowed module`() {
        val violations = PrivacyRules.httpClientViolations(
            projectPath = ":data:receipts",
            componentIds = listOf("com.squareup.okhttp3:okhttp:5.1.0", "androidx.core:core:1.19.0"),
        )

        assertThat(violations).containsExactly(
            ":data:receipts must not depend on HTTP client com.squareup.okhttp3:okhttp:5.1.0 " +
                "(only :data:model-download may use the network)",
        )
    }

    @Test
    fun `test that httpClientViolations matches every denylisted client group`() {
        val clients = listOf(
            "com.squareup.okhttp3:okhttp:5.1.0",
            "com.squareup.retrofit2:retrofit:3.0.0",
            "io.ktor:ktor-client-core:3.2.0",
            "com.android.volley:volley:1.2.1",
            "org.apache.httpcomponents:httpclient:4.5.14",
            "io.grpc:grpc-okhttp:1.73.0",
            "com.github.kittinunf.fuel:fuel:2.3.1",
        )

        val violations = PrivacyRules.httpClientViolations(":app", clients)

        assertThat(violations).hasSize(clients.size)
    }

    @Test
    fun `test that httpClientViolations is empty for the model download module`() {
        val violations = PrivacyRules.httpClientViolations(
            projectPath = ":data:model-download",
            componentIds = listOf("com.squareup.okhttp3:okhttp:5.1.0"),
        )

        assertThat(violations).isEmpty()
    }

    @Test
    fun `test that httpClientViolations ignores groups that only share a prefix`() {
        val violations = PrivacyRules.httpClientViolations(
            projectPath = ":data:receipts",
            componentIds = listOf("com.squareup.okio:okio:3.15.0", "io.ktorx:other:1.0"),
        )

        assertThat(violations).isEmpty()
    }

    @Test
    fun `test that trackingSdkViolations reports analytics and crash reporting SDKs`() {
        val violations = PrivacyRules.trackingSdkViolations(
            projectPath = ":app",
            componentIds = listOf(
                "com.google.firebase:firebase-analytics:23.0.0",
                "com.google.firebase:firebase-crashlytics:20.0.0",
                "com.google.android.gms:play-services-measurement-api:23.0.0",
                "io.sentry:sentry-android:8.0.0",
            ),
        )

        assertThat(violations).containsExactly(
            ":app must not include tracking SDK com.google.firebase:firebase-analytics:23.0.0",
            ":app must not include tracking SDK com.google.firebase:firebase-crashlytics:20.0.0",
            ":app must not include tracking SDK com.google.android.gms:play-services-measurement-api:23.0.0",
            ":app must not include tracking SDK io.sentry:sentry-android:8.0.0",
        )
    }

    @Test
    fun `test that trackingSdkViolations allows non-tracking Google libraries`() {
        val violations = PrivacyRules.trackingSdkViolations(
            projectPath = ":app",
            componentIds = listOf(
                "com.google.firebase:firebase-components:19.0.0",
                "com.google.android.gms:play-services-base:18.7.0",
                "com.google.dagger:hilt-android:2.60.1",
            ),
        )

        assertThat(violations).isEmpty()
    }

    @Test
    fun `test that internetPermissionViolations reports a non-allowed source declaring INTERNET`() {
        val violations = PrivacyRules.internetPermissionViolations(
            mapOf(
                "project :app" to manifest("""<uses-permission android:name="android.permission.INTERNET" />"""),
                "androidx.core:core:1.19.0" to manifest(""),
            ),
        )

        assertThat(violations).containsExactly(
            "project :app declares android.permission.INTERNET (only :data:model-download may)",
        )
    }

    @Test
    fun `test that internetPermissionViolations allows the model download module`() {
        val violations = PrivacyRules.internetPermissionViolations(
            mapOf(
                "project :data:model-download" to
                    manifest("""<uses-permission android:name="android.permission.INTERNET"/>"""),
            ),
        )

        assertThat(violations).isEmpty()
    }

    @Test
    fun `test that internetPermissionViolations detects attributes in any order and sdk-23 tags`() {
        val violations = PrivacyRules.internetPermissionViolations(
            mapOf(
                "lib:a:1" to manifest(
                    """<uses-permission
                        android:maxSdkVersion="99"
                        android:name="android.permission.INTERNET" />""",
                ),
                "lib:b:1" to manifest("""<uses-permission-sdk-23 android:name="android.permission.INTERNET" />"""),
            ),
        )

        assertThat(violations).hasSize(2)
    }

    @Test
    fun `test that internetPermissionViolations ignores removals and other permissions`() {
        val violations = PrivacyRules.internetPermissionViolations(
            mapOf(
                "project :app" to manifest(
                    """<uses-permission android:name="android.permission.INTERNET" tools:node="remove" />""",
                ),
                "lib:a:1" to manifest("""<uses-permission android:name="android.permission.CAMERA" />"""),
            ),
        )

        assertThat(violations).isEmpty()
    }

    private fun manifest(body: String) =
        """<manifest xmlns:android="http://schemas.android.com/apk/res/android">$body</manifest>"""
}
