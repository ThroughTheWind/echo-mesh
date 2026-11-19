plugins {
    alias(libs.plugins.conventions.android.library)
}

// The Bluetooth LE transport (WI-011). Depends on :core:mesh only (.ai/rules/transports.md).
dependencies {
    api(project(":core:mesh"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(testFixtures(project(":core:mesh")))
}
