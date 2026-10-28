package com.bhavnathacker.pocketreceipts.buildlogic

/**
 * Build-enforced privacy rules: network access is confined to
 * [NETWORK_MODULE_PATH], and no analytics, advertising or crash-reporting SDK
 * may be part of the app.
 *
 * Component ids are `group:name:version` coordinates as reported by Gradle's
 * dependency resolution.
 */
internal object PrivacyRules {

    const val NETWORK_MODULE_PATH = ":data:model-download"

    /** Maven groups of HTTP client libraries. */
    val HTTP_CLIENT_GROUPS = setOf(
        "com.squareup.okhttp3",
        "com.squareup.retrofit2",
        "io.ktor",
        "com.android.volley",
        "org.apache.httpcomponents",
        "io.grpc",
        "com.github.kittinunf.fuel",
    )

    /**
     * Analytics, advertising and crash-reporting SDKs as `group:namePrefix`
     * (an empty name prefix denies the whole group).
     */
    val TRACKING_SDKS = listOf(
        "com.google.firebase" to "firebase-analytics",
        "com.google.firebase" to "firebase-crashlytics",
        "com.google.firebase" to "firebase-perf",
        "com.google.android.gms" to "play-services-measurement",
        "com.google.android.gms" to "play-services-ads",
        "io.sentry" to "",
        "com.bugsnag" to "",
        "com.appsflyer" to "",
        "com.amplitude" to "",
        "com.mixpanel.android" to "",
        "com.segment.analytics.android" to "",
        "com.segment.analytics.kotlin" to "",
        "com.facebook.android" to "",
        "com.adjust.sdk" to "",
        "io.branch.sdk.android" to "",
    )

    private const val INTERNET_PERMISSION = "android.permission.INTERNET"
    private val USES_PERMISSION_TAG = Regex("""<uses-permission(?:-sdk-23)?\b[^>]*>""")
    private val NAME_ATTRIBUTE = Regex("""android:name\s*=\s*"([^"]*)"""")
    private val REMOVE_NODE_ATTRIBUTE = Regex("""tools:node\s*=\s*"remove"""")

    fun httpClientViolations(projectPath: String, componentIds: Collection<String>): List<String> {
        if (projectPath == NETWORK_MODULE_PATH) return emptyList()
        return componentIds
            .filter { it.group() in HTTP_CLIENT_GROUPS }
            .map {
                "$projectPath must not depend on HTTP client $it " +
                    "(only $NETWORK_MODULE_PATH may use the network)"
            }
    }

    fun trackingSdkViolations(projectPath: String, componentIds: Collection<String>): List<String> =
        componentIds
            .filter { id ->
                TRACKING_SDKS.any { (group, namePrefix) ->
                    id.group() == group && id.name().startsWith(namePrefix)
                }
            }
            .map { "$projectPath must not include tracking SDK $it" }

    /**
     * Returns one message per manifest source in [manifestsBySource] that adds the
     * INTERNET permission. Sources are keyed by Gradle component display name
     * (`project :app`, `group:name:version`); a `tools:node="remove"` entry is a
     * removal, not a declaration.
     */
    fun internetPermissionViolations(manifestsBySource: Map<String, String>): List<String> =
        manifestsBySource
            .filterKeys { it != "project $NETWORK_MODULE_PATH" }
            .filterValues { it.declaresInternetPermission() }
            .keys
            .sorted()
            .map { "$it declares $INTERNET_PERMISSION (only $NETWORK_MODULE_PATH may)" }

    private fun String.declaresInternetPermission() =
        USES_PERMISSION_TAG.findAll(this).any { tag ->
            NAME_ATTRIBUTE.find(tag.value)?.groupValues?.get(1) == INTERNET_PERMISSION &&
                !REMOVE_NODE_ATTRIBUTE.containsMatchIn(tag.value)
        }

    private fun String.group() = substringBefore(':')

    private fun String.name() = substringAfter(':').substringBefore(':')
}
