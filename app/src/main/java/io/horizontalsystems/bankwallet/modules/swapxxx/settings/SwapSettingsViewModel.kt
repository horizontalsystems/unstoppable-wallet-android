package io.horizontalsystems.bankwallet.modules.swapxxx.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SwapSettingsViewModel(private val settingsService: SwapSettingsService) : ViewModel() {
    var saveSettingsEnabled by mutableStateOf(settingsService.saveEnabledFlow.value)

    init {
        viewModelScope.launch {
            settingsService.saveEnabledFlow.collect {
                saveSettingsEnabled = it
            }
        }
    }

    fun onSettingError(id: String, error: Throwable?) {
        settingsService.onSettingError(id, error)
    }

    fun onSettingEnter(id: String, value: Any?) {
        settingsService.setSetting(id, value)
    }

    fun saveSettings() {
        settingsService.save()
    }

    fun getSettings(): Map<String, Any?> {
        return settingsService.getSettings()
    }


    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val swapSettingsService = SwapSettingsService()

            return SwapSettingsViewModel(swapSettingsService) as T
        }
    }

}
