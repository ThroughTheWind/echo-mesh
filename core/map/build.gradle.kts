plugins {
    alias(libs.plugins.conventions.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    buildFeatures {
        compose = true
    }
}

// The offline map (ADR-0006): VTM drawing Mapsforge .map files the user imports. No network code:
// vtm-http and OkHttp are in gradle/forbidden-dependencies.txt.
dependencies {
    api(project(":core:model"))
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.vtm.android)
    implementation(libs.vtm.themes)
    // VTM's tessellation library, about 45 KB per ABI. The emulator ABI is for CI and development.
    for (abi in listOf("arm64-v8a", "armeabi-v7a", "x86_64")) {
        runtimeOnly(variantOf(libs.vtm.android) { classifier("natives-$abi") })
    }
}
