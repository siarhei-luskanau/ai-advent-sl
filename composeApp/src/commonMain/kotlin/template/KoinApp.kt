package template

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinMultiplatformApplication
import org.koin.core.module.Module
import org.koin.dsl.KoinConfiguration
import org.koin.dsl.module
import template.core.common.coreCommonModule
import template.core.pref.corePrefModule
import template.navigation.NavApp
import template.ui.main.MainViewModel
import template.ui.splash.SplashViewModel

@Preview
@Composable
fun KoinApp() =
    KoinMultiplatformApplication(
        config =
            KoinConfiguration {
                modules(
                    appModule,
                    appPlatformModule,
                    coreCommonModule,
                    corePrefModule,
                )
            },
    ) {
        NavApp()
    }

expect val appPlatformModule: Module

val appModule by lazy {
    module {
        factory { SplashViewModel(navigationCallback = it[0]) }
        factory {
            MainViewModel(
                initArg = it[0],
                navigationCallback = it[1],
            )
        }
    }
}
