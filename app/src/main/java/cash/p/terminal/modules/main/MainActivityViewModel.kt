package cash.p.terminal.modules.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.managers.UserManager
import kotlinx.coroutines.launch

class MainActivityViewModel(userManager: UserManager) : ViewModel() {

    val navigateToMainLiveData = MutableLiveData(false)

    private var userLevel = userManager.currentUserLevelFlow.value

    init {
        viewModelScope.launch {
            userManager.currentUserLevelFlow.collect {
                if (it != userLevel) {
                    navigateToMainLiveData.postValue(true)
                }
            }
        }
    }

    fun onNavigatedToMain() {
        navigateToMainLiveData.postValue(false)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainActivityViewModel(App.userManager) as T
        }
    }
}
