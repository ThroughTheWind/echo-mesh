plugins {
    alias(libs.plugins.conventions.android.application)
    alias(libs.plugins.kotlin.compose)
}

// The app version lives once, in gradle/libs.versions.toml. The version code is derived from it
// (MAJOR * 1_000_000 + MINOR * 1_000 + PATCH) so the two can never disagree: 0.1.0 is 1000.
val appVersion = libs.versions.app.get()
val appVersionCode: Int =
    run {
        val parts = appVersion.split(".").map { it.toIntOrNull() ?: -1 }
        require(parts.size == 3 && parts.all { it in 0..999 }) {
            "The app version must be MAJOR.MINOR.PATCH with parts from 0 to 999, got $appVersion"
        }
        parts[0] * 1_000_000 + parts[1] * 1_000 + parts[2]
    }

// Release signing comes from the environment, which the release workflow fills from GitHub secrets
// (docs/releasing.md). Without it, assembleRelease still builds an unsigned APK.
val releaseKeystore = providers.environmentVariable("RELEASE_KEYSTORE_FILE").orNull
val packageRoot = providers.gradleProperty("app.packageRoot").get()

android {
    namespace = packageRoot

    defaultConfig {
        applicationId = packageRoot
        versionCode = appVersionCode
        versionName = appVersion
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = providers.environmentVariable("RELEASE_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-dev"
            // en-XA and ar-XB in debug builds: a run in each catches truncation and hard-coded text (NFR-21).
            isPseudoLocalesEnabled = true
        }
    }

    buildFeatures {
        compose = true
    }
}

// ADR-0003: the app never asks for INTERNET. A permission is added to `allowed` only by an ADR that
// names it, cited on the same line. Today: none.
manifestPermissions {
    // No internet (ADR-0003), and never background location (NFR-10): sharing uses a `location`
    // foreground service started from a tap instead (WI-075).
    forbidden.set(setOf("android.permission.INTERNET", "android.permission.ACCESS_BACKGROUND_LOCATION"))
    allowed.set(emptySet())
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:mesh"))
    implementation(project(":core:crypto"))
    implementation(project(":core:crypto:keystore"))
    implementation(project(":core:data"))
    implementation(project(":core:map"))
    implementation(project(":core:designsystem"))
    implementation(project(":adapters:ble"))
    // The simulated radio is for development only; CI proves the release build does not link it.
    debugImplementation(project(":adapters:loopback"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.zxing.core)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
