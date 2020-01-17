package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinSettingsManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.entities.*

class CoinSettingsManager(private val localStorage: ILocalStorage): ICoinSettingsManager {

    private val defaultDerivation = AccountType.Derivation.bip49
    private val defaultSyncMode = SyncMode.Fast

    override var bitcoinDerivation: AccountType.Derivation
        get() {
            return localStorage.bitcoinDerivation ?: defaultDerivation
        }
        set(derivation) {
            localStorage.bitcoinDerivation = derivation
        }

    override var syncMode: SyncMode
        get() {
            return localStorage.syncMode ?: defaultSyncMode
        }
        set(syncMode) {
            localStorage.syncMode = syncMode
        }

    override fun coinSettingsForCreate(coinType: CoinType): CoinSettings {
        val coinSettings = mutableMapOf<CoinSetting, String>()

        coinType.settings.forEach { setting ->
            when(setting) {
                CoinSetting.Derivation -> {
                    coinSettings[CoinSetting.Derivation] = defaultDerivation.value
                }
                CoinSetting.SyncMode -> {
                    coinSettings[CoinSetting.SyncMode] = SyncMode.New.value
                }
            }
        }

        return coinSettings
    }

    override fun coinSettings(coinType: CoinType): CoinSettings {
        val coinSettings = mutableMapOf<CoinSetting, String>()

        coinType.settings.forEach { setting ->
            when(setting) {
                CoinSetting.Derivation -> {
                    coinSettings[CoinSetting.Derivation] = bitcoinDerivation.value
                }
                CoinSetting.SyncMode -> {
                    coinSettings[CoinSetting.SyncMode] = syncMode.value
                }
            }
        }

        return coinSettings
    }

}