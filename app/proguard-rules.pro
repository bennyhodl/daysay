# JNI: the native library looks these up by name.
-keep class com.benschroth.daylightmic.engine.WhisperLib { *; }
-keepclasseswithmembernames class * { native <methods>; }

# Components referenced from the manifest are kept by AGP. Keep the enum names that are
# persisted to disk and shown to the user.
-keepclassmembers enum com.benschroth.daylightmic.** { *; }

# Readable stack traces from Play Console.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
