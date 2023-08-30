package io.horizontalsystems.bankwallet.modules.receivemain

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

class ReceiveTokenSelectViewModel(
    private val service: BalanceService,
    private val balanceViewItemFactory: BalanceViewItemFactory,
    private val balanceViewTypeManager: BalanceViewTypeManager
) : ViewModel() {
    private var items: List<BalanceViewItem> = listOf()

    var uiState by mutableStateOf(
        ReceiveTokenSelectUiState(
            items = items
        )
    )

    init {
        service.start()

        viewModelScope.launch {
            service.balanceItemsFlow.collect { items ->
                refreshViewItems(items)
            }
        }
    }

    private suspend fun refreshViewItems(balanceItems: List<BalanceModule.BalanceItem>?) {
        withContext(Dispatchers.IO) {
            if (balanceItems != null) {
                items = balanceItems.map { balanceItem ->
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
                items = listOf()
            }

            emitState()
        }
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = ReceiveTokenSelectUiState(
                items = items,
            )
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReceiveTokenSelectViewModel(
                service = BalanceService.getInstance(),
                balanceViewItemFactory = BalanceViewItemFactory(),
                balanceViewTypeManager = App.balanceViewTypeManager
            ) as T
        }
    }
}

data class ReceiveTokenSelectUiState(val items: List<BalanceViewItem>)
