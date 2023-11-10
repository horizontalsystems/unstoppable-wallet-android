package cash.p.terminal.modules.tokenselect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.managers.BalanceHiddenManager
import cash.p.terminal.core.swappable
import cash.p.terminal.modules.balance.BalanceModule
import cash.p.terminal.modules.balance.BalanceService
import cash.p.terminal.modules.balance.BalanceSortType
import cash.p.terminal.modules.balance.BalanceSorter
import cash.p.terminal.modules.balance.BalanceViewItem2
import cash.p.terminal.modules.balance.BalanceViewItemFactory
import cash.p.terminal.modules.balance.BalanceViewTypeManager
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TokenSelectViewModel(
    private val service: BalanceService,
    private val balanceViewItemFactory: BalanceViewItemFactory,
    private val balanceViewTypeManager: BalanceViewTypeManager,
    private val itemsFilter: ((BalanceModule.BalanceItem) -> Boolean)?,
    private val balanceSorter: BalanceSorter,
    private val balanceHiddenManager: BalanceHiddenManager,
    private val blockchainTypes: ArrayList<BlockchainType>?
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
                blockchainTypes?.let { types ->
                    itemsFiltered = itemsFiltered.filter { item ->
                        types.contains(item.wallet.token.blockchainType)
                    }
                }
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
                        hideBalance = balanceHiddenManager.balanceHidden,
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

    class FactoryForSend(private val blockchainTypes: ArrayList<BlockchainType>? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TokenSelectViewModel(
                service = BalanceService.getInstance("wallet"),
                balanceViewItemFactory = BalanceViewItemFactory(),
                balanceViewTypeManager = App.balanceViewTypeManager,
                itemsFilter = null,
                balanceSorter = BalanceSorter(),
                balanceHiddenManager = App.balanceHiddenManager,
                blockchainTypes = blockchainTypes,
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
                balanceSorter = BalanceSorter(),
                balanceHiddenManager = App.balanceHiddenManager,
                blockchainTypes = null,
            ) as T
        }
    }
}

data class TokenSelectUiState(
    val items: List<BalanceViewItem2>,
    val noItems: Boolean,
)
