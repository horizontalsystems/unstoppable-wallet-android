package io.horizontalsystems.bankwallet.modules.multiswap.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.ViewModelUiState

class SwapSettingsViewModel(settings: Map<String, Any?>) : ViewModelUiState<SwapSettingsUiState>() {
    private var errors = mutableMapOf<String, Throwable>()
    private val settings = settings.toMutableMap()

    override fun createState() = SwapSettingsUiState(
        applyEnabled = errors.isEmpty(),
    )

    fun onSettingError(id: String, error: Throwable?) {
        if (error == null) {
            errors.remove(id)
        } else {
            errors[id] = error
        }

        emitState()
    }

    fun onSettingEnter(id: String, value: Any?) {
        settings[id] = value
    }

    fun getSettings() = settings

    class Factory(private val settings: Map<String, Any?>) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwapSettingsViewModel(settings) as T
        }
    }
}

data class SwapSettingsUiState(val applyEnabled: Boolean)
