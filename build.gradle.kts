// Top-level build file. Plugins are declared here (not applied) so convention
// plugins in build-logic can apply them by id from a shared classloader.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kover)
}

// One aggregated coverage report for all modules: ./gradlew koverXmlReport
dependencies {
    subprojects
        .filter { it.buildFile.exists() }
        .forEach { kover(project(it.path)) }
}

kover {
    reports {
        filters {
            excludes {
                // Generated code: Hilt, Dagger factories, Compose previews.
                packages("hilt_aggregated_deps", "dagger.hilt.internal.aggregatedroot.codegen")
                classes("*_Hilt*", "Hilt_*", "*_Factory", "*_MembersInjector", "*_GeneratedInjector")
                annotatedBy(
                    "androidx.compose.ui.tooling.preview.Preview",
                    "androidx.compose.ui.tooling.preview.PreviewLightDark",
                )
            }
        }
    }
}
