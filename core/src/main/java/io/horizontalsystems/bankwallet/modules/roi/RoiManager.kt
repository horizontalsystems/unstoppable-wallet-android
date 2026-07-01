package io.horizontalsystems.bankwallet.modules.roi

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.marketkit.models.HsTimePeriod
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class RoiManager(private val localStorage: ILocalStorage) {

    private val gold = PerformanceCoin("tether-gold", "GOLD", "Commodity")
    private val sp500 = PerformanceCoin("snp", "SP500", "S&P 500")
    val prioritizedCoins = listOf(gold, sp500, PerformanceCoin("bitcoin", "BTC", "Bitcoin"))
    private val defaultCoins = listOf(
        sp500,
        PerformanceCoin("bitcoin", "BTC", "Bitcoin"),
        PerformanceCoin("tether", "USDT", "Tether")
    )
    private val defaultPeriods = listOf(
        HsTimePeriod.Month6,
        HsTimePeriod.Year1
    )

    private val _dataUpdatedFlow = MutableSharedFlow<Unit>()
    val dataUpdatedFlow = _dataUpdatedFlow.asSharedFlow()

    fun getSelectedCoins(): List<PerformanceCoin> {
        return localStorage.roiPerformanceCoins.ifEmpty { defaultCoins }
    }

    fun getSelectedPeriods(): List<HsTimePeriod> {
        return localStorage.selectedPeriods.ifEmpty { defaultPeriods }
    }

    suspend fun update(selectedCoins: List<PerformanceCoin>, selectedPeriods: List<HsTimePeriod>) {
        localStorage.roiPerformanceCoins = selectedCoins
        localStorage.selectedPeriods = selectedPeriods

        _dataUpdatedFlow.emit(Unit)
    }
}
