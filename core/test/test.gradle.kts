plugins {
    id("pocketreceipts.jvm.library")
}

dependencies {
    api(platform(libs.junit.bom))
    api(libs.junit.jupiter.api)
    api(libs.kotlinx.coroutines.test)
}
