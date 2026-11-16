package com.bhavnathacker.pocketreceipts.buildlogic

/**
 * Build-enforced privacy rules: network access is confined to
 * [NETWORK_MODULE_PATH], no analytics, advertising or crash-reporting SDK
 * may be part of the app, and no usage-logging upload backend may be registered.
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

    /**
     * Libraries (`group:name`) whose INTERNET permission is accepted only because
     * the app manifest removes INTERNET. ML Kit needs the classes of its usage-log
     * upload backend, so the app keeps it but removes its permission and its
     * backend registration (see [usageLoggingBackendViolations]).
     */
    val INTERNET_REMOVED_BY_APP = setOf(
        "com.google.android.datatransport:transport-backend-cct",
    )

    private const val APP_SOURCE = "project :app"
    private const val INTERNET_PERMISSION = "android.permission.INTERNET"
    private val BACKEND_META_DATA = Regex("""<meta-data\b[^>]*android:name\s*=\s*"backend:([^"]*)"""")
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
     * removal, not a declaration. A library in [INTERNET_REMOVED_BY_APP] is
     * accepted when the app's own manifest removes INTERNET.
     */
    fun internetPermissionViolations(manifestsBySource: Map<String, String>): List<String> {
        val appRemovesInternet = manifestsBySource[APP_SOURCE]?.removesInternetPermission() == true
        return manifestsBySource
            .filterKeys { it != "project $NETWORK_MODULE_PATH" }
            .filterKeys { !(appRemovesInternet && it.groupAndName() in INTERNET_REMOVED_BY_APP) }
            .filterValues { it.declaresInternetPermission() }
            .keys
            .sorted()
            .map { "$it declares $INTERNET_PERMISSION (only $NETWORK_MODULE_PATH may)" }
    }

    /**
     * Returns one message per usage-logging upload backend that the merged
     * manifest of [variantName] registers. Google's data-transport runtime finds
     * backends only through `backend:<factory>` meta-data; without one, its
     * events are dropped on the device.
     */
    fun usageLoggingBackendViolations(variantName: String, mergedManifest: String): List<String> =
        BACKEND_META_DATA.findAll(mergedManifest)
            .map { it.groupValues[1] }
            .map {
                "$variantName merged manifest registers usage-logging backend $it " +
                    "(remove its meta-data in app/src/main/AndroidManifest.xml)"
            }
            .toList()

    private fun String.internetPermissionTags() =
        USES_PERMISSION_TAG.findAll(this)
            .map { it.value }
            .filter { NAME_ATTRIBUTE.find(it)?.groupValues?.get(1) == INTERNET_PERMISSION }

    private fun String.declaresInternetPermission() =
        internetPermissionTags().any { !REMOVE_NODE_ATTRIBUTE.containsMatchIn(it) }

    private fun String.removesInternetPermission() =
        internetPermissionTags().any { REMOVE_NODE_ATTRIBUTE.containsMatchIn(it) }

    private fun String.groupAndName() = substringBeforeLast(':')

    private fun String.group() = substringBefore(':')

    private fun String.name() = substringAfter(':').substringBefore(':')
}
