package cash.p.terminal.modules.balance.cex

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.imageUrl
import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.core.providers.CexProviderManager
import cash.p.terminal.core.providers.ICexProvider
import cash.p.terminal.modules.balance.*
import io.horizontalsystems.marketkit.models.CoinPrice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.collect
import java.math.BigDecimal

class BalanceCexViewModel(
    private val totalBalance: TotalBalance,
    private val localStorage: ILocalStorage,
    private val balanceViewTypeManager: BalanceViewTypeManager,
    private val balanceCexRepository: BalanceCexRepositoryWrapper,
    private val xRateRepository: BalanceXRateRepository,
    private val balanceCexSorter: BalanceCexSorter,
    private val cexProviderManager: CexProviderManager,
) : ViewModel(), ITotalBalance by totalBalance {

    private var balanceViewType = balanceViewTypeManager.balanceViewTypeFlow.value

    val sortTypes =
        listOf(BalanceSortType.Value, BalanceSortType.Name, BalanceSortType.PercentGrowth)
    private var sortType = localStorage.sortType

    private val currency by xRateRepository::baseCurrency

    private var expandedItemId: String? = null
    private var isRefreshing = false
    private var viewItems = mutableListOf<BalanceCexViewItem>()
    private var isActiveScreen = false
    private var withdrawEnabled = false

    var uiState by mutableStateOf(
        UiState(
            isRefreshing = isRefreshing,
            viewItems = viewItems,
            sortType = sortType,
            isActiveScreen = isActiveScreen,
            withdrawEnabled = withdrawEnabled,
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

        viewModelScope.launch(Dispatchers.IO) {
            cexProviderManager.cexProviderFlow.collect {
                handleCexProvider(it)
            }
        }

        viewModelScope.launch {
            totalBalance.stateFlow.collect {
                refreshViewItems()
                emitState()
            }
        }

        totalBalance.start(viewModelScope)
        balanceCexRepository.start()
    }

    private fun handleCexProvider(cexProvider: ICexProvider?) {
        balanceCexRepository.setCexProvider(cexProvider)
    }

    private fun handleXRateUpdate(latestRates: Map<String, CoinPrice?>) {
        for (i in 0 until viewItems.size) {
            val viewItem = viewItems[i]
            viewItem.coinUid?.let { coinUid ->
                if (latestRates.containsKey(coinUid)) {
                    viewItems[i] = createBalanceCexViewItem(
                        cexAsset = viewItem.cexAsset,
                        latestRate = latestRates[coinUid]
                    )
                }
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

        viewItems.replaceAll { viewItem ->
            createBalanceCexViewItem(viewItem.cexAsset, viewItem.coinUid?.let { latestRates[it] })
        }
    }

    private fun handleUpdatedItems(items: List<CexAsset>?) {
        if (items != null) {
            isActiveScreen = true

            xRateRepository.setCoinUids(items.mapNotNull { it.coin?.uid })
            val latestRates = xRateRepository.getLatestRates()

            viewItems = items.map { cexAsset ->
                createBalanceCexViewItem(cexAsset, cexAsset.coin?.let { latestRates[it.uid] })
            }.toMutableList()

            sortItems()
        } else {
            isActiveScreen = false

            xRateRepository.setCoinUids(listOf())

            viewItems.clear()
        }

        withdrawEnabled = items?.any { it.withdrawEnabled } ?: false

        refreshTotalBalance()
        emitState()
    }

    private fun refreshTotalBalance() {
        val totalServiceItems = viewItems.map {
            TotalService.BalanceItem(
                it.cexAsset.freeBalance,
                false,
                it.coinPrice
            )

        }
        totalBalance.setTotalServiceItems(totalServiceItems)
    }

    private fun createBalanceCexViewItem(
        cexAsset: CexAsset,
        latestRate: CoinPrice?
    ): BalanceCexViewItem {
        val expanded = cexAsset.id == expandedItemId
        val (primaryValue, secondaryValue) = BalanceViewHelper.getPrimaryAndSecondaryValues(
            balance = cexAsset.freeBalance,
            visible = !balanceHidden,
            fullFormat = expanded,
            coinDecimals = cexAsset.decimals,
            dimmed = false,
            coinPrice = latestRate,
            currency = currency,
            balanceViewType = balanceViewType
        )

        return BalanceCexViewItem(
            coinIconUrl = cexAsset.coin?.imageUrl,
            coinIconPlaceholder = R.drawable.coin_placeholder,
            coinCode = cexAsset.id,
            badge = null,
            primaryValue = primaryValue,
            exchangeValue = BalanceViewHelper.rateValue(latestRate, currency, true),
            diff = latestRate?.diff,
            secondaryValue = secondaryValue,
            expanded = expanded,
            coinUid = cexAsset.coin?.uid,
            assetId = cexAsset.id,
            cexAsset = cexAsset,
            coinPrice = latestRate,
            depositEnabled = cexAsset.depositEnabled,
            withdrawEnabled = cexAsset.withdrawEnabled,
        )
    }

    private fun sortItems() {
        viewItems = balanceCexSorter.sort(viewItems, sortType).toMutableList()
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = UiState(
                isRefreshing = isRefreshing,
                viewItems = viewItems,
                sortType = sortType,
                isActiveScreen = isActiveScreen,
                withdrawEnabled = withdrawEnabled,
            )
        }
    }

    override fun onCleared() {
        totalBalance.stop()
        balanceCexRepository.stop()
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

            balanceCexRepository.refresh()
            xRateRepository.refresh()
            // A fake 2 seconds 'refresh'
            delay(2300)

            isRefreshing = false
            emitState()
        }
    }

    fun onClickItem(viewItem: BalanceCexViewItem) {
        expandedItemId = when (viewItem.assetId) {
            expandedItemId -> null
            else -> viewItem.assetId
        }

        viewItems = viewItems.map {
            it.copy(expanded = it.assetId == expandedItemId)
        }.toMutableList()

        emitState()
    }
}

data class UiState(
    val isRefreshing: Boolean,
    val viewItems: List<BalanceCexViewItem>,
    val sortType: BalanceSortType,
    val isActiveScreen: Boolean,
    val withdrawEnabled: Boolean
)

data class BalanceCexViewItem(
    val coinIconUrl: String?,
    val coinIconPlaceholder: Int,
    val coinCode: String,
    val badge: String?,
    val primaryValue: DeemedValue<String>,
    val exchangeValue: DeemedValue<String>,
    val diff: BigDecimal?,
    val secondaryValue: DeemedValue<String>,
    val expanded: Boolean,
    val coinUid: String?,
    val assetId: String,
    val coinPrice: CoinPrice?,
    val cexAsset: CexAsset,
    val depositEnabled: Boolean,
    val withdrawEnabled: Boolean
) {
    val fiatValue by lazy { coinPrice?.value?.let { cexAsset.freeBalance.times(it) } }
}
