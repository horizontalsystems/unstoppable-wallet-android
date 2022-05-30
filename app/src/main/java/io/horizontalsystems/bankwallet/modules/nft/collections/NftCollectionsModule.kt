package io.horizontalsystems.bankwallet.modules.nft.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.modules.nft.collection.assets.CollectionAsset
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle

object NftCollectionsModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val assetItemsRepository = NftAssetItemsRepository(App.nftManager)
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
    LastSale(TranslatableString.ResString(R.string.Nfts_PriceType_LastSale)),
    Days7(TranslatableString.ResString(R.string.Nfts_PriceType_Days_7)),
    Days30(TranslatableString.ResString(R.string.Nfts_PriceType_Days_30))
}

data class NftCollectionViewItem(
    val slug: String,
    val name: String,
    val imageUrl: String?,
    val expanded: Boolean,
    val assets: List<CollectionAsset>
) {
    val ownedAssetCount = assets.size
}
