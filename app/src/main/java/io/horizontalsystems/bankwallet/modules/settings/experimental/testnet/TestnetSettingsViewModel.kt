package io.horizontalsystems.bankwallet.modules.settings.experimental.testnet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class TestnetSettingsViewModel(private val service: TestnetSettingsService) : ViewModel() {
    var testnetEnabled by mutableStateOf(service.isTestnetEnabled)
        private set

    fun setTestnetMode(enabled: Boolean) {
        testnetEnabled = enabled
        service.setTestnetMode(enabled)
    }
}
