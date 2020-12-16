package io.horizontalsystems.bankwallet.modules.derivatoinsettings

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.DerivationSetting

class DerivationSettingsInteractor(
        private val derivationSettingsManager: IDerivationSettingsManager,
        private val coinManager: ICoinManager
) : DerivationSettingsModule.IInteractor {

    override val allActiveSettings: List<Pair<DerivationSetting, Coin>>
        get() = derivationSettingsManager.allActiveSettings()

    override fun derivation(coinType: CoinType): Derivation {
        return derivationSettingsManager.setting(coinType)?.derivation
                ?: derivationSettingsManager.defaultSetting(coinType)?.derivation
                ?: throw Exception("No derivation found for ${coinType.javaClass.simpleName}")
    }

    override fun getCoin(coinType: CoinType): Coin {
        return coinManager.coins.first { it.type == coinType }
    }

    override fun saveDerivation(derivationSetting: DerivationSetting) {
        derivationSettingsManager.save(derivationSetting)
    }

}
