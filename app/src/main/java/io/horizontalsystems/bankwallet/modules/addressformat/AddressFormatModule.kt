package io.horizontalsystems.bankwallet.modules.addressformat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.views.ListPosition
import io.reactivex.Observable

object AddressFormatModule {

    interface IService {
        val items: List<Item>
        val itemsAsync: Observable<List<Item>>
        fun set(derivation: Derivation, coinType: CoinType)
        fun set(bitcoinCashCoinType: BitcoinCashCoinType)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = AddressFormatService(App.derivationSettingsManager, App.bitcoinCashCoinTypeManager)

            return AddressFormatViewModel(service, StringProvider()) as T
        }
    }

    data class Item(val coinType: CoinType, val type: ItemType)

    sealed class ItemType {
        class Derivation(val derivations: List<AccountType.Derivation>, var current: AccountType.Derivation) : ItemType()
        class BitcoinCashType(val types: List<BitcoinCashCoinType>, var current: BitcoinCashCoinType) : ItemType()
    }

    data class SectionItem(val coinTypeName: String, val viewItems: List<ViewItem>)
    data class ViewItem(val title: String, val subtitle: String, val selected: Boolean, val listPosition: ListPosition)
}
