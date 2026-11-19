// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.detekt)
}

// One Detekt run over every module's sources, without type resolution, so it
// needs no Android variant and no compilation.
detekt {
    source.setFrom(
        fileTree(rootDir) {
            include("**/src/**/*.kt")
            exclude("**/build/**")
            // Session worktrees are full copies of the project
            exclude(".claude/**")
        }
    )
    config.setFrom(file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    baseline = file("config/detekt/baseline.xml")
    parallel = true
    basePath.set(rootDir)
}

dependencies {
    detektPlugins(libs.compose.rules.detekt)
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
