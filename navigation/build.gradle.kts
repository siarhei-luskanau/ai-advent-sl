plugins {
    id("composeMultiplatformConvention")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    androidLibrary.namespace = "template.navigation"
    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.kotlinx.serialization.json)
            implementation(projects.ui.uiCommon)
            implementation(projects.ui.uiMain)
            implementation(projects.ui.uiSplash)
        }
    }
}
