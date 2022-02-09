package io.horizontalsystems.bankwallet.modules.nft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle

object NftsModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val accountRepository = NftsAccountRepository(App.accountManager)
            val nftManager = NftManager(App.appDatabase.nftCollectionDao())
            val service = NftsService(nftManager, accountRepository)

            return NftsViewModel(service) as T
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
    val ownedAssetCount: Long,
    val expanded: Boolean,
    val assets: List<ViewItemNftAsset>
)

data class ViewItemNftAsset(
    val tokenId: String,
    val name: String,
    val imageUrl: String,
    val coinPrice: CoinValue,
    val currencyPrice: CurrencyValue,
    val onSale: Boolean
)
