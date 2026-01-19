package template.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import org.koin.compose.getKoin
import org.koin.core.parameter.parametersOf
import template.ui.common.theme.AppTheme
import template.ui.main.MainScreen
import template.ui.splash.SplashScreen

@Preview
@Composable
fun NavApp() =
    AppTheme {
        val koin = getKoin()
        val backStack = mutableStateListOf<NavKey>(AppRoutes.Splash)
        val appNavigation = AppNavigation(backStack = backStack)
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider =
                entryProvider {
                    entry<AppRoutes.Splash> {
                        SplashScreen {
                            koin.get { parametersOf(appNavigation) }
                        }
                    }
                    entry<AppRoutes.Main> {
                        MainScreen {
                            koin.get { parametersOf(it.initArg, appNavigation) }
                        }
                    }
                },
        )
    }

internal sealed interface AppRoutes : NavKey {
    @Serializable
    data object Splash : AppRoutes

    @Serializable
    data class Main(
        val initArg: String,
    ) : AppRoutes
}
