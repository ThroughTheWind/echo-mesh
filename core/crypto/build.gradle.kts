plugins {
    alias(libs.plugins.conventions.jvm.library)
}

// The only module allowed to import a cryptographic API (checkCryptoBoundaries, .ai/rules/crypto.md).
// Tink is its one crypto library, the same artifact on the JVM and on Android (ADR-0004).
dependencies {
    implementation(libs.tink.android)
}
