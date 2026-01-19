plugins {
    id("androidTestConvention")
    id("composeMultiplatformConvention")
}

kotlin {
    androidLibrary.namespace = "template.app"
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.coreCommon)
            implementation(projects.core.corePref)
            implementation(projects.navigation)
            implementation(projects.ui.uiCommon)
            implementation(projects.ui.uiMain)
            implementation(projects.ui.uiSplash)
        }

        jvmMain.dependencies {
            implementation(libs.androidx.datastore.core.okio)
        }

        androidMain.dependencies {
            implementation(libs.androidx.datastore.core.okio)
        }

        iosMain.dependencies {
            implementation(libs.androidx.datastore.core.okio)
        }
    }
}
