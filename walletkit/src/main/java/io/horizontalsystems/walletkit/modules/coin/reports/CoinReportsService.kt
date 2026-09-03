package io.horizontalsystems.walletkit.modules.coin.reports

import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.walletkit.entities.DataState
import io.horizontalsystems.marketkit.models.CoinReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class CoinReportsService(
    private val coinUid: String,
    private val marketKit: MarketKitWrapper
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val _stateFlow = MutableSharedFlow<DataState<List<CoinReport>>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val stateFlow: SharedFlow<DataState<List<CoinReport>>> = _stateFlow.asSharedFlow()

    private fun fetch() {
        coroutineScope.launch {
            try {
                val reports = marketKit.coinReportsSingle(coinUid)
                _stateFlow.tryEmit(DataState.Success(reports))
            } catch (e: Throwable) {
                _stateFlow.tryEmit(DataState.Error(e))
            }
        }
    }

    fun start() {
        fetch()
    }

    fun refresh() {
        fetch()
    }

    fun stop() {
        coroutineScope.cancel()
    }
}
