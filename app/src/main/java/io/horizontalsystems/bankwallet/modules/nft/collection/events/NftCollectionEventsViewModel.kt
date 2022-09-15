package io.horizontalsystems.bankwallet.modules.nft.collection.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.modules.market.overview.coinValue
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.marketkit.models.NftEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class NftCollectionEventsViewModel(
    private val service: NftCollectionEventsService
) : ViewModel() {

    var viewItem by mutableStateOf<ViewItem?>(null)
        private set

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    var loadingMore by mutableStateOf(false)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    var eventTypeSelectorState by mutableStateOf<SelectorDialogState>(SelectorDialogState.Closed)
        private set

    private val eventTypeSelect: Select<NftEventTypeWrapper>
        get() = Select(
            NftEventTypeWrapper(service.eventType),
            listOf(
                NftEventTypeWrapper(NftEvent.EventType.All),
                NftEventTypeWrapper(NftEvent.EventType.Sale),
                NftEventTypeWrapper(NftEvent.EventType.List),
                NftEventTypeWrapper(NftEvent.EventType.OfferEntered),
                NftEventTypeWrapper(NftEvent.EventType.BidEntered),
                NftEventTypeWrapper(NftEvent.EventType.Transfer)
            )
        )

    init {
        service.itemsUpdatedFlow.collectWith(viewModelScope) {
            viewItem = ViewItem(eventTypeSelect, service.items?.getOrNull()?.map { eventViewItem(it) })

            loadingMore = false
            viewState = service.items?.exceptionOrNull()?.let { ViewState.Error(it) } ?: ViewState.Success
        }

        viewModelScope.launch {
            service.start()
        }
    }

    private fun eventViewItem(item: NftCollectionEventsService.Item) =
        EventViewItem(
            providerCollectionUid = item.event.assetMetadata.providerCollectionUid,
            nftUid = item.event.assetMetadata.nftUid,
            date = item.event.date,
            type = item.event.eventType,
            imageUrl = item.event.assetMetadata.imageUrl,
            price = item.event.amount?.coinValue,
            priceInFiat = item.priceInFiat
        )

    fun onBottomReached() {
        loadingMore = !isRefreshing

        viewModelScope.launch {
            service.loadMore()
        }
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    private fun refreshWithMinLoadingSpinnerPeriod() {
        viewModelScope.launch {
            isRefreshing = true

            service.refresh()

            delay(1000)
            isRefreshing = false
        }
    }

    fun onClickEventType() {
        eventTypeSelectorState = SelectorDialogState.Opened(eventTypeSelect)
    }

    fun onSelectEvenType(eventTypeWrapper: NftEventTypeWrapper) {
        viewModelScope.launch {
            service.setEventType(eventTypeWrapper.eventType)
        }
        eventTypeSelectorState = SelectorDialogState.Closed
    }

    fun onDismissEventTypeDialog() {
        eventTypeSelectorState = SelectorDialogState.Closed
    }

    data class ViewItem(
        val eventTypeSelect: Select<NftEventTypeWrapper>,
        val events: List<EventViewItem>?
    )

    data class EventViewItem(
        val providerCollectionUid: String,
        val nftUid: NftUid,
        val type: NftEvent.EventType,
        val date: Date?,
        val imageUrl: String?,
        val price: CoinValue?,
        val priceInFiat: CurrencyValue?
    )
}

data class NftEventTypeWrapper(
    val eventType: NftEvent.EventType
) : WithTranslatableTitle {

    override val title: TranslatableString
        get() = title(eventType)

    companion object {
        fun title(eventType: NftEvent.EventType): TranslatableString {
            val titleResId = when (eventType) {
                NftEvent.EventType.All -> R.string.NftCollection_EventType_All
                NftEvent.EventType.List -> R.string.NftCollection_EventType_List
                NftEvent.EventType.Sale -> R.string.NftCollection_EventType_Sale
                NftEvent.EventType.OfferEntered -> R.string.NftCollection_EventType_OfferEntered
                NftEvent.EventType.BidEntered -> R.string.NftCollection_EventType_BidEntered
                NftEvent.EventType.BidWithdrawn -> R.string.NftCollection_EventType_BidWithdrawn
                NftEvent.EventType.Transfer -> R.string.NftCollection_EventType_Transfer
                NftEvent.EventType.Approve -> R.string.NftCollection_EventType_Approve
                NftEvent.EventType.Custom -> R.string.NftCollection_EventType_Custom
                NftEvent.EventType.Payout -> R.string.NftCollection_EventType_Payout
                NftEvent.EventType.Cancel -> R.string.NftCollection_EventType_Cancelled
                NftEvent.EventType.BulkCancel -> R.string.NftCollection_EventType_BulkCancel
                NftEvent.EventType.Unknown -> R.string.NftCollection_EventType_Unknown
            }

            return TranslatableString.ResString(titleResId)
        }
    }

}

