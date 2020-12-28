package io.horizontalsystems.bankwallet.modules.derivatoinsettings

import io.horizontalsystems.bankwallet.core.IDerivationSettingsManager
import io.horizontalsystems.bankwallet.core.managers.BitcoinCashCoinTypeManager
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.BitcoinCashCoinType
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.reactivex.subjects.BehaviorSubject

class DerivationSettingsService(
        private val derivationSettingsManager: IDerivationSettingsManager,
        private val bitcoinCashCoinTypeManager: BitcoinCashCoinTypeManager
) : DerivationSettingsModule.IService {

    override val itemsObservable = BehaviorSubject.create<List<DerivationSettingsModule.Item>>()
    override var items = listOf<DerivationSettingsModule.Item>()
        private set(value) {
            field = value
            itemsObservable.onNext(value)
        }

    init {
        syncItems()
    }

    private fun syncItems() {
        val settingItems = mutableListOf<DerivationSettingsModule.Item>()

        derivationSettingsManager.allActiveSettings().forEach { (setting, coinType) ->
            settingItems.add(DerivationSettingsModule.Item(coinType, DerivationSettingsModule.ItemType.Derivation(AccountType.Derivation.values().asList(), setting.derivation)))
        }

        if (bitcoinCashCoinTypeManager.hasActiveSetting) {
            settingItems.add(DerivationSettingsModule.Item(CoinType.BitcoinCash, DerivationSettingsModule.ItemType.BitcoinCashType(BitcoinCashCoinType.values().asList(), bitcoinCashCoinTypeManager.bitcoinCashCoinType)))
        }

        items = settingItems
    }

    override fun set(derivation: AccountType.Derivation, coinType: CoinType) {
        derivationSettingsManager.save(DerivationSetting(coinType, derivation))

        syncItems()
    }

    override fun set(bitcoinCashCoinType: BitcoinCashCoinType) {
        bitcoinCashCoinTypeManager.save(bitcoinCashCoinType)

        syncItems()
    }
}
