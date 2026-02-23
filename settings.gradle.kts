pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Unstoppable"

include(":app")
include(":core")
include(":components:icons")
include(":components:chartview")
include(":subscriptions-core")
if (file("subscriptions-google-play").exists()) {
    include(":subscriptions-google-play")
}
include(":subscriptions-dev")
include(":subscriptions-fdroid")
