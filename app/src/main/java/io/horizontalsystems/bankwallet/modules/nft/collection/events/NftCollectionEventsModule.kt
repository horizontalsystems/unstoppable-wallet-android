package io.horizontalsystems.bankwallet.modules.nft.collection.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.nft.NftEventMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.marketkit.models.BlockchainType

class NftCollectionEventsModule {

    class Factory(
        private val eventListType: NftEventListType,
        private val defaultEventType: NftEventMetadata.EventType = NftEventMetadata.EventType.Sale
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val nftProvider = when (eventListType) {
                is NftEventListType.Asset -> {
                    App.nftMetadataManager.provider(eventListType.nftUid.blockchainType)
                }
                is NftEventListType.Collection -> {
                    App.nftMetadataManager.provider(eventListType.blockchainType)
                }
            }

            val service = NftCollectionEventsService(
                eventListType,
                defaultEventType,
                nftProvider,
                BalanceXRateRepository(App.currencyManager, App.marketKit)
            )
            return NftCollectionEventsViewModel(service) as T
        }
    }

}

sealed class SelectorDialogState {
    object Closed : SelectorDialogState()
    class Opened(val select: Select<NftEventTypeWrapper>) : SelectorDialogState()
}

sealed class NftEventListType {
    data class Collection(val blockchainType: BlockchainType, val providerUid: String) : NftEventListType()
    data class Asset(val nftUid: NftUid) : NftEventListType()
}
