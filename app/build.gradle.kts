plugins {
    id("pocketreceipts.android.application")
    id("pocketreceipts.android.compose")
    id("pocketreceipts.hilt")
}

android {
    namespace = "com.bhavnathacker.pocketreceipts"

    defaultConfig {
        applicationId = "com.bhavnathacker.pocketreceipts"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:ui"))
    implementation(project(":feature:receipts"))
    implementation(project(":feature:capture"))
    implementation(project(":feature:ask"))
    implementation(project(":feature:modelsetup"))
    implementation(project(":feature:settings"))
    implementation(project(":data:receipts"))
    implementation(project(":data:ai"))
    implementation(project(":data:model-download"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.timber)
    implementation(libs.compose.state.events)
    androidTestImplementation(project(":core:ui-test"))
    androidTestImplementation(libs.truth)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}