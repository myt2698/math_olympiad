plugins {
    id("com.android.application")
}

android {
    namespace = "com.mathplanet.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mathplanet.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        buildConfig = true
    }

    androidResources {
        // MP4 is already compressed. Keeping it uncompressed avoids extra APK
        // build time and lets Android stream/copy the asset efficiently.
        noCompress += "mp4"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
