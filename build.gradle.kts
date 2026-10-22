plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.daymark.studentplanner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.daymark.studentplanner"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
}
