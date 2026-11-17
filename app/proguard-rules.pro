# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# Keep CineVault models, Firestore POJOs, and Moshi JsonAdapters
-keep class com.example.data.model.** { *; }
-keep class com.example.data.local.** { *; }
-keep class com.example.player.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    @com.google.firebase.firestore.PropertyName <fields>;
}

# Keep libmpv JNI bridge and callbacks
-keep class dev.jdtech.mpv.** { *; }
-dontwarn dev.jdtech.mpv.**
-keep class is.xyz.mpv.** { *; }
-dontwarn is.xyz.mpv.**

# Keep Kotlinx Serialization routes/models
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.example.**$$serializer { *; }
-keepclassmembers class com.example.** {
    *** Companion;
}

# Keep WorkManager worker constructors
-keep class com.example.data.downloads.MediaDownloadWorker { *; }

# Suppress optional R8 warnings for third-party libraries
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn okhttp3.**
-dontwarn retrofit2.**

