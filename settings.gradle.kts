pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "YT Cutter"
include(":app")
include(":components")
include(
    ":core:model",
    ":core:common",
    ":core:database",
    ":core:datastore",
    ":core:network",
    ":core:notifications",
    ":core:designsystem",
    ":core:navigation",
    ":core:data",
    ":feature:home",
    ":feature:downloads",
    ":feature:settings",
    ":feature:cookies"
)
