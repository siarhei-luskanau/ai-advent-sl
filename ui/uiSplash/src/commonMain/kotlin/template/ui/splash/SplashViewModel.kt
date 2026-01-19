package template.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val navigationCallback: SplashNavigationCallback,
) : ViewModel() {
    val viewState = MutableStateFlow<SplashViewState>(SplashViewState.Loading)

    fun onEvent(event: SplashViewEvent) {
        when (event) {
            SplashViewEvent.Launched -> {
                viewModelScope.launch {
                    navigationCallback.goChatScreen()
                }
            }
        }
    }
}
