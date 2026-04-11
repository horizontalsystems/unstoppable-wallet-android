plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(libs.lint.api)
    compileOnly(libs.lint.checks)

    testImplementation(libs.lint.lint)
    testImplementation(libs.lint.tests)
    testImplementation(libs.junit)
}
