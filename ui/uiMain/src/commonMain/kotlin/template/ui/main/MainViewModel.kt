package template.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val initArg: String,
    private val navigationCallback: MainNavigationCallback,
) : ViewModel() {
    val viewState = MutableStateFlow<MainViewState>(MainViewState.Loading)

    init {
        viewModelScope.launch {
            viewState.value = MainViewState.Success(data = initArg)
        }
    }

    fun onEvent(event: MainViewEvent) {
        when (event) {
            MainViewEvent.NavigateBack -> viewModelScope.launch { navigationCallback.goBack() }
        }
    }
}
