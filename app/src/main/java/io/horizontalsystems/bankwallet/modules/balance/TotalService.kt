package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class TotalService(
    private val currencyManager: ICurrencyManager,
    private val coinManager: ICoinManager,
    private val marketKit: MarketKit
) {
    private var totalCurrencyValue: CurrencyValue? = null
    private var totalCoinValue: CoinValue? = null
    private var dimmed = false

    private val _stateFlow = MutableStateFlow(
        State(
            currencyValue = totalCurrencyValue,
            coinValue = totalCoinValue,
            dimmed = dimmed
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    private val platformCoins: List<PlatformCoin>
    private var platformCoin: PlatformCoin? = null
    private var coinPrice: CoinPrice? = null
    private var currency = currencyManager.baseCurrency
    private var items: List<BalanceModule.BalanceItem>? = null

    init {
        platformCoins = listOf(
            CoinType.Bitcoin, CoinType.Ethereum
        ).mapNotNull {
            coinManager.getPlatformCoin(it)
        }

        handleUpdatedPlatformCoin(platformCoins.firstOrNull())
    }

    suspend fun start() = withContext(Dispatchers.IO) {
        launch {
            currencyManager.baseCurrencyUpdatedSignal.asFlow().collect {
                handleUpdatedCurrency(currencyManager.baseCurrency)
            }
        }
    }

    fun setBalanceItems(items: List<BalanceModule.BalanceItem>?) {
        this.items = items

        refreshTotalCurrencyValue()
        refreshTotalCoinValue()
        refreshDimmed()

        emitState()
    }

    fun toggleType() {
        val indexOf = platformCoins.indexOf(platformCoin)
        val platformCoin = (platformCoins.getOrNull(indexOf + 1)
            ?: platformCoins.firstOrNull())

        handleUpdatedPlatformCoin(platformCoin)

        refreshTotalCoinValue()

        emitState()
    }

    private fun handleUpdatedCurrency(currency: Currency) {
        this.currency = currency

        refreshCoinPrice()
        refreshTotalCurrencyValue()

        emitState()
    }

    private fun handleUpdatedPlatformCoin(platformCoin: PlatformCoin?) {
        this.platformCoin = platformCoin

        refreshCoinPrice()
    }

    private fun refreshCoinPrice() {
        coinPrice = platformCoin?.let {
            marketKit.coinPrice(it.coin.uid, currency.code)
        }
    }

    private fun refreshTotalCurrencyValue() {
        totalCurrencyValue = items?.let { items ->
            var total = BigDecimal.ZERO
            items.forEach { item ->
                total = total.add(item.balanceFiatTotal ?: BigDecimal.ZERO)
            }

            CurrencyValue(currency, total)
        }
    }

    private fun refreshTotalCoinValue() {
        val tmpTotalCurrencyValue = totalCurrencyValue
        val tmpCoinPrice = coinPrice
        val tmpPlatformCoin = platformCoin

        totalCoinValue = when {
            tmpTotalCurrencyValue == null -> null
            tmpCoinPrice == null -> null
            tmpPlatformCoin == null -> null
            else -> {
                val value = tmpTotalCurrencyValue.value / tmpCoinPrice.value
                CoinValue(tmpPlatformCoin, value)
            }
        }
    }

    private fun refreshDimmed() {
        dimmed = items?.let { items ->
            items.any { it.state !is AdapterState.Synced || (it.coinPrice != null && it.coinPrice.expired) }
        } ?: false
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                currencyValue = totalCurrencyValue,
                coinValue = totalCoinValue,
                dimmed = dimmed
            )
        }
    }

    data class State(
        val currencyValue: CurrencyValue?,
        val coinValue: CoinValue?,
        val dimmed: Boolean
    )
}
