plugins {
    id("composeMultiplatformConvention")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    androidLibrary.namespace = "template.core.pref"
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
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
