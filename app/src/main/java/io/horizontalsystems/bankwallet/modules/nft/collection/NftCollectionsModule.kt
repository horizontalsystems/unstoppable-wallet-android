package io.horizontalsystems.bankwallet.modules.nft.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.modules.nft.NftAssetContract
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle

object NftCollectionsModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val nftItemFactory = NftItemFactory(App.coinManager)

            val assetItemsRepository = NftAssetItemsRepository(App.nftManager, nftItemFactory)
            val assetItemsPricedRepository = NftAssetItemsPricedRepository()
            val assetItemsPricedWithCurrencyRepository = NftAssetItemsPricedWithCurrencyRepository(
                BalanceXRateRepository(App.currencyManager, App.marketKit)
            )

            val service = NftCollectionsService(
                App.accountManager,
                assetItemsRepository,
                assetItemsPricedRepository,
                assetItemsPricedWithCurrencyRepository
            )

            return NftCollectionsViewModel(service) as T
        }
    }
}

enum class PriceType(override val title: TranslatableString) : WithTranslatableTitle {
    Days7(TranslatableString.ResString(R.string.Nfts_PriceType_Days_7)),
    Days30(TranslatableString.ResString(R.string.Nfts_PriceType_Days_30)),
    LastSale(TranslatableString.ResString(R.string.Nfts_PriceType_LastSale))
}

data class NftCollectionViewItem(
    val slug: String,
    val name: String,
    val imageUrl: String,
    val expanded: Boolean,
    val assets: List<NftAssetItemPricedWithCurrency>
) {
    val ownedAssetCount = assets.size
}

data class NftAssetItem(
    val accountId: String,
    val tokenId: String,
    val name: String,
    val imageUrl: String,
    val imagePreviewUrl: String,
    val description: String,
    val ownedCount: Int,
    val contract: NftAssetContract,
    val onSale: Boolean,
    val prices: Prices
) {
    data class Prices(
        val average7d: CoinValue?,
        val average30d: CoinValue?,
        val last: CoinValue?,
    )
}

data class NftAssetItemPriced(
    val assetItem: NftAssetItem,
    val coinPrice: CoinValue?
)

data class NftAssetItemPricedWithCurrency(
    val assetItem: NftAssetItem,
    val coinPrice: CoinValue?,
    val currencyPrice: CurrencyValue?,
)
