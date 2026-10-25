# APVM Proguard / R8 Rules

# Room persistence library
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**

# Gson serialization rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory

# APVM Data models (JSON serialized / Room persisted / State models)
-keep class com.example.android_manager.model.** { *; }
-keep class com.example.android_manager.data.history.ScanHistoryEntity { *; }
-keep class com.example.android_manager.data.history.ScanHistoryDao { *; }
-keep class com.example.android_manager.data.settings.AppSettings { *; }
-keep class com.example.android_manager.data.process.DeviceMemoryInfo { *; }
-keep class com.example.android_manager.data.dashboard.DeviceSecurityPostureInfo { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
