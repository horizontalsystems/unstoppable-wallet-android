package io.horizontalsystems.bankwallet.modules.tokenselect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.swappable
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule
import io.horizontalsystems.bankwallet.modules.balance.BalanceService
import io.horizontalsystems.bankwallet.modules.balance.BalanceSortType
import io.horizontalsystems.bankwallet.modules.balance.BalanceSorter
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem2
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItemFactory
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewTypeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TokenSelectViewModel(
    private val service: BalanceService,
    private val balanceViewItemFactory: BalanceViewItemFactory,
    private val balanceViewTypeManager: BalanceViewTypeManager,
    private val itemsFilter: ((BalanceModule.BalanceItem) -> Boolean)?,
    private val balanceSorter: BalanceSorter
) : ViewModel() {

    private var noItems = false
    private var query: String? = null
    private var balanceViewItems = listOf<BalanceViewItem2>()
    var uiState by mutableStateOf(
        TokenSelectUiState(
            items = balanceViewItems,
            noItems = noItems
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
                var itemsFiltered: List<BalanceModule.BalanceItem> = balanceItems
                itemsFilter?.let {
                    itemsFiltered = itemsFiltered.filter(it)
                }
                noItems = itemsFiltered.isEmpty()

                val tmpQuery = query
                if (!tmpQuery.isNullOrBlank()) {
                    itemsFiltered = itemsFiltered.filter {
                        val coin = it.wallet.coin
                        coin.code.contains(tmpQuery, true) || coin.name.contains(tmpQuery, true)
                    }
                }

                val itemsSorted = balanceSorter.sort(itemsFiltered, BalanceSortType.Value)
                balanceViewItems = itemsSorted.map { balanceItem ->
                    balanceViewItemFactory.viewItem2(
                        item = balanceItem,
                        currency = service.baseCurrency,
                        hideBalance = false,
                        watchAccount = service.isWatchAccount,
                        balanceViewType = balanceViewTypeManager.balanceViewTypeFlow.value,
                        networkAvailable = service.networkAvailable
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
                noItems = noItems,
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
                service = BalanceService.getInstance("wallet"),
                balanceViewItemFactory = BalanceViewItemFactory(),
                balanceViewTypeManager = App.balanceViewTypeManager,
                itemsFilter = null,
                balanceSorter = BalanceSorter()
            ) as T
        }
    }

    class FactoryForSwap : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TokenSelectViewModel(
                service = BalanceService.getInstance("wallet"),
                balanceViewItemFactory = BalanceViewItemFactory(),
                balanceViewTypeManager = App.balanceViewTypeManager,
                itemsFilter = {
                    it.wallet.token.swappable
                },
                balanceSorter = BalanceSorter()
            ) as T
        }
    }
}

data class TokenSelectUiState(
    val items: List<BalanceViewItem2>,
    val noItems: Boolean,
)
