package io.horizontalsystems.bankwallet.modules.nft.collections

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.BalanceHiddenManager
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.balance.TotalService
import io.horizontalsystems.bankwallet.modules.balance.TotalUIState
import io.horizontalsystems.bankwallet.modules.nft.DataWithError
import io.horizontalsystems.bankwallet.modules.nft.NftCollectionRecord
import io.horizontalsystems.bankwallet.modules.nft.collection.assets.NftCollectionAssetsService.Item
import io.horizontalsystems.bankwallet.modules.nft.viewState
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.launch
import java.net.UnknownHostException

class NftCollectionsViewModel(
    private val service: NftCollectionsService,
    private val totalService: TotalService,
    private val balanceHiddenManager: BalanceHiddenManager
) : ViewModel() {
    val priceType by service::priceType

    var viewState by mutableStateOf<ViewState>(ViewState.Loading)
        private set
    var loading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<TranslatableString?>(null)
        private set
    var collectionViewItems by mutableStateOf<List<NftCollectionViewItem>>(listOf())
        private set
    var totalState by mutableStateOf(createTotalUIState(totalService.stateFlow.value))
        private set

    init {
        viewModelScope.launch {
            service.serviceItemDataFlow
                .collect {
                    handleNftCollections(it)
                }
        }

        viewModelScope.launch {
            totalService.stateFlow.collect {
                totalState = createTotalUIState(it)
            }
        }

        totalService.start()

        viewModelScope.launch {
            service.start()
        }
    }

    private fun handleNftCollections(dataWithError: DataWithError<Map<NftCollectionRecord, List<Item>>?>) {
        val data = dataWithError.value
        val error = dataWithError.error

        dataWithError.viewState?.let {
            viewState = it
        }

        data?.let { syncItems(it) }

        errorMessage = error?.let { errorText(it) }
    }

    private fun errorText(error: Exception): TranslatableString {
        return when (error) {
            is UnknownHostException -> TranslatableString.ResString(R.string.Hud_Text_NoInternet)
            else -> TranslatableString.PlainString(error.message ?: error.javaClass.simpleName)
        }
    }

    private fun syncItems(
        collectionItems: Map<NftCollectionRecord, List<Item>>
    ) {
        val expandedStates = collectionViewItems.associate { it.slug to it.expanded }
        collectionViewItems = collectionItems.map { (collectionRecord, assetItems) ->
            NftCollectionViewItem(
                slug = collectionRecord.uid,
                name = collectionRecord.name,
                imageUrl = collectionRecord.imageUrl,
                assets = assetItems,
                expanded = expandedStates[collectionRecord.uid] ?: false
            )
        }

//        totalService.setItems(
//            collectionItems.map { it.value }.flatten().mapNotNull { asset ->
//                asset.price?.let { price ->
//                    TotalService.BalanceItem(price.coinValue.value, isValuePending = false, coinPrice = asset.xRate)
//                }
//            }
//        )
    }

    private fun createTotalUIState(totalState: TotalService.State) = when (totalState) {
        TotalService.State.Hidden -> TotalUIState.Hidden
        is TotalService.State.Visible -> TotalUIState.Visible(
            currencyValueStr = totalState.currencyValue?.let {
                App.numberFormatter.formatFiatFull(it.value, it.currency.symbol)
            } ?: "---",
            coinValueStr = totalState.coinValue?.let {
                "~" + App.numberFormatter.formatCoinFull(it.value, it.coin.code, it.decimal)
            } ?: "---",
            dimmed = totalState.dimmed
        )
    }

    override fun onCleared() {
        totalService.stop()
        service.stop()
    }

    fun refresh() {
        viewModelScope.launch {
            loading = true
            service.refresh()
            loading = false
        }
    }

    fun toggleCollection(collection: NftCollectionViewItem) {
        val index = collectionViewItems.indexOf(collection)

        if (index != -1) {
            collectionViewItems = collectionViewItems.toMutableList().apply {
                this[index] = collection.copy(expanded = !collection.expanded)
            }
        }
    }

    fun updatePriceType(priceType: PriceType) {
        service.updatePriceType(priceType)
    }

    fun errorShown() {
        errorMessage = null
    }

    fun onBalanceClick() {
        balanceHiddenManager.toggleBalanceHidden()
    }

    fun toggleTotalType() {
        totalService.toggleType()
    }

}
