package io.horizontalsystems.bankwallet.modules.addressformat

import io.horizontalsystems.bankwallet.core.IDerivationSettingsManager
import io.horizontalsystems.bankwallet.core.managers.BitcoinCashCoinTypeManager
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.BitcoinCashCoinType
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.horizontalsystems.coinkit.models.CoinType
import io.reactivex.subjects.BehaviorSubject

class AddressFormatService(
        private val derivationSettingsManager: IDerivationSettingsManager,
        private val bitcoinCashCoinTypeManager: BitcoinCashCoinTypeManager
) : AddressFormatModule.IService {

    override val itemsAsync = BehaviorSubject.create<List<AddressFormatModule.Item>>()
    override var items = listOf<AddressFormatModule.Item>()
        private set(value) {
            field = value
            itemsAsync.onNext(value)
        }

    init {
        syncItems()
    }

    private fun syncItems() {
        val settingItems = mutableListOf<AddressFormatModule.Item>()

        derivationSettingsManager.allActiveSettings().forEach { (setting, coinType) ->
            settingItems.add(AddressFormatModule.Item(coinType, AddressFormatModule.ItemType.Derivation(AccountType.Derivation.values().asList(), setting.derivation)))
        }

        if (bitcoinCashCoinTypeManager.hasActiveSetting) {
            settingItems.add(AddressFormatModule.Item(CoinType.BitcoinCash, AddressFormatModule.ItemType.BitcoinCashType(BitcoinCashCoinType.values().asList(), bitcoinCashCoinTypeManager.bitcoinCashCoinType)))
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
