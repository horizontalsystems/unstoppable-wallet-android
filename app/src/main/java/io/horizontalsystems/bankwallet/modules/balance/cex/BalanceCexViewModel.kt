package io.horizontalsystems.bankwallet.modules.balance.cex

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.managers.ActiveAccountState
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CexType
import io.horizontalsystems.bankwallet.modules.balance.*
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.CoinPrice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.collect
import java.math.BigDecimal

class BalanceCexViewModel(
    private val totalBalance: TotalBalance,
    private val localStorage: ILocalStorage,
    private val balanceViewTypeManager: BalanceViewTypeManager,
    private val xRateRepository: BalanceXRateRepository,
    private val balanceCexSorter: BalanceCexSorter,
    private val accountManager: IAccountManager,
) : ViewModel(), ITotalBalance by totalBalance {

    private var balanceCexRepository: IBalanceCexRepository? = null
    private var balanceViewType = balanceViewTypeManager.balanceViewTypeFlow.value

    val sortTypes =
        listOf(BalanceSortType.Value, BalanceSortType.Name, BalanceSortType.PercentGrowth)
    var sortType by mutableStateOf(localStorage.sortType)
        private set

    private val currency by xRateRepository::baseCurrency

    private var expandedItemId: String? = null
    private var isRefreshing = false
    private var viewItems = mutableListOf<BalanceCexViewItem>()

    var uiState by mutableStateOf(
        UiState(
            isRefreshing = isRefreshing,
            viewItems = viewItems
        )
    )
        private set

    private var collectCexRepoItemsJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            accountManager.activeAccountStateFlow.collect {
                handleActiveAccount(it)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            balanceViewTypeManager.balanceViewTypeFlow.collect {
                handleUpdatedBalanceViewType(it)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            xRateRepository.itemObservable.collect {
                handleXRateUpdate(it)
            }
        }

        totalBalance.start(viewModelScope)
    }

    private fun handleActiveAccount(activeAccount: ActiveAccountState) {
        collectCexRepoItemsJob?.cancel()
        balanceCexRepository?.stop()
        viewItems.clear()
        totalBalance.setTotalServiceItems(listOf())

        val accountType = (activeAccount as? ActiveAccountState.ActiveAccount)
            ?.account
            ?.type

        val cexType = (accountType as? AccountType.Cex)?.cexType
        balanceCexRepository = when (cexType) {
            is CexType.Coinzix -> CoinzixBalanceCexRepository()
            is CexType.Binance -> TODO()
            null -> null
        }

        balanceCexRepository?.let { repo ->
            collectCexRepoItemsJob = viewModelScope.launch(Dispatchers.IO) {
                repo.itemsFlow.collect {
                    handleUpdatedItems(it)
                }
            }

            repo.start()
        }

        emitState()
    }

    private fun handleXRateUpdate(latestRates: Map<String, CoinPrice?>) {
        for (i in 0 until viewItems.size) {
            val balanceItem = viewItems[i]

            if (latestRates.containsKey(balanceItem.coinUid)) {
                viewItems[i] = createBalanceCexViewItem(viewItems[i].balanceCexItem, latestRates[balanceItem.coinUid])
            }
        }

        if (sortType is BalanceSortType.Value || sortType is BalanceSortType.PercentGrowth) {
            sortItems()
        }

        refreshTotalBalance()
        emitState()
    }


    private fun handleUpdatedBalanceViewType(balanceViewType: BalanceViewType) {
        this.balanceViewType = balanceViewType
        refreshViewItems()

        emitState()
    }

    private fun refreshViewItems() {
        val latestRates = xRateRepository.getLatestRates()

        viewItems.replaceAll {
            createBalanceCexViewItem(it.balanceCexItem, latestRates[it.balanceCexItem.coin.uid])
        }
    }

    private fun handleUpdatedItems(items: List<BalanceCexItem>) {
        xRateRepository.setCoinUids(items.map { it.coin.uid })
        val latestRates = xRateRepository.getLatestRates()

        viewItems = items.map {
            createBalanceCexViewItem(it, latestRates[it.coin.uid])
        }.toMutableList()

        sortItems()
        refreshTotalBalance()
        emitState()
    }

    private fun refreshTotalBalance() {
        val totalServiceItems = viewItems.map {
            TotalService.BalanceItem(
                it.balanceCexItem.balance,
                false,
                it.coinPrice
            )

        }
        totalBalance.setTotalServiceItems(totalServiceItems)
    }

    private fun createBalanceCexViewItem(
        balanceCexItem: BalanceCexItem,
        latestRate: CoinPrice?
    ): BalanceCexViewItem {
        val expanded = balanceCexItem.id == expandedItemId
        val (primaryValue, secondaryValue) = BalanceViewHelper.getPrimaryAndSecondaryValues(
            balance = balanceCexItem.balance,
            visible = !balanceHidden,
            fullFormat = expanded,
            coinDecimals = balanceCexItem.decimals,
            dimmed = false,
            coinPrice = latestRate,
            currency = currency,
            balanceViewType = balanceViewType
        )

        return BalanceCexViewItem(
            coinIconUrl = balanceCexItem.coin.imageUrl,
            coinIconPlaceholder = R.drawable.coin_placeholder,
            coinCode = balanceCexItem.coin.code,
            badge = null,
            primaryValue = primaryValue,
            exchangeValue = BalanceViewHelper.rateValue(latestRate, currency, true),
            diff = latestRate?.diff,
            secondaryValue = secondaryValue,
            expanded = expanded,
            hasCoinInfo = true,
            coinUid = balanceCexItem.coin.uid,
            id = balanceCexItem.id,
            balanceCexItem = balanceCexItem,
            coinPrice = latestRate
        )
    }

    private fun sortItems() {
        viewItems = balanceCexSorter.sort(viewItems, sortType).toMutableList()
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = UiState(
                isRefreshing = isRefreshing,
                viewItems = viewItems
            )
        }
    }

    override fun onCleared() {
        totalBalance.stop()
    }

    override fun toggleBalanceVisibility() {
        totalBalance.toggleBalanceVisibility()
        refreshViewItems()

        emitState()
    }

    fun onSelectSortType(sortType: BalanceSortType) {
        this.sortType = sortType
        localStorage.sortType = sortType

        sortItems()
        emitState()
    }

    fun onRefresh() {
        if (isRefreshing) {
            return
        }

        viewModelScope.launch {
            isRefreshing = true
            emitState()

            xRateRepository.refresh()
            // A fake 2 seconds 'refresh'
            delay(2300)

            isRefreshing = false
            emitState()
        }
    }

    fun onClickItem(viewItem: BalanceCexViewItem) {
        expandedItemId = when {
            viewItem.id == expandedItemId -> null
            else -> viewItem.id
        }

        viewItems = viewItems.map {
            it.copy(expanded = it.id == expandedItemId)
        }.toMutableList()

        emitState()
    }
}

data class BalanceCexItem(
    val balance: BigDecimal,
    val coin: Coin,
    val decimals: Int
) {
    val id: String = coin.uid
}

data class UiState(val isRefreshing: Boolean, val viewItems: List<BalanceCexViewItem>)

data class BalanceCexViewItem(
    val coinIconUrl: String,
    val coinIconPlaceholder: Int,
    val coinCode: String,
    val badge: String?,
    val primaryValue: DeemedValue<String>,
    val exchangeValue: DeemedValue<String>,
    val diff: BigDecimal?,
    val secondaryValue: DeemedValue<String>,
    val expanded: Boolean,
    val hasCoinInfo: Boolean,
    val coinUid: String,
    val id: String,
    val balanceCexItem: BalanceCexItem,
    val coinPrice: CoinPrice?
) {
    val fiatValue by lazy { coinPrice?.value?.let { balanceCexItem.balance.times(it) } }
}
