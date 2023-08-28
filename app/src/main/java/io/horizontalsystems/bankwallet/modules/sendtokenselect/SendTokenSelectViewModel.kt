package io.horizontalsystems.bankwallet.modules.sendtokenselect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule
import io.horizontalsystems.bankwallet.modules.balance.BalanceService
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItemFactory
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewTypeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SendTokenSelectViewModel(
    private val service: BalanceService,
    private val balanceViewItemFactory: BalanceViewItemFactory,
    private val balanceViewTypeManager: BalanceViewTypeManager,
) : ViewModel() {

    private var balanceViewItems = listOf<BalanceViewItem>()
    private var filteringEnabled = true
    var uiState by mutableStateOf(
        SendTokenSelectUiState(
            items = balanceViewItems,
            filteringEnabled = filteringEnabled,
        )
    )
        private set

    init {
        service.start()

        viewModelScope.launch {
            service.balanceItemsFlow.collect { items ->
                filteringEnabled = !items.isNullOrEmpty()

                refreshViewItems(items)
            }
        }
    }

    private suspend fun refreshViewItems(balanceItems: List<BalanceModule.BalanceItem>?) {
        withContext(Dispatchers.IO) {
            if (balanceItems != null) {
                balanceViewItems = balanceItems.map { balanceItem ->
                    balanceViewItemFactory.viewItem(
                        item = balanceItem,
                        currency = service.baseCurrency,
                        expanded = false,
                        hideBalance = false,
                        watchAccount = service.isWatchAccount,
                        balanceViewType = balanceViewTypeManager.balanceViewTypeFlow.value
                    )
                }
            } else {
                balanceViewItems = listOf()
            }

            emitState()
        }
    }

    fun updateFilter(filter: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val filteredItems = service.balanceItemsFlow.value?.filter {
                val coin = it.wallet.coin
                coin.code.contains(filter, true) || coin.name.contains(filter, true)
            }

            refreshViewItems(filteredItems)
        }
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = SendTokenSelectUiState(
                items = balanceViewItems,
                filteringEnabled = filteringEnabled,
            )
        }
    }

    override fun onCleared() {
        service.clear()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SendTokenSelectViewModel(
                BalanceService.getInstance(),
                BalanceViewItemFactory(),
                App.balanceViewTypeManager
            ) as T
        }
    }
}

data class SendTokenSelectUiState(
    val items: List<BalanceViewItem>,
    val filteringEnabled: Boolean
)
