package cash.p.terminal.modules.tokenselect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.managers.BalanceHiddenManager
import cash.p.terminal.wallet.balance.BalanceItem
import cash.p.terminal.modules.balance.DefaultBalanceService
import cash.p.terminal.wallet.BalanceSortType
import cash.p.terminal.modules.balance.BalanceSorter
import cash.p.terminal.modules.balance.BalanceViewItem2
import cash.p.terminal.modules.balance.BalanceViewItemFactory
import cash.p.terminal.modules.balance.BalanceViewTypeManager
import cash.p.terminal.modules.balance.ITotalBalance
import cash.p.terminal.modules.balance.TotalBalance
import cash.p.terminal.modules.balance.TotalService
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import kotlin.getValue

class TokenSelectViewModel(
    private val service: DefaultBalanceService,
    private val balanceViewItemFactory: BalanceViewItemFactory,
    private val balanceViewTypeManager: BalanceViewTypeManager,
    private val itemsFilter: ((BalanceItem) -> Boolean)?,
    private val balanceSorter: BalanceSorter,
    private val balanceHiddenManager: BalanceHiddenManager,
    private val blockchainTypes: List<BlockchainType>?,
    private val tokenTypes: List<TokenType>?,
    private val totalBalance: TotalBalance,
) : ViewModel(), ITotalBalance by totalBalance {

    private var noItems = false
    private var query: String? = null
    private var balanceViewItems = listOf<BalanceViewItem2>()
    private val itemsBalanceHidden by lazy { mutableMapOf<Wallet, Boolean>() }

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

    fun onBalanceClick(item: BalanceViewItem2) {
        if (balanceHidden) {
            HudHelper.vibrate(App.instance)
            itemsBalanceHidden[item.wallet] = !itemsBalanceHidden.getOrDefault(item.wallet, true)
            viewModelScope.launch {
                refreshViewItems(service.balanceItemsFlow.value)
            }
        }
    }

    private suspend fun refreshViewItems(balanceItems: List<BalanceItem>?) {
        withContext(Dispatchers.IO) {
            if (balanceItems != null) {
                var itemsFiltered: List<BalanceItem> = balanceItems.filter { it.balanceData.available > BigDecimal.ZERO }
                blockchainTypes?.let { types ->
                    itemsFiltered = itemsFiltered.filter { item ->
                        types.contains(item.wallet.token.blockchainType)
                    }
                }
                tokenTypes?.let { types ->
                    itemsFiltered = itemsFiltered.filter { item ->
                        types.contains(item.wallet.token.type)
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

                val itemsSorted = balanceSorter.sort(
                    items = itemsFiltered,
                    sortType = BalanceSortType.Value
                )
                balanceViewItems = itemsSorted.map { balanceItem ->
                    balanceViewItemFactory.viewItem2(
                        item = balanceItem,
                        currency = service.baseCurrency,
                        hideBalance = balanceHiddenManager.balanceHidden &&
                                itemsBalanceHidden.getOrDefault(balanceItem.wallet, true),
                        watchAccount = service.isWatchAccount,
                        isSwipeToDeleteEnabled = true,
                        balanceViewType = balanceViewTypeManager.balanceViewTypeFlow.value,
                        networkAvailable = service.networkAvailable,
                        showStackingUnpaid = false
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

    class FactoryForSend(
        private val blockchainTypes: List<BlockchainType>? = null,
        private val tokenTypes: List<TokenType>? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val totalService = TotalService(
                currencyManager = App.currencyManager,
                marketKit = App.marketKit,
                baseTokenManager = App.baseTokenManager,
                balanceHiddenManager = App.balanceHiddenManager
            )

            return TokenSelectViewModel(
                service = DefaultBalanceService.getInstance("wallet"),
                balanceViewItemFactory = BalanceViewItemFactory(),
                balanceViewTypeManager = App.balanceViewTypeManager,
                itemsFilter = {
                    !it.wallet.account.isWatchAccount
                },
                balanceSorter = BalanceSorter(),
                balanceHiddenManager = App.balanceHiddenManager,
                blockchainTypes = blockchainTypes,
                tokenTypes = tokenTypes,
                totalBalance = TotalBalance(totalService, App.balanceHiddenManager),
            ) as T
        }
    }
}

data class TokenSelectUiState(
    val items: List<BalanceViewItem2>,
    val noItems: Boolean,
)
