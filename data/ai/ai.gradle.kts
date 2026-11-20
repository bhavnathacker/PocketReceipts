plugins {
    id("pocketreceipts.android.library")
    id("pocketreceipts.hilt")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.litertlm.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
}
