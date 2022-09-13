package io.horizontalsystems.bankwallet.modules.nft.collection.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.marketkit.models.NftEvent

class NftCollectionEventsModule {

    class Factory(private val eventListType: NftEventListType, private val defaultEventType: NftEvent.EventType = NftEvent.EventType.Sale) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val nftProvider = when (eventListType) {
                is NftEventListType.Asset -> {
                    App.nftMetadataManager.provider(eventListType.nftUid.blockchainType)
                }
                is NftEventListType.Collection -> TODO()
            }

            val service = NftCollectionEventsService(
                eventListType,
                defaultEventType,
                nftProvider,
                App.marketKit,
                BalanceXRateRepository(App.currencyManager, App.marketKit)
            )
            return NftCollectionEventsViewModel(service) as T
        }
    }

}

data class ViewItem(
    val eventTypeSelect: Select<NftEventTypeWrapper>,
    val events: List<CollectionEvent>?
)

sealed class SelectorDialogState {
    object Closed : SelectorDialogState()
    class Opened(val select: Select<NftEventTypeWrapper>) : SelectorDialogState()
}

sealed class NftEventListType {
    data class Collection(val uid: String): NftEventListType()
    data class Asset(val nftUid: NftUid): NftEventListType()
}
