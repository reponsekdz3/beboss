# Preserve line numbers and source file attributes for actionable crash stack traces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Room Database Preservation
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**

# Data Models and Moshi JSON Serialization
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
}
-keep class com.example.data.model.** { *; }
-keep class com.example.data.db.** { *; }
-keep class com.squareup.moshi.** { *; }

# Android Keystore and Cryptographic Providers
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }
-keep class com.example.util.SubscriptionSecurityManager { *; }
-keep class com.example.util.SecurityUtils { *; }
-keep class com.example.util.BiometricAuthManager { *; }

# Jetpack Compose and Lifecycle
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class androidx.lifecycle.** { *; }

# Coroutines and Flow
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Strip verbose/debug logs in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

