plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.suchock.sarahmiller"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.suchock.sarahmiller"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
    }
}
