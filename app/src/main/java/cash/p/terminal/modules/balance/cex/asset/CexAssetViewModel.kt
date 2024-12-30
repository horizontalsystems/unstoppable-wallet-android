package cash.p.terminal.modules.balance.cex.asset

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.core.App
import cash.p.terminal.core.managers.BalanceHiddenManager
import cash.p.terminal.core.managers.PriceManager
import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.modules.balance.BalanceViewItemFactory
import cash.p.terminal.wallet.balance.BalanceViewType
import cash.p.terminal.modules.balance.DefaultBalanceXRateRepository
import cash.p.terminal.modules.balance.cex.BalanceCexViewItem
import cash.p.terminal.wallet.models.CoinPrice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.collect

class CexAssetViewModel(
    private val cexAsset: CexAsset,
    private val balanceHiddenManager: BalanceHiddenManager,
    private val xRateRepository: DefaultBalanceXRateRepository,
    private val balanceViewItemFactory: BalanceViewItemFactory,
    private val priceManager: PriceManager
) : ViewModel() {

    private val title = cexAsset.id
    private var balanceViewItem: BalanceCexViewItem? = null

    var uiState by mutableStateOf(
        UiState(
            title = title,
            balanceViewItem = balanceViewItem
        )
    )
        private set

    init {
        xRateRepository.setCoinUids(listOfNotNull(cexAsset.coin?.uid))

        viewModelScope.launch(Dispatchers.IO) {
            balanceHiddenManager.balanceHiddenFlow.collect {
                updateBalanceViewItem()
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            xRateRepository.itemObservable.collect {
                updateBalanceViewItem()
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            priceManager.priceChangeIntervalFlow.collect {
                updateBalanceViewItem()
            }
        }
    }

    fun toggleBalanceVisibility() {
        balanceHiddenManager.toggleBalanceHidden()
    }

    private fun createBalanceCexViewItem(
        cexAsset: CexAsset,
        latestRate: CoinPrice?
    ): BalanceCexViewItem {
        val currency = xRateRepository.baseCurrency
        val balanceHidden = balanceHiddenManager.balanceHidden

        return balanceViewItemFactory.cexViewItem(
            cexAsset = cexAsset,
            currency = currency,
            latestRate = latestRate,
            hideBalance = balanceHidden,
            balanceViewType = BalanceViewType.CoinThenFiat,
            fullFormat = true,
            adapterState = AdapterState.Synced
        )
    }

    @Synchronized
    private fun updateBalanceViewItem() {
        val latestRates = xRateRepository.getLatestRates()
        balanceViewItem = createBalanceCexViewItem(cexAsset, cexAsset.coin?.let { latestRates[it.uid] })

        emitUiState()
    }

    private fun emitUiState() {
        viewModelScope.launch {
            uiState = UiState(
                title = title,
                balanceViewItem = balanceViewItem,
            )
        }
    }

    class Factory(private val cexAsset: CexAsset) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CexAssetViewModel(
                cexAsset,
                App.balanceHiddenManager,
                DefaultBalanceXRateRepository("cex-asset", App.currencyManager, App.marketKit),
                BalanceViewItemFactory(),
                App.priceManager
                ) as T
        }
    }

    data class UiState(
        val title: String,
        val balanceViewItem: BalanceCexViewItem?
    )
}
