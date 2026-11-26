plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// A release tag is the source of truth; local builds retain a safe default.
val localTag = providers.gradleProperty("androidgitLocalTag").orElse("v7.0.1-stable")
val releaseTag = providers.gradleProperty("releaseTag")
    .orElse(providers.environmentVariable("GITHUB_REF_TYPE").flatMap { type ->
        if (type == "tag") providers.environmentVariable("GITHUB_REF_NAME") else localTag
    })
    .orElse(localTag)
    .get()
val versionMatch = requireNotNull(
    Regex("""v(0|[1-9][0-9]{0,5})\.(0|[1-9][0-9]?)\.(0|[1-9][0-9]?)(?:-[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?""").matchEntire(releaseTag)
) { "Invalid release tag: $releaseTag (expected vMAJOR.MINOR.PATCH with optional suffix; minor/patch 0..99)" }
val (majorText, minorText, patchText) = versionMatch.destructured
val computedVersionCode = majorText.toLong() * 10000L + minorText.toLong() * 100L + patchText.toLong()
require(computedVersionCode in 1L..2100000000L) { "Invalid Android versionCode: $computedVersionCode" }
val computedVersionName = releaseTag.removePrefix("v")

android {
    namespace = "com.android.git"
    compileSdk = 37
    compileSdkMinor = 0

    defaultConfig {
        applicationId = "com.android.git"
        minSdk = 26
        targetSdk = 37
        versionCode = computedVersionCode.toInt()
        versionName = computedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
    }

    compileOptions {
        // JGit 7.x requires Java 17, ensuring compatibility here
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            // Bouncy Castle 1.87 ships these legal documents in multiple JARs.
            // Merge rather than discard them, retaining all license/notice text.
            merges += setOf("META-INF/LICENSE.md", "META-INF/NOTICE.md")
            // JGit and jgit-ssh.apache both contain this duplicate OSGi metadata file.
            excludes += "OSGI-INF/l10n/plugin.properties"
        }
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += "**/libandroidx.graphics.path.so"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // EncryptedSharedPreferences for secure token/API key storage
    implementation(libs.androidx.security.crypto)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Material 3 interface, progressive Haze blur and Tabler Icons.
    implementation(libs.haze.core)
    implementation(libs.haze.blur)
    implementation(libs.tabler.icons)

    implementation(libs.androidx.navigation.compose)
    // JGit & Required Crypto Dependencies for modern SSH support
    implementation(libs.jgit)
    implementation(libs.jgit.ssh)
    implementation(libs.sshd.osgi)
    implementation(libs.bcprov)
    implementation(libs.bcpkix)

    // Gemini AI
    implementation(libs.generativeai)

    implementation(libs.slf4j.simple)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
