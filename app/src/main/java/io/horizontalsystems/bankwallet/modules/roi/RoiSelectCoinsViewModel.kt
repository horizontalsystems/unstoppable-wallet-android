package io.horizontalsystems.bankwallet.modules.roi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.HsTimePeriod
import java.util.UUID

class RoiSelectCoinsViewModel(
    private val localStorage: ILocalStorage,
    private val marketKit: MarketKitWrapper
) : ViewModelUiState<RoiSelectCoinsUiState>() {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RoiSelectCoinsViewModel(App.localStorage, App.marketKit) as T
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
    private val coinImages = mapOf(
        "tether-gold" to R.drawable.ic_gold_32,
        "sp500" to R.drawable.ic_sp500_32,
    )

    init {
        val tmpItems = mutableListOf<XxxItem>()

        val fullCoins = marketKit.fullCoins("", 100).toMutableList()

        defaultCoins.map { defaultCoin ->
            val index = fullCoins.indexOfFirst { it.coin.uid == defaultCoin.uid }
            val fullCoin = if (index != -1) {
                fullCoins.removeAt(index)
            } else {
                null
            }

            val xxxItem = if (fullCoin != null) {
                XxxItem.fromCoin(fullCoin.coin)
            } else {
                XxxItem(defaultCoin, localImage = coinImages[defaultCoin.uid])
            }

            tmpItems.add(xxxItem)
        }

        tmpItems.addAll(
            fullCoins.map {
                XxxItem.fromCoin(it.coin)
            }
        )

        items = tmpItems

        emitState()
    }

    override fun createState() = RoiSelectCoinsUiState(
        periods = periods,
        items = items,
        isSaveable = selectedCoins.size == 3,
        selectedCoins = selectedCoins
    )

    fun onToggle(item: XxxItem, selected: Boolean) {
        if (selected) {
            if (selectedCoins.size >= 3) throw CapacityExceededException()

            selectedCoins += item.performanceCoin
        } else {
            selectedCoins -= item.performanceCoin
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
    val imageUrl: String? = null,
    val alternativeImageUrl: String? = null,
    val localImage: Int? = null,
) {
    val uid by performanceCoin::uid
    val code by performanceCoin::code
    val name by performanceCoin::name

    companion object {
        fun fromCoin(coin: Coin): XxxItem {
            return XxxItem(
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
}
