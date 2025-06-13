package io.horizontalsystems.bankwallet.modules.roi

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.marketkit.models.HsTimePeriod

class RoiManager(private val localStorage: ILocalStorage) {

    private val gold = PerformanceCoin("tether-gold", "GOLD", "Commodity")
    private val sp500 = PerformanceCoin("sp500", "SP500", "S&P 500")
    val defaultCoins = listOf(gold, sp500, PerformanceCoin("bitcoin", "BTC", "Bitcoin"))
    private val defaultPeriods = listOf(
        HsTimePeriod.Month6,
        HsTimePeriod.Year1
    )

    fun getSelectedCoins(): List<PerformanceCoin> {
        return localStorage.roiPerformanceCoins.ifEmpty { defaultCoins }
    }

    fun getSelectedPeriods(): List<HsTimePeriod> {
        return localStorage.selectedPeriods.ifEmpty { defaultPeriods }
    }

    fun update(selectedCoins: List<PerformanceCoin>, selectedPeriods: List<HsTimePeriod>) {
        localStorage.roiPerformanceCoins = selectedCoins
        localStorage.selectedPeriods = selectedPeriods
    }
}
