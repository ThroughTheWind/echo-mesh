plugins {
    alias(libs.plugins.conventions.jvm.library)
}

dependencies {
    api(project(":core:mesh"))
    testImplementation(testFixtures(project(":core:mesh")))
}
