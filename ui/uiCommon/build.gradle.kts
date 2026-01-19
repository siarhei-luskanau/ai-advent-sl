plugins {
    id("composeMultiplatformConvention")
}

kotlin.androidLibrary.namespace = "template.ui.common"

compose.resources {
    publicResClass = true
    packageOfResClass = "${kotlin.androidLibrary.namespace}.resources"
    generateResClass = always
}
