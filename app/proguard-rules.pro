-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault,EnclosingMethod

# JGit Rules
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**

# Bouncy Castle Provider Rules
# Provider algorithms are registered through reflection and must survive R8 shrinking.
# Keep the provider/algorithm registry, not the entire BC library, to avoid bloating R8.
-keep class org.bouncycastle.jce.provider.BouncyCastleProvider { *; }
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }
-keep class org.bouncycastle.jcajce.interfaces.** { *; }
-dontwarn org.bouncycastle.**
# NTRU parameter tables are eagerly initialized by the provider. Keep only the
# affected PQC packages so R8 cannot null or optimize their static parameters,
# without keeping the entire Bouncy Castle crypto implementation in memory.
-keep class org.bouncycastle.pqc.crypto.ntru.** { *; }
-keep class org.bouncycastle.pqc.math.ntru.** { *; }

# Apache Mina SSHD / JGit SSH Rules
# SSHD discovers these classes by reflection. In particular, R8 previously renamed
# NoFlowControl to wu2, then Mina failed to call its no-argument constructor.
-keep class org.apache.sshd.common.kex.extension.** { *; }
-keepnames class org.apache.sshd.common.kex.extension.**
-keep class org.apache.sshd.common.io.** { *; }
-keepnames class org.apache.sshd.common.io.**
-keep class org.apache.sshd.common.config.keys.** { *; }
-keep class org.apache.sshd.common.util.security.** { *; }
-keep class org.apache.sshd.client.** { *; }
-keep class org.apache.sshd.sftp.** { *; }
-keep class org.eclipse.jgit.transport.sshd.** { *; }
-keepnames class org.eclipse.jgit.transport.sshd.**
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
