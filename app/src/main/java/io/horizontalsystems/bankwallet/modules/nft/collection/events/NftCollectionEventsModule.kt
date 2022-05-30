package io.horizontalsystems.bankwallet.modules.nft.collection.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.modules.hsnft.HsNftApiProvider
import io.horizontalsystems.bankwallet.modules.nft.EventType
import io.horizontalsystems.bankwallet.ui.compose.Select

class NftCollectionEventsModule {

    class Factory(private val collectionUid: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = NftCollectionEventsService(
                collectionUid,
                HsNftApiProvider(),
                App.nftManager,
                BalanceXRateRepository(App.currencyManager, App.marketKit)
            )
            return NftCollectionEventsViewModel(service) as T
        }
    }

}

data class ViewItem(
    val eventTypeSelect: Select<EventType>,
    val events: List<CollectionEvent>
)

sealed class SelectorDialogState {
    object Closed : SelectorDialogState()
    class Opened(val select: Select<EventType>) : SelectorDialogState()
}
