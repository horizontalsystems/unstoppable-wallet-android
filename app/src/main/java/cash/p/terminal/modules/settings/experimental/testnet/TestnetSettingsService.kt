package cash.p.terminal.modules.settings.experimental.testnet

import cash.p.terminal.core.managers.EvmTestnetManager

class TestnetSettingsService(private val testnetManager: EvmTestnetManager) {
    val isTestnetEnabled = testnetManager.isTestnetEnabled

    fun setTestnetMode(enabled: Boolean) {
        testnetManager.isTestnetEnabled = enabled
    }
}
