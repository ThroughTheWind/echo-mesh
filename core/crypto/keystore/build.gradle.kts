plugins {
    alias(libs.plugins.conventions.android.library)
}

// The Android Keystore half of :core:crypto: it holds the key that seals what the app stores
// (threat model §5.6). It lives under core/crypto/ because it uses a cryptographic API
// (checkCryptoBoundaries), and is a separate module because :core:crypto runs on the plain JVM.
dependencies {
    api(project(":core:crypto"))
    implementation(libs.tink.android)
}
