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
import io.horizontalsystems.bankwallet.entities.nft.NftEventMetadata.EventType
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.modules.coin.ContractInfo
import io.horizontalsystems.bankwallet.modules.market.overview.coinValue
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.SelectOptional
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class NftCollectionEventsViewModel(
    private val service: NftCollectionEventsService
) : ViewModel() {

    var events by mutableStateOf<List<EventViewItem>?>(null)
        private set

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set

    var loadingMore by mutableStateOf(false)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    val eventTypeSelect: Select<NftEventTypeWrapper>
        get() = Select(
            NftEventTypeWrapper(service.eventType),
            listOf(
                NftEventTypeWrapper(EventType.All),
                NftEventTypeWrapper(EventType.Sale),
                NftEventTypeWrapper(EventType.List),
                NftEventTypeWrapper(EventType.BidEntered),
                NftEventTypeWrapper(EventType.BidWithdrawn),
                NftEventTypeWrapper(EventType.Transfer),
                NftEventTypeWrapper(EventType.Mint),
                NftEventTypeWrapper(EventType.Cancel),
            )
        )

    val contractSelect: SelectOptional<ContractInfo>?
        get() = if (service.contracts.size > 1) SelectOptional(service.contract, service.contracts) else null

    init {
        service.itemsUpdatedFlow.collectWith(viewModelScope) {
            events = service.items?.getOrNull()?.map { eventViewItem(it) }

            loadingMore = false
            viewState = service.items?.exceptionOrNull()?.let { ViewState.Error(it) } ?: ViewState.Success
        }

        viewModelScope.launch {
            service.start()
        }
    }

    private fun eventViewItem(item: NftCollectionEventsService.Item): EventViewItem {
        val type = item.event.eventType ?: EventType.Unknown

        val price: CoinValue?
        val priceInFiat: CurrencyValue?
        when (type) {
            EventType.Transfer,
            EventType.Mint -> {
                price = null
                priceInFiat = null
            }
            else -> {
                price = item.event.amount?.coinValue
                priceInFiat = item.priceInFiat
            }
        }

        return EventViewItem(
            providerCollectionUid = item.event.assetMetadata.providerCollectionUid,
            nftUid = item.event.assetMetadata.nftUid,
            date = item.event.date,
            type = item.event.eventType ?: EventType.Unknown,
            imageUrl = item.event.assetMetadata.imageUrl,
            price = price,
            priceInFiat = priceInFiat
        )
    }

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

    fun onSelect(eventTypeWrapper: NftEventTypeWrapper) {
        viewModelScope.launch {
            service.setEventType(eventTypeWrapper.eventType)
        }
    }

    fun onSelect(contract: ContractInfo) {
        viewModelScope.launch {
            service.setContract(contract)
        }
    }

    data class ViewItem(
        val events: List<EventViewItem>?
    )

    data class EventViewItem(
        val providerCollectionUid: String,
        val nftUid: NftUid,
        val type: EventType,
        val date: Date?,
        val imageUrl: String?,
        val price: CoinValue?,
        val priceInFiat: CurrencyValue?
    )
}

data class NftEventTypeWrapper(
    val eventType: EventType
) : WithTranslatableTitle {

    override val title: TranslatableString
        get() = title(eventType)

    companion object {
        fun title(eventType: EventType): TranslatableString {
            val titleResId = when (eventType) {
                EventType.All -> R.string.NftCollection_EventType_All
                EventType.List -> R.string.NftCollection_EventType_List
                EventType.Sale -> R.string.NftCollection_EventType_Sale
                EventType.OfferEntered -> R.string.NftCollection_EventType_OfferEntered
                EventType.BidEntered -> R.string.NftCollection_EventType_BidEntered
                EventType.BidWithdrawn -> R.string.NftCollection_EventType_BidWithdrawn
                EventType.Transfer -> R.string.NftCollection_EventType_Transfer
                EventType.Approve -> R.string.NftCollection_EventType_Approve
                EventType.Custom -> R.string.NftCollection_EventType_Custom
                EventType.Payout -> R.string.NftCollection_EventType_Payout
                EventType.Cancel -> R.string.NftCollection_EventType_Cancelled
                EventType.BulkCancel -> R.string.NftCollection_EventType_BulkCancel
                EventType.Mint -> R.string.NftCollection_EventType_Mint
                EventType.Unknown -> R.string.NftCollection_EventType_Unknown
            }

            return TranslatableString.ResString(titleResId)
        }
    }

}

