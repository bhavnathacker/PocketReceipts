plugins {
    id("pocketreceipts.android.library")
    id("pocketreceipts.hilt")
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.timber)
}
