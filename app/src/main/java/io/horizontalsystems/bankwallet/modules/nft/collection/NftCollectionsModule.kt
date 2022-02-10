package io.horizontalsystems.bankwallet.modules.nft.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.nft.NftManager
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle

object NftCollectionsModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val accountRepository = NftCollectionsAccountRepository(App.accountManager)
            val nftManager = NftManager(App.appDatabase.nftCollectionDao())
            val service = NftCollectionsService(nftManager, accountRepository)

            return NftCollectionsViewModel(service) as T
        }
    }
}

enum class PriceType(override val title: TranslatableString) : WithTranslatableTitle {
    Days7(TranslatableString.ResString(R.string.Nfts_PriceType_Days_7)),
    Days30(TranslatableString.ResString(R.string.Nfts_PriceType_Days_30)),
    LastPrice(TranslatableString.ResString(R.string.Nfts_PriceType_LastPrice))
}

data class ViewItemNftCollection(
    val slug: String,
    val name: String,
    val imageUrl: String,
    val ownedAssetCount: Int,
    val expanded: Boolean,
    val assets: List<ViewItemNftAsset>
)

data class ViewItemNftAsset(
    val tokenId: String,
    val name: String,
    val imagePreviewUrl: String,
    val coinPrice: CoinValue,
    val currencyPrice: CurrencyValue,
    val onSale: Boolean
)
