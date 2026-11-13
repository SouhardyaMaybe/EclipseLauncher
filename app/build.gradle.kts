plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}

android {
    namespace = "com.eclipselauncher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.eclipselauncher"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

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
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE,LICENSE.txt,NOTICE,NOTICE.txt}"
        }
    }
}

// Resolution strategy to force Compose 1.6.1 and prevent transitive deps from pulling newer versions
configurations.all {
    resolutionStrategy {
        // Force all Compose libraries to 1.6.1
        force("androidx.compose:compose-compiler:1.6.1")
        force("androidx.compose.foundation:foundation:1.6.1")
        force("androidx.compose.foundation:foundation-layout:1.6.1")
        force("androidx.compose.foundation:foundation-text:1.6.1")
        force("androidx.compose.ui:ui:1.6.1")
        force("androidx.compose.ui:ui-graphics:1.6.1")
        force("androidx.compose.ui:ui-text:1.6.1")
        force("androidx.compose.ui:ui-unit:1.6.1")
        force("androidx.compose.ui:ui-util:1.6.1")
        force("androidx.compose.animation:animation:1.6.1")
        force("androidx.compose.animation:animation-core:1.6.1")
        force("androidx.compose.animation:animation-graphics:1.6.1")
        force("androidx.compose.runtime:runtime:1.6.1")
        force("androidx.compose.runtime:runtime-livedata:1.6.1")
        force("androidx.compose.runtime:runtime-rxjava3:1.6.1")
        force("androidx.compose.runtime:runtime-saveable:1.6.1")
        force("androidx.compose.material:material:1.6.1")
        force("androidx.compose.material:material-icons-core:1.6.1")
        force("androidx.compose.material:material-icons-extended:1.6.1")
        force("androidx.compose.material:material-ripple:1.6.1")
        force("androidx.compose.ui:ui-geometry:1.6.1")
        force("androidx.compose.ui:ui-test:1.6.1")
        force("androidx.compose.ui:ui-test-junit4:1.6.1")
        force("androidx.compose.ui:ui-test-manifest:1.6.1")
        force("androidx.compose.ui:ui-tooling:1.6.1")
        force("androidx.compose.ui:ui-tooling-data:1.6.1")
        force("androidx.compose.ui:ui-tooling-preview:1.6.1")
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

    // Compose 1.6.1 (compatible with AGP 8.9, compileSdk 34, Kotlin 2.0)
    val composeVersion = "1.6.1"

    // Material 3
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material3:material3-window-size-class:1.2.1")

    // Material 3 Adaptive (use older alpha compatible with 1.6.x)
    implementation("androidx.compose.material3:material3-adaptive:1.0.0-alpha01")
    // Note: material3-adaptive-navigation-suite 1.5.0-rc01 requires AGP 9.1+, disabled for now

    // Compose Foundation & UI
    implementation("androidx.compose.foundation:foundation:$composeVersion")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-graphics:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.ui:ui-util:$composeVersion")
    implementation("androidx.compose.animation:animation:$composeVersion")
    implementation("androidx.compose.animation:animation-graphics:$composeVersion")
    implementation("androidx.compose.runtime:runtime:$composeVersion")
    implementation("androidx.compose.runtime:runtime-livedata:$composeVersion")
    implementation("androidx.compose.runtime:runtime-rxjava3:$composeVersion")

    // Window Insets & System Bars
    implementation("androidx.window:window:1.3.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.2.1")

    // Coroutines & Flow
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // Coil Image Loading - using version compatible with Compose 1.6.1
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-core:2.5.0")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.2")

    // Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended")

    // Serialization (for JSON/config)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest:1.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling-data:1.6.1")
}