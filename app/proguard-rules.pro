-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault,EnclosingMethod

# JGit Rules
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**

# Apache Mina SSHD / JGit SSH Rules
# SSHD contains optional Java SE integrations that are not shipped by Android.
# Keep the SSH implementation because JGit loads parts of it through factories/reflection.
-keep class org.apache.sshd.** { *; }
-keep class org.eclipse.jgit.transport.sshd.** { *; }
-dontwarn org.apache.sshd.**
-dontwarn java.rmi.**
-dontwarn javax.management.**
-dontwarn javax.security.auth.login.**

# SLF4J Rules
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

# Tink/Security Crypto Rules
-dontwarn com.google.errorprone.annotations.**

# Keep Enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Fix: Correctly keep Kotlin data class component functions for destructuring
-keepclassmembers class * {
    *** component*();
}
