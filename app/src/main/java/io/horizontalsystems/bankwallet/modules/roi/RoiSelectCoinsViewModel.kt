package io.horizontalsystems.bankwallet.modules.roi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.chart.stringResId
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.HsTimePeriod
import kotlinx.coroutines.launch
import java.util.UUID

class RoiSelectCoinsViewModel(
    private val marketKit: MarketKitWrapper,
    private val roiManager: RoiManager
) : ViewModelUiState<RoiSelectCoinsUiState>() {
    private var periods = listOf(HsTimePeriod.Week1, HsTimePeriod.Month1, HsTimePeriod.Month3, HsTimePeriod.Month6, HsTimePeriod.Year1, HsTimePeriod.Year5)
    private var selectedPeriods = roiManager.getSelectedPeriods()
    private var selectedCoins = roiManager.getSelectedCoins()

    private val coinImages = mapOf(
        "tether-gold" to R.drawable.ic_gold_32,
        "snp" to R.drawable.ic_sp500_32,
    )
    private val allCoinItems: List<CoinItem>
    private var coinItems: List<CoinItem>

    init {
        val tmpItems = mutableListOf<CoinItem>()

        val fullCoins = marketKit.topFullCoins(100).toMutableList()

        roiManager.prioritizedCoins.map { prioritizedCoin ->
            val index = fullCoins.indexOfFirst { it.coin.uid == prioritizedCoin.uid }
            val fullCoin = if (index != -1) {
                fullCoins.removeAt(index)
            } else {
                null
            }

            val coinItem = if (fullCoin != null) {
                CoinItem.fromCoin(fullCoin.coin)
            } else {
                CoinItem(prioritizedCoin, localImage = coinImages[prioritizedCoin.uid])
            }

            tmpItems.add(coinItem)
        }

        tmpItems.addAll(
            fullCoins.map {
                CoinItem.fromCoin(it.coin)
            }
        )

        allCoinItems = tmpItems
        coinItems = allCoinItems
    }

    override fun createState() = RoiSelectCoinsUiState(
        periods = periods.map { it.toTranslatable() },
        selectedPeriods = selectedPeriods.map { it.toTranslatable() },
        coinItems = coinItems,
        selectedCoins = selectedCoins,
        isSaveable = selectedCoins.size == 3
    )

    fun onToggle(item: CoinItem, selected: Boolean) {
        if (selected) {
            if (selectedCoins.size >= 3) throw CapacityExceededException()

            selectedCoins += item.performanceCoin
        } else {
            selectedCoins -= item.performanceCoin
        }

        emitState()
    }

    fun onApply() {
        viewModelScope.launch {
            val sorted = selectedCoins.sortedBy { sc ->
                allCoinItems.indexOfFirst { it.performanceCoin.uid == sc.uid }
            }
            roiManager.update(sorted, selectedPeriods)
        }
    }

    fun onSelectPeriod(index: Int, selected: HsTimePeriodTranslatable) {
        val selectedTimePeriod = selected.timePeriod

        val mutableList = selectedPeriods.toMutableList()

        val indexToReset = selectedPeriods.indexOf(selectedTimePeriod)
        if (indexToReset != -1) {
            mutableList[indexToReset] = (periods - selectedTimePeriod).first()
        }

        mutableList[index] = selectedTimePeriod

        selectedPeriods = mutableList

        emitState()
    }

    fun onFilter(filter: String) {
        coinItems = if (filter.isBlank()) {
            allCoinItems
        } else {
            allCoinItems.filter {
                it.name.contains(filter, true) || it.code.contains(filter, true)
            }
        }

        emitState()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RoiSelectCoinsViewModel(App.marketKit, App.roiManager) as T
        }
    }
}

class CapacityExceededException : Exception(Translator.getString(R.string.ROI_SelectCoin_Warning_AllowedNumberOfCoins, 3))

data class RoiSelectCoinsUiState(
    val periods: List<HsTimePeriodTranslatable>,
    val selectedPeriods: List<HsTimePeriodTranslatable>,
    val coinItems: List<CoinItem>,
    val selectedCoins: List<PerformanceCoin>,
    val isSaveable: Boolean,
) {
    val uuid = UUID.randomUUID().toString()
}

data class CoinItem(
    val performanceCoin: PerformanceCoin,
    val imageUrl: String? = null,
    val alternativeImageUrl: String? = null,
    val localImage: Int? = null,
) {
    val uid by performanceCoin::uid
    val code by performanceCoin::code
    val name by performanceCoin::name

    companion object {
        fun fromCoin(coin: Coin) = CoinItem(
            PerformanceCoin(
                coin.uid,
                coin.code,
                coin.name,
            ),
            coin.imageUrl,
            coin.alternativeImageUrl,
        )
    }
}

data class HsTimePeriodTranslatable(val timePeriod: HsTimePeriod) : WithTranslatableTitle {
    override val title = TranslatableString.ResString(timePeriod.stringResId)

}

fun HsTimePeriod.toTranslatable(): HsTimePeriodTranslatable {
    return HsTimePeriodTranslatable(this)
}