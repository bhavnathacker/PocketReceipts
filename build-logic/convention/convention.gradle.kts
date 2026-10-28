import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.bhavnathacker.pocketreceipts.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("privacy") {
            id = "pocketreceipts.privacy"
            implementationClass = "com.bhavnathacker.pocketreceipts.buildlogic.PrivacyConventionPlugin"
        }
        register("jvmLibrary") {
            id = "pocketreceipts.jvm.library"
            implementationClass = "com.bhavnathacker.pocketreceipts.buildlogic.JvmLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "pocketreceipts.android.application"
            implementationClass = "com.bhavnathacker.pocketreceipts.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "pocketreceipts.android.library"
            implementationClass = "com.bhavnathacker.pocketreceipts.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "pocketreceipts.android.compose"
            implementationClass = "com.bhavnathacker.pocketreceipts.buildlogic.AndroidComposeConventionPlugin"
        }
        register("hilt") {
            id = "pocketreceipts.hilt"
            implementationClass = "com.bhavnathacker.pocketreceipts.buildlogic.HiltConventionPlugin"
        }
        register("androidFeature") {
            id = "pocketreceipts.android.feature"
            implementationClass = "com.bhavnathacker.pocketreceipts.buildlogic.AndroidFeatureConventionPlugin"
        }
    }
}
