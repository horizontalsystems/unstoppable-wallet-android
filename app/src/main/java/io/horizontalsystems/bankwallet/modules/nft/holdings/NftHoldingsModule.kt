package io.horizontalsystems.bankwallet.modules.nft.holdings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.modules.balance.TotalService

object NftHoldingsModule {
    class Factory(
        private val account: Account
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
//            val assetItemsRepository = NftAssetItemsRepository(App.nftManager)
//            val assetItemsPricedRepository = NftAssetItemsPricedRepository()
//            val assetItemsPricedWithCurrencyRepository = NftAssetItemsPricedWithCurrencyRepository(
//                BalanceXRateRepository(App.currencyManager, App.marketKit)
//            )
//
//            val service = NftCollectionsService(
//                App.accountManager,
//                assetItemsRepository,
//                assetItemsPricedRepository,
//                assetItemsPricedWithCurrencyRepository
//            )
            val totalService = TotalService(App.currencyManager, App.marketKit, App.baseTokenManager, App.balanceHiddenManager)
            val xRateRepository = BalanceXRateRepository(App.currencyManager, App.marketKit)
            val service = NftHoldingsService(account, App.nftAdapterManager, App.nftMetadataManager, App.nftMetadataSyncer, xRateRepository)
            return NftHoldingsViewModel(service, totalService, App.balanceHiddenManager) as T
        }
    }

}

data class NftCollectionViewItem(
    val uid: String,
    val name: String,
    val imageUrl: String?,
    val count: Int,
    val expanded: Boolean,
    val assets: List<NftAssetViewItem>
)

data class NftAssetViewItem(
    val name: String,
    val imageUrl: String?,
    val collectionUid: String?,
    val count: Int,
    val onSale: Boolean,
    val priceInCoin: CoinValue?,
    val priceInFiat: CurrencyValue?
)