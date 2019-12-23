package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinSettingsManager
import io.horizontalsystems.bankwallet.entities.*

class CoinSettingsManager: ICoinSettingsManager {

    override fun coinSettingsToRequest(coin: Coin, accountOrigin: AccountOrigin): CoinSettings {
        val coinSettings = mutableMapOf<CoinSetting, String>()

        coin.type.settings.forEach { setting ->
            when(setting) {
                CoinSetting.Derivation -> {
                    coinSettings[CoinSetting.Derivation] = AccountType.Derivation.bip49.value
                }
                CoinSetting.SyncMode -> {
                    if (accountOrigin == AccountOrigin.Restored) {
                        coinSettings[CoinSetting.SyncMode] = SyncMode.Fast.value
                    }
                }
            }
        }

        return coinSettings
    }

    override fun coinSettingsToSave(coin: Coin, accountOrigin: AccountOrigin, requestedCoinSettings: CoinSettings): CoinSettings {
        coin.type.settings.forEach { setting ->
            when (setting) {
                CoinSetting.SyncMode -> {
                    if (accountOrigin == AccountOrigin.Created) {
                        requestedCoinSettings[CoinSetting.SyncMode] = SyncMode.New.value
                    }
                }
                else -> {

                }
            }
        }

        return requestedCoinSettings
    }
}