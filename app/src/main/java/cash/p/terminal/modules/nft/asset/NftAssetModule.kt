package cash.p.terminal.modules.nft.asset

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.entities.nft.NftUid
import cash.p.terminal.modules.balance.BalanceXRateRepository
import kotlinx.parcelize.Parcelize

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

    @Parcelize
    data class Input(val collectionUid: String?, val nftUidString: String) : Parcelable {
        val nftUid: NftUid
            get() = NftUid.fromUid(nftUidString)

        constructor(collectionUid: String?, nftUid: NftUid) : this(collectionUid, nftUid.uid)
    }

    enum class Tab(@StringRes val titleResId: Int) {
        Overview(R.string.NftAsset_Overview),
        Activity(R.string.NftAsset_Activity);
    }

    enum class NftAssetAction(@StringRes val title: Int) {
        Share(R.string.NftAsset_Action_Share),
        Save(R.string.NftAsset_Action_Save)
    }
}