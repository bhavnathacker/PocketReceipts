plugins {
    id("pocketreceipts.android.library")
    id("pocketreceipts.android.compose")
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    implementation(project(":domain"))

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.truth)
}
