package io.horizontalsystems.bankwallet.modules.nft.collection.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.nft.EventType
import io.horizontalsystems.bankwallet.ui.compose.Select
import kotlinx.coroutines.launch

class NftCollectionEventsViewModel(
    private val service: NftCollectionActivityService
) : ViewModel() {

    var viewItem by mutableStateOf<ViewItem?>(null)
        private set

    var events by mutableStateOf<List<CollectionEvent>?>(null)
        private set

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    var loadingMore by mutableStateOf(false)
        private set

    var eventTypeSelectorState by mutableStateOf<SelectorDialogState>(SelectorDialogState.Closed)
        private set

    private val eventTypeSelect: Select<EventType>
        get() = Select(
            service.eventType,
            listOf(
                EventType.All,
                EventType.Sale,
                EventType.BidEntered,
                EventType.OfferEntered,
                EventType.Transfer
            )
        )

    init {
        service.items.collectWith(viewModelScope) { result ->
            result.getOrNull()?.let { list ->
                viewItem = ViewItem(eventTypeSelect, list)

                loadingMore = false
            }

            viewState = result.exceptionOrNull()?.let { ViewState.Error(it) } ?: ViewState.Success
        }

        viewModelScope.launch {
            service.start()
        }
    }

    fun onBottomReached() {
        loadingMore = true

        viewModelScope.launch {
            service.loadMore()
        }
    }

    fun onErrorClick() {
        // TODO("not implemented")
    }

    fun onClickEventType() {
        eventTypeSelectorState = SelectorDialogState.Opened(eventTypeSelect)
    }

    fun onSelectEvenType(eventType: EventType) {
        viewModelScope.launch {
            service.setEventType(eventType)
        }
        eventTypeSelectorState = SelectorDialogState.Closed
    }

    fun onDismissEventTypeDialog() {
        eventTypeSelectorState = SelectorDialogState.Closed
    }

}


