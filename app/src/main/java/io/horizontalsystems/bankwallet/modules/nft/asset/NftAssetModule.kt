package io.horizontalsystems.bankwallet.modules.nft.asset

import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.modules.hsnft.HsNftApiProvider
import io.horizontalsystems.bankwallet.modules.hsnft.HsNftApiV1Response
import io.horizontalsystems.bankwallet.modules.nft.NftAssetContract
import java.util.*

object NftAssetModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val collectionUid: String,
        private val contractAddress: String,
        private val tokenId: String
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = NftAssetRepository(BalanceXRateRepository(App.currencyManager, App.marketKit))
            val service = NftAssetService(
                collectionUid,
                contractAddress,
                tokenId,
                HsNftApiProvider(),
                App.nftManager,
                repository
            )
            return NftAssetViewModel(service) as T
        }
    }

    const val collectionUidKey = "collectionUidKey"
    const val contractAddressKey = "contractAddressKey"
    const val tokenIdKey = "tokenIdKey"

    fun prepareParams(collectionUid: String, contractAddress: String, tokenId: String) = bundleOf(
        collectionUidKey to collectionUid,
        contractAddressKey to contractAddress,
        tokenIdKey to tokenId,
    )

}

data class NftAssetModuleAssetItem(
    val name: String?,
    val imageUrl: String?,
    val collectionName: String,
    val collectionUid: String,
    val description: String?,
    val contract: NftAssetContract,
    val tokenId: String,
    val assetLinks: HsNftApiV1Response.Asset.Links?,
    val collectionLinks: HsNftApiV1Response.Collection.Links?,
    val stats: Stats,
    val onSale: Boolean,
    val attributes: List<Attribute>
) {
    data class Price(
        val coinValue: CoinValue,
        val currencyValue: CurrencyValue? = null
    )

    data class Stats(
        val lastSale: Price?,
        val average7d: Price? = null,
        val average30d: Price? = null,
        val collectionFloor: Price? = null,
        val sale: Sale? = null,
        val bestOffer: Price? = null
    )

    data class Sale(
        val untilDate: Date?,
        val type: PriceType,
        val price: Price?
    ) {
        enum class PriceType {
            BuyNow, TopBid, MinimumBid
        }
    }

    data class Attribute(
        val type: String,
        val value: String,
        val percent: String?,
        val searchUrl: String
    )

    enum class NftAssetAction(@StringRes val title: Int) {
        Share(R.string.NftAsset_Action_Share),
        Save(R.string.NftAsset_Action_Save)
    }
}
