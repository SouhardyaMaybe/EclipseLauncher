# ProGuard rules for EclipseLauncher

# Keep Application class
-keep class com.eclipselauncher.EclipseLauncherApplication {
    *;
}

# Keep MainActivity
-keep class com.eclipselauncher.MainActivity {
    *;
}

# Keep ViewModels
-keep class com.eclipselauncher.**ViewModel {
    *;
}

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.flow.** { *; }

# Keep Coil
-keep class coil.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }

# Keep Material 3
-keep class androidx.compose.material3.** { *; }

# Keep Navigation
-keep class androidx.navigation.** { *; }

# Keep Serialization
-keep class kotlinx.serialization.** { *; }

# Keep Dagger/Hilt (if added later)
# -keep class dagger.** { *; }

# Keep Room (if added later)
# -keep class androidx.room.** { *; }

# Suppress warnings for third-party libraries
-dontwarn kotlinx.coroutines.**
-dontwarn coil.**
-dontwarn androidx.compose.**
-dontwarn androidx.navigation.**
-dontwarn kotlinx.serialization.**

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# Keep annotations
-keepattributes *Annotation*