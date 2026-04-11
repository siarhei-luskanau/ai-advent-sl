plugins {
    id("composeMultiplatformConvention")
}

kotlin {
    androidLibrary.namespace = "template.ui.chat"
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.coreCommon)
            implementation(projects.ui.uiCommon)
            implementation(libs.koog.agents)
            implementation(libs.markdown.renderer)
        }
    }
}
