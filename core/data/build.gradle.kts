plugins {
    alias(libs.plugins.conventions.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

// The encrypted local store and settings (spec FR-12, threat model §5.6). Plain JVM: the app hands
// it a directory and the Keystore key from :core:crypto:keystore, so every test runs without Android.
dependencies {
    api(project(":core:crypto"))
    implementation(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)
}
