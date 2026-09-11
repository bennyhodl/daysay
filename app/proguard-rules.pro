# JNI: the native library looks these up by name.
-keep class dev.bennyb.daysay.engine.WhisperLib { *; }
-keepclasseswithmembernames class * { native <methods>; }

# Components referenced from the manifest are kept by AGP. Keep the enum names that are
# persisted to disk and shown to the user.
-keepclassmembers enum dev.bennyb.daysay.** { *; }

# Readable stack traces from Play Console.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
