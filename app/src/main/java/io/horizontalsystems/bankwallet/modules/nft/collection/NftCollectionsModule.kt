package io.horizontalsystems.bankwallet.modules.nft.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.nft.NftAssetContract
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle

object NftCollectionsModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val nftItemFactory = NftItemFactory(App.coinManager)

            val repository = NftCollectionsRepository(App.nftManager, App.accountManager, nftItemFactory)

            val service = NftCollectionsService(repository, App.marketKit, App.currencyManager)

            return NftCollectionsViewModel(service) as T
        }
    }
}

enum class PriceType(override val title: TranslatableString) : WithTranslatableTitle {
    Days7(TranslatableString.ResString(R.string.Nfts_PriceType_Days_7)),
    Days30(TranslatableString.ResString(R.string.Nfts_PriceType_Days_30)),
    LastPrice(TranslatableString.ResString(R.string.Nfts_PriceType_LastPrice))
}

data class NftCollectionViewItem(
    val slug: String,
    val name: String,
    val imageUrl: String,
    val expanded: Boolean,
    val assets: List<NftAssetItemPriced>
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
    val coinPrice: CoinValue?,
    val currencyPrice: CurrencyValue?
)
