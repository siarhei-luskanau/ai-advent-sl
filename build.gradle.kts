plugins {
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.compose.multiplatform).apply(false)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.android).apply(false)
    alias(libs.plugins.kotlin.jvm).apply(false)
    alias(libs.plugins.kotlin.multiplatform).apply(false)
}

allprojects {
    apply(from = "$rootDir/ktlint.gradle")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    detekt {
        parallel = true
        ignoreFailures = false
    }
}
