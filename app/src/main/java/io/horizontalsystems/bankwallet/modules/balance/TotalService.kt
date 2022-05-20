package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.managers.NumberRounding
import io.horizontalsystems.bankwallet.entities.CoinValueRounded
import io.horizontalsystems.bankwallet.entities.CurrencyValueRounded
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

class TotalService(
    private val currencyManager: ICurrencyManager,
    private val coinManager: ICoinManager,
    private val marketKit: MarketKit,
    private val numberRounding: NumberRounding
) {
    private var balanceHidden = false
    private var totalCurrencyValue: CurrencyValueRounded? = null
    private var totalCoinValue: CoinValueRounded? = null
    private var dimmed = false

    private val _stateFlow: MutableStateFlow<State> = MutableStateFlow(
        State.Visible(
            currencyValue = totalCurrencyValue,
            coinValue = totalCoinValue,
            dimmed = dimmed
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    private val platformCoins = listOf(CoinType.Bitcoin, CoinType.Ethereum)
        .mapNotNull {
            coinManager.getPlatformCoin(it)
        }
    private var platformCoin: PlatformCoin? = null
    private var coinPrice: CoinPrice? = null
    private var currency = currencyManager.baseCurrency
    private var items: List<BalanceModule.BalanceItem>? = null
    private var coinPriceUpdatesJob: Job? = null

    suspend fun start(balanceHidden: Boolean) = withContext(Dispatchers.IO) {
        this@TotalService.balanceHidden = balanceHidden

        launch {
            currencyManager.baseCurrencyUpdatedSignal.asFlow().collect {
                handleUpdatedCurrency(currencyManager.baseCurrency)
            }
        }

        handleUpdatedPlatformCoin(platformCoins.firstOrNull())
    }

    fun setBalanceItems(items: List<BalanceModule.BalanceItem>?) {
        this.items = items

        refreshTotalCurrencyValue()
        refreshTotalCoinValue()
        refreshDimmed()

        emitState()
    }

    fun setBalanceHidden(balanceHidden: Boolean) {
        this.balanceHidden = balanceHidden

        emitState()
    }

    suspend fun toggleType() {
        val indexOf = platformCoins.indexOf(platformCoin)
        val platformCoin = (platformCoins.getOrNull(indexOf + 1)
            ?: platformCoins.firstOrNull())

        handleUpdatedPlatformCoin(platformCoin)

        refreshTotalCoinValue()

        emitState()
    }

    private suspend fun handleUpdatedCurrency(currency: Currency) {
        this.currency = currency

        refreshCoinPrice()
        resubscribeForCoinPrice()
        refreshTotalCurrencyValue()
        refreshTotalCoinValue()

        emitState()
    }

    private suspend fun handleUpdatedPlatformCoin(platformCoin: PlatformCoin?) {
        this.platformCoin = platformCoin

        refreshCoinPrice()
        resubscribeForCoinPrice()
    }

    private fun refreshCoinPrice() {
        coinPrice = platformCoin?.let {
            marketKit.coinPrice(it.coin.uid, currency.code)
        }
    }

    private suspend fun resubscribeForCoinPrice() = withContext(Dispatchers.IO) {
        coinPriceUpdatesJob?.cancel()

        platformCoin?.let { platformCoin ->
            coinPriceUpdatesJob = launch {
                marketKit.coinPriceObservable(platformCoin.coin.uid, currency.code)
                    .asFlow()
                    .collect {
                        coinPrice = it

                        refreshTotalCoinValue()

                        emitState()
                    }
            }
        }
    }

    private fun refreshTotalCurrencyValue() {
        totalCurrencyValue = items?.let { items ->
            var total = BigDecimal.ZERO
            items.forEach { item ->
                total = total.add(item.balanceFiatTotal ?: BigDecimal.ZERO)
            }

            CurrencyValueRounded(currency, numberRounding.getRoundedCurrencyShort(total, 8))
        }
    }

    private fun refreshTotalCoinValue() {
        val tmpTotalCurrencyValue = totalCurrencyValue?.value
        val tmpCoinPrice = coinPrice
        val tmpPlatformCoin = platformCoin

        totalCoinValue = when {
            tmpTotalCurrencyValue == null -> null
            tmpCoinPrice == null -> null
            tmpPlatformCoin == null -> null
            else -> {
                val value = tmpTotalCurrencyValue.value.divide(tmpCoinPrice.value, tmpPlatformCoin.decimals, RoundingMode.HALF_UP)
                val rounded = numberRounding.getRoundedCoinShort(value, tmpPlatformCoin.decimals)
                CoinValueRounded(tmpPlatformCoin, rounded)
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
            if (balanceHidden) {
                State.Hidden
            } else {
                State.Visible(
                    currencyValue = totalCurrencyValue,
                    coinValue = totalCoinValue,
                    dimmed = dimmed
                )
            }
        }
    }

    sealed class State {
        data class Visible(
            val currencyValue: CurrencyValueRounded?,
            val coinValue: CoinValueRounded?,
            val dimmed: Boolean
        ) : State()

        object Hidden : State()
    }
}
