plugins {
    alias(libs.plugins.conventions.jvm.library)
    `java-test-fixtures`
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    // MeshTransportContract: every transport adapter's tests extend it (.ai/rules/transports.md).
    testFixturesApi(libs.kotlinx.coroutines.test)
    testFixturesApi(libs.kotlin.test)
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter)
}
