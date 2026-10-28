plugins {
    id("pocketreceipts.android.library")
    id("pocketreceipts.android.compose")
}

dependencies {
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.androidx.junit)
    api(libs.androidx.espresso.core)
}
