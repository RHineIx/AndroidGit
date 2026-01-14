-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault,EnclosingMethod

# JGit Rules
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**

# SLF4J Rules
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

# Keep Enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Fix: Correctly keep Kotlin data class component functions for destructuring
-keepclassmembers class * {
    *** component*();
}