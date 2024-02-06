package io.horizontalsystems.bankwallet.modules.swapxxx.settings

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SwapSettingsService {
    private var errors = mutableMapOf<String, Throwable>()
    private var settings = mapOf<String, Any?>()
    private var modifiedSettings = mutableMapOf<String, Any?>()

    private val _saveEnabledFlow = MutableStateFlow(isSaveEnabled())
    val saveEnabledFlow: StateFlow<Boolean>
        get() = _saveEnabledFlow.asStateFlow()

    fun setSetting(id: String, value: Any?) {
        if (settings[id] == value) {
            modifiedSettings.remove(id)
        } else {
            modifiedSettings[id] = value
        }

        Log.e("AAA", "settings: $settings")
        Log.e("AAA", "modifiedSettings: $modifiedSettings")

        emitState()
    }

    fun onSettingError(id: String, error: Throwable?) {
        if (error == null) {
            errors.remove(id)
        } else {
            errors[id] = error
        }

        emitState()
    }

    fun save() {
        settings += modifiedSettings
    }

    private fun emitState() {
        _saveEnabledFlow.update {
            isSaveEnabled()
        }
    }

    private fun isSaveEnabled(): Boolean {
        return errors.isEmpty() && modifiedSettings.isNotEmpty()
    }

    fun getSettings() = settings
}
