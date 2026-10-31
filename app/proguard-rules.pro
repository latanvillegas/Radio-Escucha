# Add project specific ProGuard rules here.
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes MethodParameters
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Moshi & Kotlin Reflection DTOs
-dontwarn com.squareup.moshi.**
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Media3 / ExoPlayer
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }

# Coil & SVG / GIF Decoders
-dontwarn coil.**
-dontwarn com.caverock.androidsvg.**
-keep class com.caverock.androidsvg.** { *; }
-keep class coil.util.** { *; }

