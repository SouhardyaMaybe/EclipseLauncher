plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}

android {
    namespace = "com.eclipselauncher"
    compileSdk = rootProject.extra["compileSdk"].toString().toInt()

    defaultConfig {
        applicationId = "com.eclipselauncher"
        minSdk = rootProject.extra["minSdk"].toString().toInt()
        targetSdk = rootProject.extra["targetSdk"].toString().toInt()
        versionCode = rootProject.extra["versionCode"].toString().toInt()
        versionName = rootProject.extra["versionName"].toString()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xopt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-Xopt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-Xopt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-Xopt-in=androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi",
            "-Xopt-in=androidx.compose.material3.adaptive.navigation.suite.ExperimentalMaterial3AdaptiveNavigationSuiteApi"
        )
    }

    buildFeatures {
        compose = true
        viewBinding = false
        dataBinding = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = rootProject.extra["composeBomVersion"].toString()
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE,LICENSE.txt,NOTICE,NOTICE.txt}"
        }
    }
}

dependencies {
    // Core Android
    val activityVersion = "1.9.0"
    val coreKtxVersion = "1.13.1"
    val lifecycleVersion = "2.8.2"

    implementation("androidx.core:core-ktx:$coreKtxVersion")
    implementation("androidx.activity:activity-compose:$activityVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:${rootProject.ext.get("composeBomVersion")}")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Material 3
    implementation("androidx.compose.material3:material3:${rootProject.extra["material3Version"]}")
    implementation("androidx.compose.material3:material3-window-size-class:${rootProject.extra["material3Version"]}")

    // Material 3 Adaptive
    implementation("androidx.compose.material3:material3-adaptive:${rootProject.extra["material3AdaptiveVersion"]}")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:${rootProject.extra["material3AdaptiveNavigationSuiteVersion"]}")

    // Compose Foundation & UI
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-util")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-graphics")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.runtime:runtime-rxjava3")

    // Window Insets & System Bars
    implementation("androidx.window:window:1.3.0")
    implementation("androidx.compose.material3:material3-window-size-class")

    // Coroutines & Flow
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${rootProject.extra["coroutinesVersion"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${rootProject.extra["coroutinesVersion"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${rootProject.extra["coroutinesVersion"]}")

    // Flow (included in kotlinx-coroutines-core)

    // Coil Image Loading
    implementation("io.coil-kt:coil-compose:${rootProject.extra["coilVersion"]}")
    implementation("io.coil-kt:coil-core:${rootProject.extra["coilVersion"]}")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.2")

    // Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended")

    // Serialization (for JSON/config)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${rootProject.extra["coroutinesVersion"]}")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-tooling-data")
}