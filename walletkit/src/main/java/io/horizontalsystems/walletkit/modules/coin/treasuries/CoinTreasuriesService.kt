package io.horizontalsystems.walletkit.modules.coin.treasuries

import io.horizontalsystems.walletkit.core.managers.CurrencyManager
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.entities.DataState
import io.horizontalsystems.walletkit.modules.coin.treasuries.CoinTreasuriesModule.TreasuryTypeFilter
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.CoinTreasury
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class CoinTreasuriesService(
    val coin: Coin,
    private val repository: CoinTreasuriesRepository,
    private val currencyManager: CurrencyManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val _stateFlow = MutableSharedFlow<DataState<List<CoinTreasury>>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val stateFlow: SharedFlow<DataState<List<CoinTreasury>>> = _stateFlow.asSharedFlow()

    val currency: Currency
        get() = currencyManager.baseCurrency

    val treasuryTypes = TreasuryTypeFilter.values().toList()
    var treasuryType: TreasuryTypeFilter = TreasuryTypeFilter.All
        set(value) {
            field = value
            rebuildItems()
        }

    var sortDescending: Boolean = true
        set(value) {
            field = value
            rebuildItems()
        }

    private fun rebuildItems() {
        fetch(forceRefresh = false)
    }

    private fun forceRefresh() {
        fetch(forceRefresh = true)
    }

    private fun fetch(forceRefresh: Boolean) {
        coroutineScope.launch {
            try {
                val coinTreasuries = repository.coinTreasuriesSingle(coin.uid, currency.code, treasuryType, sortDescending, forceRefresh)
                _stateFlow.tryEmit(DataState.Success(coinTreasuries))
            } catch (e: Throwable) {
                _stateFlow.tryEmit(DataState.Error(e))
            }
        }
    }

    fun start() {
        forceRefresh()
    }

    fun refresh() {
        forceRefresh()
    }

    fun stop() {
        coroutineScope.cancel()
    }
}
