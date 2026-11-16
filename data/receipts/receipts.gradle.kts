plugins {
    id("pocketreceipts.android.library")
    id("pocketreceipts.hilt")
    id("pocketreceipts.android.room")
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.timber)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
}
