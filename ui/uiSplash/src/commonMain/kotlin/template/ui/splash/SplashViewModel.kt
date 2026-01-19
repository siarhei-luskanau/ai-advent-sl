package template.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class SplashViewModel(
    private val navigationCallback: SplashNavigationCallback,
) : ViewModel() {
    val viewState = MutableStateFlow<SplashViewState>(SplashViewState.Loading)

    fun onEvent(event: SplashViewEvent) {
        when (event) {
            SplashViewEvent.Launched -> {
                viewModelScope.launch {
                    navigationCallback.goMainScreen(initArg = Random.nextInt(16).toString())
                }
            }
        }
    }
}
