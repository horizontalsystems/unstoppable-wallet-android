package cash.p.terminal.modules.nft.holdings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.entities.CoinValue
import io.horizontalsystems.core.entities.CurrencyValue
import cash.p.terminal.entities.nft.NftUid
import cash.p.terminal.modules.balance.DefaultBalanceXRateRepository
import cash.p.terminal.modules.balance.TotalBalance
import cash.p.terminal.modules.balance.TotalService

object NftHoldingsModule {
    class Factory(
        private val account: cash.p.terminal.wallet.Account
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val totalService = TotalService(App.currencyManager, App.marketKit, App.baseTokenManager, App.balanceHiddenManager)
            val xRateRepository = DefaultBalanceXRateRepository("nft-holding", App.currencyManager, App.marketKit)
            val service = NftHoldingsService(account, App.nftAdapterManager, App.nftMetadataManager, App.nftMetadataSyncer, xRateRepository)
            return NftHoldingsViewModel(service, TotalBalance(totalService, App.balanceHiddenManager)) as T
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
    val collectionUid: String?,
    val nftUid: NftUid,
    val name: String,
    val imageUrl: String?,
    val count: Int,
    val onSale: Boolean,
    val price: CoinValue?,
    val priceInFiat: CurrencyValue?
)