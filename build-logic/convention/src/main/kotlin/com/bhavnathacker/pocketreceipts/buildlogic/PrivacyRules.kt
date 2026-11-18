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
     * Upload-backend libraries (`group:name`) whose INTERNET permission is accepted
     * only while the app manifest removes their backend registration (the value:
     * the `backend:` factory class). ML Kit needs the classes of its usage-log
     * upload backend, so the app keeps the library; with its registration removed,
     * it has no way to send anything (see [usageLoggingBackendViolations]).
     */
    val INTERNET_ACCEPTED_WITHOUT_BACKEND = mapOf(
        "com.google.android.datatransport:transport-backend-cct" to
            "com.google.android.datatransport.cct.CctBackendFactory",
    )

    private const val APP_SOURCE = "project :app"
    private const val INTERNET_PERMISSION = "android.permission.INTERNET"
    private val BACKEND_META_DATA = Regex("""<meta-data\b[^>]*android:name\s*=\s*"backend:([^"]*)"""")
    private val META_DATA_TAG = Regex("""<meta-data\b[^>]*>""")
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
     * removal, not a declaration. A library in [INTERNET_ACCEPTED_WITHOUT_BACKEND]
     * is accepted when the app's own manifest removes its backend registration.
     */
    fun internetPermissionViolations(manifestsBySource: Map<String, String>): List<String> {
        val appManifest = manifestsBySource[APP_SOURCE].orEmpty()
        return manifestsBySource
            .filterKeys { it != "project $NETWORK_MODULE_PATH" }
            .filterKeys { source ->
                val backend = INTERNET_ACCEPTED_WITHOUT_BACKEND[source.groupAndName()]
                backend == null || !appManifest.removesBackend(backend)
            }
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

    private fun String.removesBackend(factory: String) =
        META_DATA_TAG.findAll(this)
            .map { it.value }
            .any { tag ->
                NAME_ATTRIBUTE.find(tag)?.groupValues?.get(1) == "backend:$factory" &&
                    REMOVE_NODE_ATTRIBUTE.containsMatchIn(tag)
            }

    private fun String.groupAndName() = substringBeforeLast(':')

    private fun String.group() = substringBefore(':')

    private fun String.name() = substringAfter(':').substringBefore(':')
}
