plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // FIXED: Matched KSP version strictly to Kotlin 2.2.10 to prevent "unexpected jvm signature V"
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
}

android {
    namespace = "com.ak.battleship"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ak.battleship"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Turn on R8 minification and code optimization
            isMinifyEnabled = true

            // Turn on resource shrinking to trim the APK size
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Use the local debug key to sign this optimized build
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        // FIXED: Upgraded compilation requirements to Java 17 to prevent R8 task warning-as-error failures
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        // FIXED: Updated JVM target mapping to match Java 17 toolchains
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Room Database
    // FIXED: Upgraded Room to 2.7.0 to support Kotlin 2.2.10 and KSP2 backend
    val roomVersion = "2.7.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    // FIXED: Swapped kapt compiler hook out for the robust KSP processor hook
    ksp("androidx.room:room-compiler:$roomVersion")

    // Compose BOM 2024.12.01 (Stable)
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("com.airbnb.android:lottie-compose:6.0.0")

    // Check for the latest stable version depending on your target (Android vs Desktop)
    implementation("org.tensorflow:tensorflow-lite:2.14.0")

    implementation("androidx.compose.material:material-icons-extended")

    // Add this to your dependencies block:
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    testImplementation(kotlin("test"))
}