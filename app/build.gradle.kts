@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)
    id("com.google.devtools.ksp") version "2.3.6"
}

kotlin {
    androidTarget()
    
    wasmJs {
        // compilerOptions DSL is used for moduleName if needed, but not strictly required
        browser {
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "battleship.js"
                devServer = (devServer ?: org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
                implementation("androidx.room:room-common:2.7.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                val roomVersion = "2.7.0"
                implementation("androidx.room:room-runtime:$roomVersion")
                implementation("androidx.room:room-ktx:$roomVersion")
                implementation("org.tensorflow:tensorflow-lite:2.14.0")
                implementation("androidx.datastore:datastore-preferences:1.0.0")
                implementation("com.airbnb.android:lottie-compose:6.0.0")
            }
        }
        val wasmJsMain by getting {
            dependencies {
            }
        }
    }
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val roomVersion = "2.7.0"
    add("kspAndroid", "androidx.room:room-compiler:$roomVersion")
}
