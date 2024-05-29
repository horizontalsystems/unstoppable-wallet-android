package io.horizontalsystems.bankwallet.modules.balance.cex

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.providers.CexAsset
import io.horizontalsystems.bankwallet.core.providers.CexProviderManager
import io.horizontalsystems.bankwallet.core.providers.ICexProvider
import io.horizontalsystems.bankwallet.modules.balance.BalanceSortType
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItemFactory
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewType
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewTypeManager
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository
import io.horizontalsystems.bankwallet.modules.balance.DeemedValue
import io.horizontalsystems.bankwallet.modules.balance.ITotalBalance
import io.horizontalsystems.bankwallet.modules.balance.SyncingProgress
import io.horizontalsystems.bankwallet.modules.balance.TotalBalance
import io.horizontalsystems.bankwallet.modules.balance.TotalService
import io.horizontalsystems.marketkit.models.Coin
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
    private val balanceViewItemFactory: BalanceViewItemFactory,
    private val balanceCexRepository: BalanceCexRepositoryWrapper,
    private val xRateRepository: BalanceXRateRepository,
    private val balanceCexSorter: BalanceCexSorter,
    private val cexProviderManager: CexProviderManager,
) : ViewModelUiState<UiState>(), ITotalBalance by totalBalance {

    private var balanceViewType = balanceViewTypeManager.balanceViewTypeFlow.value

    val sortTypes = listOf(BalanceSortType.Value, BalanceSortType.Name, BalanceSortType.PercentGrowth)
    private var sortType = localStorage.sortType

    private val currency by xRateRepository::baseCurrency

    private var isRefreshing = false
    private var viewItems = mutableListOf<BalanceCexViewItem>()
    private var isActiveScreen = false
    private var withdrawEnabled = false

    init {
        viewModelScope.launch(Dispatchers.IO) {
            balanceCexRepository.itemsFlow.collect { (assets, adapterState) ->
                handleUpdatedItems(assets, adapterState)
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

    override fun createState()= UiState(
        isRefreshing = isRefreshing,
        viewItems = viewItems,
        sortType = sortType,
        isActiveScreen = isActiveScreen,
        withdrawEnabled = withdrawEnabled,
    )

    private fun handleCexProvider(cexProvider: ICexProvider?) {
        balanceCexRepository.setCexProvider(cexProvider)
    }

    private fun handleXRateUpdate(latestRates: Map<String, CoinPrice?>) {
        for (i in 0 until viewItems.size) {
            val viewItem = viewItems[i]
            viewItem.coin?.uid?.let { coinUid ->
                if (latestRates.containsKey(coinUid)) {
                    viewItems[i] = createBalanceCexViewItem(
                        cexAsset = viewItem.cexAsset,
                        latestRate = latestRates[coinUid],
                        adapterState = viewItem.adapterState
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
            createBalanceCexViewItem(viewItem.cexAsset, viewItem.coin?.uid?.let { latestRates[it] }, viewItem.adapterState)
        }
    }

    private fun handleUpdatedItems(items: List<CexAsset>?, adapterState: AdapterState) {
        if (items != null) {
            isActiveScreen = true

            xRateRepository.setCoinUids(items.mapNotNull { it.coin?.uid })
            val latestRates = xRateRepository.getLatestRates()

            viewItems = items.map { cexAsset ->
                createBalanceCexViewItem(cexAsset, cexAsset.coin?.let { latestRates[it.uid] }, adapterState)
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
                it.cexAsset.freeBalance + it.cexAsset.lockedBalance,
                false,
                it.coinPrice
            )

        }
        totalBalance.setTotalServiceItems(totalServiceItems)
    }

    private fun createBalanceCexViewItem(
        cexAsset: CexAsset,
        latestRate: CoinPrice?,
        adapterState: AdapterState
    ): BalanceCexViewItem {
        return balanceViewItemFactory.cexViewItem(
            cexAsset = cexAsset,
            currency = currency,
            latestRate = latestRate,
            hideBalance = balanceHidden,
            balanceViewType = balanceViewType,
            fullFormat = false,
            adapterState

        )
    }

    private fun sortItems() {
        viewItems = balanceCexSorter.sort(viewItems, sortType).toMutableList()
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
}

data class UiState(
    val isRefreshing: Boolean,
    val viewItems: List<BalanceCexViewItem>,
    val sortType: BalanceSortType,
    val isActiveScreen: Boolean,
    val withdrawEnabled: Boolean
)

data class BalanceCexViewItem(
    val coin: Coin?,
    val coinCode: String,
    val badge: String?,
    val primaryValue: DeemedValue<String>,
    val exchangeValue: DeemedValue<String>,
    val diff: BigDecimal?,
    val secondaryValue: DeemedValue<String>,
    val coinValueLocked: DeemedValue<String?>,
    val fiatValueLocked: DeemedValue<String>,
    val assetId: String,
    val coinPrice: CoinPrice?,
    val cexAsset: CexAsset,
    val depositEnabled: Boolean,
    val withdrawEnabled: Boolean,
    val syncingProgress: SyncingProgress,
    val failedIconVisible: Boolean,
    val errorMessage: String?,
    val adapterState: AdapterState
) {
    val fiatValue by lazy { coinPrice?.value?.let { cexAsset.freeBalance.times(it) } }
}
