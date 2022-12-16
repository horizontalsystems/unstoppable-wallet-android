package io.horizontalsystems.bankwallet.modules.settings.experimental.testnet

import io.horizontalsystems.bankwallet.core.managers.EvmTestnetManager

class TestnetSettingsService(private val testnetManager: EvmTestnetManager) {
    val isTestnetEnabled = testnetManager.isTestnetEnabled

    fun setTestnetMode(enabled: Boolean) {
        testnetManager.isTestnetEnabled = enabled
    }
}
