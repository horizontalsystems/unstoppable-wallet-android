package cash.p.terminal.modules.balance.cex

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.imageUrl
import cash.p.terminal.modules.balance.*
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.CoinPrice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.collect
import java.math.BigDecimal

class BalanceCexViewModel(
    private val totalBalance: TotalBalance,
    private val localStorage: ILocalStorage,
    private val balanceViewTypeManager: BalanceViewTypeManager,
    private val balanceCexRepository: IBalanceCexRepository,
    private val xRateRepository: BalanceXRateRepository,
    private val balanceCexSorter: BalanceCexSorter,
) : ViewModel(), ITotalBalance by totalBalance {

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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            balanceCexRepository.itemsFlow.collect {
                handleUpdatedItems(it)
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
        balanceCexRepository.start()
    }

    private fun handleXRateUpdate(latestRates: Map<String, CoinPrice?>) {
        refreshTotalBalance(xRateRepository.getLatestRates())

        for (i in 0 until viewItems.size) {
            val balanceItem = viewItems[i]

            if (latestRates.containsKey(balanceItem.coinUid)) {
                viewItems[i] = createBalanceCexViewItem(viewItems[i].balanceCexItem, latestRates[balanceItem.coinUid])
            }
        }

        if (sortType is BalanceSortType.Value || sortType is BalanceSortType.PercentGrowth) {
            sortItems()
        }

        emitItems()
    }


    private fun handleUpdatedBalanceViewType(balanceViewType: BalanceViewType) {
        this.balanceViewType = balanceViewType
        refreshViewItems()

        emitItems()
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

        refreshTotalBalance(latestRates)

        viewItems = items.map {
            createBalanceCexViewItem(it, latestRates[it.coin.uid])
        }.toMutableList()

        sortItems()
        emitItems()
    }

    private fun refreshTotalBalance(latestRates: Map<String, CoinPrice?>) {
        val totalServiceItems = viewItems.map {
            val balanceCexItem = it.balanceCexItem
            val latestRate = latestRates[balanceCexItem.coin.uid]
            TotalService.BalanceItem(
                balanceCexItem.balance,
                false,
                latestRate
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

    private fun emitItems() {
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

        emitItems()
    }

    fun onSelectSortType(sortType: BalanceSortType) {
        this.sortType = sortType
        localStorage.sortType = sortType

        sortItems()
        emitItems()
    }

    fun onRefresh() {
        xRateRepository.refresh()
    }

    fun onClickItem(viewItem: BalanceCexViewItem) {
        expandedItemId = when {
            viewItem.id == expandedItemId -> null
            else -> viewItem.id
        }

        viewItems = viewItems.map {
            it.copy(expanded = it.id == expandedItemId)
        }.toMutableList()

        emitItems()
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
    val fiatValue get() = coinPrice?.value?.let { balanceCexItem.balance.times(it) }

}
