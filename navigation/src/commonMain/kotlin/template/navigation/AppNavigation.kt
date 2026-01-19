package template.navigation

import androidx.navigation3.runtime.NavKey
import template.ui.main.MainNavigationCallback
import template.ui.splash.SplashNavigationCallback

internal class AppNavigation(
    private val backStack: MutableList<NavKey>,
) : MainNavigationCallback,
    SplashNavigationCallback {
    override fun goBack() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    override fun goMainScreen(initArg: String) {
        backStack.add(AppRoutes.Main(initArg = initArg))
        backStack.remove(AppRoutes.Splash)
    }
}
