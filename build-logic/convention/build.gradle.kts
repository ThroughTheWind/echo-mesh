plugins {
    `kotlin-dsl`
}

group = "conventions"

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
}

kotlin {
    compilerOptions {
        jvmTarget.set(
            org.jetbrains.kotlin.gradle.dsl.JvmTarget
                .fromTarget(libs.versions.jvmTarget.get()),
        )
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)

    // The gate logic in conventions.gates is plain Kotlin, tested in both directions here
    // (AGENTS.md: a control that cannot fail loudly is not a control).
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("jvmLibrary") {
            id = "conventions.jvm.library"
            implementationClass = "conventions.JvmLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "conventions.android.library"
            implementationClass = "conventions.AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "conventions.android.application"
            implementationClass = "conventions.AndroidApplicationConventionPlugin"
        }
        register("gates") {
            id = "conventions.gates"
            implementationClass = "conventions.GatesConventionPlugin"
        }
    }
}
