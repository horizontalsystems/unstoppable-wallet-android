package cash.p.terminal.featureStacking.ui.stackingCoinScreen

import androidx.lifecycle.viewModelScope
import cash.p.terminal.featureStacking.ui.PirateInvestmentChartService
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
import io.horizontalsystems.chartview.chart.ChartCurrencyValueFormatterSignificant
import io.horizontalsystems.chartview.chart.ChartViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

abstract class StackingCoinChartViewModel(
    private val service: PirateInvestmentChartService,
) : ChartViewModel(
    service = service,
    valueFormatter = ChartCurrencyValueFormatterSignificant(),
    considerAlwaysPositive = true
) {
    private val balanceHiddenManager: IBalanceHiddenManager by inject(IBalanceHiddenManager::class.java)

    abstract val coinCode: String

    init {
        viewModelScope.launch {
            balanceHiddenManager.balanceHiddenFlow.collectLatest {
                setTitleHidden(it)
            }
        }
    }

    fun setReceiveAddress(address: String) {
        service.setData(coinCode = coinCode, address = address)
    }
}