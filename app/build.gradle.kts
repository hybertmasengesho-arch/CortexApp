plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cortex.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cortex.app"
        minSdk = 24          // Quick Settings tiles need API 24+
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // Reads signing credentials from environment variables — NEVER
    // hardcoded here, since this file is committed to git. The GitHub
    // Actions workflow (.github/workflows/build-apk.yml) sets these from
    // repo secrets before building. For a local build in Android Studio,
    // set them as environment variables on your own machine, or the
    // release build simply falls back to being unsigned (installable for
    // testing, but not swappable with a properly-signed version later —
    // see README.md's "Release builds" section).
    val ksPath = System.getenv("CORTEX_KEYSTORE_PATH")
    val ksPass = System.getenv("CORTEX_KEYSTORE_PASSWORD")
    val keyAliasEnv = System.getenv("CORTEX_KEY_ALIAS")
    val keyPass = System.getenv("CORTEX_KEY_PASSWORD")
    val hasSigningConfig = !ksPath.isNullOrBlank() && file(ksPath).exists() &&
        !ksPass.isNullOrBlank() && !keyAliasEnv.isNullOrBlank() && !keyPass.isNullOrBlank()

    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                storeFile = file(ksPath!!)
                storePassword = ksPass
                keyAlias = keyAliasEnv
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            // else: unsigned release build — Android will still let you
            // install it for testing, just won't treat it as an "update"
            // to a previously signed version.
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity-ktx:1.8.2")   // registerForActivityResult, used by the file-chooser fix in MainActivity.kt
}
