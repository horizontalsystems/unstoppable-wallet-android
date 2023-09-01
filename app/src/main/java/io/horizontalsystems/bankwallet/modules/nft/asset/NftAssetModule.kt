package io.horizontalsystems.bankwallet.modules.nft.asset

import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository

object NftAssetModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val collectionUid: String,
        private val nftUid: NftUid
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = NftAssetService(
                collectionUid,
                nftUid,
                App.accountManager,
                App.nftAdapterManager,
                App.nftMetadataManager.provider(nftUid.blockchainType),
                BalanceXRateRepository("nft-asset", App.currencyManager, App.marketKit)
            )
            return NftAssetViewModel(service) as T
        }
    }

    const val collectionUidKey = "collectionUidKey"
    const val nftUidKey = "nftUidKey"

    fun prepareParams(collectionUid: String?, nftUid: NftUid) = bundleOf(
        collectionUidKey to collectionUid,
        nftUidKey to nftUid.uid
    )

    enum class Tab(@StringRes val titleResId: Int) {
        Overview(R.string.NftAsset_Overview),
        Activity(R.string.NftAsset_Activity);
    }

    enum class NftAssetAction(@StringRes val title: Int) {
        Share(R.string.NftAsset_Action_Share),
        Save(R.string.NftAsset_Action_Save)
    }
}