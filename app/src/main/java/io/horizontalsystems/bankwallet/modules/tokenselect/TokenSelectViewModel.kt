package cash.p.terminal.modules.tokenselect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.swappable
import cash.p.terminal.modules.balance.BalanceModule
import cash.p.terminal.modules.balance.BalanceService
import cash.p.terminal.modules.balance.BalanceViewItem
import cash.p.terminal.modules.balance.BalanceViewItemFactory
import cash.p.terminal.modules.balance.BalanceViewTypeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TokenSelectViewModel(
    private val service: BalanceService,
    private val balanceViewItemFactory: BalanceViewItemFactory,
    private val balanceViewTypeManager: BalanceViewTypeManager,
    private val itemsFilter: ((BalanceModule.BalanceItem) -> Boolean)?
) : ViewModel() {

    private var query: String? = null
    private var balanceViewItems = listOf<BalanceViewItem>()
    var uiState by mutableStateOf(
        TokenSelectUiState(
            items = balanceViewItems,
        )
    )
        private set

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
                val filters: List<(BalanceModule.BalanceItem) -> Boolean> = buildList {
                    itemsFilter?.let { add(it) }

                    val tmpQuery = query
                    if (!tmpQuery.isNullOrBlank()) {
                        add {
                            val coin = it.wallet.coin
                            coin.code.contains(tmpQuery, true) || coin.name.contains(tmpQuery, true)
                        }
                    }
                }

                val filteredItems = if (filters.isNotEmpty()) {
                    balanceItems.filter { item ->
                        filters.all { it.invoke(item) }
                    }
                } else {
                    balanceItems
                }

                balanceViewItems = filteredItems.map { balanceItem ->
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

    fun updateFilter(q: String) {
        viewModelScope.launch {
            query = q
            refreshViewItems(service.balanceItemsFlow.value)
        }
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = TokenSelectUiState(
                items = balanceViewItems,
            )
        }
    }

    override fun onCleared() {
        service.clear()
    }

    class FactoryForSend : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TokenSelectViewModel(
                service = BalanceService.getInstance(),
                balanceViewItemFactory = BalanceViewItemFactory(),
                balanceViewTypeManager = App.balanceViewTypeManager,
                itemsFilter = null
            ) as T
        }
    }

    class FactoryForSwap : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TokenSelectViewModel(
                service = BalanceService.getInstance(),
                balanceViewItemFactory = BalanceViewItemFactory(),
                balanceViewTypeManager = App.balanceViewTypeManager,
                itemsFilter = {
                    it.wallet.token.swappable
                }
            ) as T
        }
    }
}

data class TokenSelectUiState(
    val items: List<BalanceViewItem>,
)
