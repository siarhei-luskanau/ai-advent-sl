plugins {
    id("composeMultiplatformConvention")
}

kotlin {
    androidLibrary.namespace = "template.ui.main"
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.coreCommon)
            implementation(projects.ui.uiCommon)
        }
    }
}
