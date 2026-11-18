plugins {
    alias(libs.plugins.conventions.jvm.library)
    `java-test-fixtures`
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    // The decoder's fuzz test (ADR-0005, .ai/rules/crypto.md). CI fuzzes with JAZZER_FUZZ=1; a plain
    // test run replays the committed seeds and every past finding.
    testImplementation(libs.jazzer.junit)
    // MeshTransportContract: every transport adapter's tests extend it (.ai/rules/transports.md).
    testFixturesApi(libs.kotlinx.coroutines.test)
    testFixturesApi(libs.kotlin.test)
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter)
}
