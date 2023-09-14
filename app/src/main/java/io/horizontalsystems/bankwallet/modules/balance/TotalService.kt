package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.managers.BalanceHiddenManager
import io.horizontalsystems.bankwallet.core.managers.BaseTokenManager
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import java.math.BigDecimal

class TotalService(
    private val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper,
    private val baseTokenManager: BaseTokenManager,
    private val balanceHiddenManager: BalanceHiddenManager
) {
    private var balanceHidden = balanceHiddenManager.balanceHidden
    private var totalCurrencyValue: CurrencyValue? = null
    private var totalCoinValue: CoinValue? = null
    private var dimmed = false

    private val _stateFlow: MutableStateFlow<State> = MutableStateFlow(
        State.Visible(
            currencyValue = totalCurrencyValue,
            coinValue = totalCoinValue,
            dimmed = dimmed
        )
    )
    val stateFlow = _stateFlow.asStateFlow()

    private var baseToken: Token? = null
    private var coinPrice: CoinPrice? = null
    private var currency = currencyManager.baseCurrency
    private var items: List<BalanceItem>? = null
    private var coinPriceUpdatesJob: Job? = null

    private var coroutineScope = CoroutineScope(Dispatchers.IO)

    fun start() {
        coroutineScope.launch {
            currencyManager.baseCurrencyUpdatedSignal.asFlow().collect {
                handleUpdatedCurrency(currencyManager.baseCurrency)
            }
        }

        coroutineScope.launch {
            baseTokenManager.baseTokenFlow.collect {
                handleUpdatedBaseToken(it)
            }
        }

        coroutineScope.launch {
            balanceHiddenManager.balanceHiddenFlow.collect {
                setBalanceHidden(it)
            }
        }
    }

    fun stop() {
        coroutineScope.cancel()
    }

    fun setItems(items: List<BalanceItem>?) {
        this.items = items

        refreshTotalCurrencyValue()
        refreshTotalCoinValue()
        refreshDimmed()

        emitState()
    }

    fun toggleType() {
        baseTokenManager.toggleBaseToken()
    }

    private fun setBalanceHidden(balanceHidden: Boolean) {
        this.balanceHidden = balanceHidden

        emitState()
    }

    private fun handleUpdatedCurrency(currency: Currency) {
        this.currency = currency

        refreshCoinPrice()
        resubscribeForCoinPrice()
        refreshTotalCurrencyValue()
        refreshTotalCoinValue()

        emitState()
    }

    private fun handleUpdatedBaseToken(baseToken: Token?) {
        this.baseToken = baseToken

        refreshCoinPrice()
        resubscribeForCoinPrice()
        refreshTotalCoinValue()

        emitState()
    }

    private fun refreshCoinPrice() {
        coinPrice = baseToken?.let {
            marketKit.coinPrice(it.coin.uid, currency.code)
        }
    }

    private fun resubscribeForCoinPrice() {
        coinPriceUpdatesJob?.cancel()

        baseToken?.let { platformCoin ->
            coinPriceUpdatesJob = coroutineScope.launch {
                marketKit.coinPriceObservable("total", platformCoin.coin.uid, currency.code)
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
                total = total.add(item.coinPrice?.value?.let { item.value.times(it) } ?: BigDecimal.ZERO)
            }

            CurrencyValue(currency, total)
        }
    }

    private fun refreshTotalCoinValue() {
        val tmpTotalCurrencyValue = totalCurrencyValue
        val tmpCoinPrice = coinPrice
        val tmpBaseToken = baseToken

        totalCoinValue = when {
            tmpTotalCurrencyValue == null -> null
            tmpCoinPrice == null -> null
            tmpBaseToken == null -> null
            else -> {
                val value = tmpTotalCurrencyValue.value / tmpCoinPrice.value
                CoinValue(tmpBaseToken, value)
            }
        }
    }

    private fun refreshDimmed() {
        dimmed = items?.let { items ->
            items.any {
                it.isValuePending || (it.coinPrice != null && it.coinPrice.expired)
            }
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

    data class BalanceItem(
        val value: BigDecimal,
        val isValuePending: Boolean,
        val coinPrice: CoinPrice?
    )

    sealed class State {
        data class Visible(
            val currencyValue: CurrencyValue?,
            val coinValue: CoinValue?,
            val dimmed: Boolean
        ) : State()

        object Hidden : State()
    }
}
