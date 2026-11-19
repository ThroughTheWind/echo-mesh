pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "echo"

include(":app")
include(":core:model")
include(":core:mesh")
include(":core:crypto")
include(":core:crypto:keystore")
include(":core:data")
include(":core:map")
include(":core:designsystem")
include(":adapters:loopback")
include(":adapters:ble")
include(":adapters:wifidirect")
include(":adapters:wifiaware")
