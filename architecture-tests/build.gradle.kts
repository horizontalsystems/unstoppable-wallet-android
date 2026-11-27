// Architecture rules as unit tests. Konsist reads the Kotlin sources of every
// module straight from disk, so this module depends on none of them and runs
// without compiling the app.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.konsist)
}

// The inputs are other modules' sources, which Gradle does not track here.
tasks.withType<Test>().configureEach {
    outputs.upToDateWhen { false }
}
