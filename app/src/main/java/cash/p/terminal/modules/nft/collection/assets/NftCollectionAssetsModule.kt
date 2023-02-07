package cash.p.terminal.modules.nft.collection.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.modules.balance.BalanceXRateRepository
import io.horizontalsystems.marketkit.models.BlockchainType

object NftCollectionAssetsModule {

    class Factory(
        private val blockchainType: BlockchainType,
        private val collectionUid: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = NftCollectionAssetsService(
                blockchainType,
                collectionUid,
                App.nftMetadataManager.provider(blockchainType),
                BalanceXRateRepository(App.currencyManager, App.marketKit)
            )
            return NftCollectionAssetsViewModel(service) as T
        }
    }

}
