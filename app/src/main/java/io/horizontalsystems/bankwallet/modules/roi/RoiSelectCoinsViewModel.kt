package io.horizontalsystems.bankwallet.modules.roi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.marketkit.models.HsTimePeriod
import java.util.UUID

class RoiSelectCoinsViewModel(private val localStorage: ILocalStorage) : ViewModelUiState<RoiSelectCoinsUiState>() {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RoiSelectCoinsViewModel(App.localStorage) as T
        }
    }

    private var periods = listOf(
        HsTimePeriod.Week1,
        HsTimePeriod.Year1
    )

    private val gold = PerformanceCoin("tether-gold", "GOLD", "Commodity")
    private val sp500 = PerformanceCoin("sp500", "SP500", "S&P 500")
    private val defaultCoins = listOf(gold, sp500, PerformanceCoin("bitcoin", "BTC", "Bitcoin"))
    private val defaultPeriods = listOf(
        HsTimePeriod.Month6,
        HsTimePeriod.Year1
    )
    private var items: List<XxxItem> = listOf()

    private var selectedCoins = localStorage.roiPerformanceCoins.ifEmpty { defaultCoins }

    init {
        val fullCoins = App.marketKit.fullCoins("", 100)

        items = fullCoins.map {
            XxxItem(
                PerformanceCoin(
                    it.coin.uid,
                    it.coin.code,
                    it.coin.name
                )
            )
        }

        emitState()
    }

    override fun createState() = RoiSelectCoinsUiState(
        periods = periods,
        items = items,
        isSaveable = selectedCoins.size == 3,
        selectedCoins = selectedCoins.toList()
    )

    fun onToggle(item: XxxItem, selected: Boolean) {
        if (selected) {
            if (selectedCoins.size >= 3) throw CapacityExceededException()

            selectedCoins = selectedCoins + item.performanceCoin

        } else {
            val mutableList = selectedCoins.toMutableList()
            mutableList.remove(item.performanceCoin)

            selectedCoins = mutableList
        }

        emitState()
    }

    fun onApply() {
        localStorage.roiPerformanceCoins = selectedCoins
    }
}

class CapacityExceededException : Exception(Translator.getString(R.string.ROI_SelectCoin_Warning_AllowedNumberOfCoins, 3))

data class RoiSelectCoinsUiState(
    val periods: List<HsTimePeriod>,
    val items: List<XxxItem>,
    val isSaveable: Boolean,
    val selectedCoins: List<PerformanceCoin>,
) {
    val uuid = UUID.randomUUID().toString()
}

data class PerformanceCoin(
    val uid: String,
    val code: String,
    val name: String,
)

data class XxxItem(
    val performanceCoin: PerformanceCoin,
) {
    val uid by performanceCoin::uid
    val code by performanceCoin::code
    val name by performanceCoin::name
}
