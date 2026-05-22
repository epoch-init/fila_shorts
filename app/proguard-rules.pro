# =========================================================
# FILA SPORTS: PROGUARD / R8 RULES
# =========================================================

# 1. PROTECT DATABASE ENTITIES & DAOs (Room SQLite)
# If VideoEntity variables are renamed, Room cannot map the SQL columns.
-keep class zeki.productions.shorts.data.** { *; }
-keepclassmembers class zeki.productions.shorts.data.** { *; }

# Protect Room's generated implementation classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase$Builder { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# 2. PROTECT VIEWMODELS
# Prevents R8 from stripping or renaming ViewModel classes and their variables
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
    *;
}

# 3. PROTECT JSON MODELS & LOGIC (Optional but recommended)
# Since you interact with external .json files, we want to ensure your Crypto and Loader logic isn't broken.
-keep class zeki.productions.shorts.logic.** { *; }

# 4. EXOPLAYER / MEDIA3 COMPATIBILITY
# Media3 usually bundles its own rules, but this guarantees the hardware decoding pipeline isn't severed.
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-keep class * extends androidx.media3.exoplayer.ExoPlayer { *; }

# 5. JETPACK COMPOSE ANIMATIONS & REFLECTION
# Compose uses reflection under the hood for some animations and states.
-keep class androidx.compose.animation.** { *; }
-keep class androidx.compose.runtime.snapshots.** { *; }

# 6. COROUTINES
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# 7. SUPPRESS WARNINGS FOR UNRESOLVED EXTERNAL LIBRARIES
# (Prevents the build from failing if a library references a class that isn't included in Android)
-dontwarn androidx.**
-dontwarn kotlinx.**
-dontwarn org.json.**

# =========================================================
# 8. JETPACK COMPOSE R8 FULL-MODE CRASH FIXES
# =========================================================
# Protect the entire Semantics package
-keep class androidx.compose.ui.semantics.** { *; }
-keepclassmembers class androidx.compose.ui.semantics.** { *; }

# ULTIMATE FIX: Force R8 Full Mode to keep all Kotlin synthetic $default
# methods across the entire Compose UI package.
-keepclassmembers class androidx.compose.ui.** {
    *** *$default(...);
}

# Explicitly protect the exact file-class and method causing the crash
-keep class androidx.compose.ui.semantics.SemanticsPropertiesKt {
    public static void performImeAction$default(...);
    public static void perform*(...);
}

# Protect Compose Focus, Text, and Inputs
-keep class androidx.compose.ui.focus.** { *; }
-keep class androidx.compose.ui.text.** { *; }
-keep class androidx.compose.foundation.text.** { *; }