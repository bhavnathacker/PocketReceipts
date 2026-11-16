plugins {
    id("pocketreceipts.android.feature")
}

dependencies {
    implementation(libs.mlkit.document.scanner)
    implementation(libs.androidx.activity.compose)

    androidTestImplementation(libs.truth)
}
