package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.rx2.asFlow
import java.math.BigDecimal

class TotalService(
    private val currencyManager: ICurrencyManager,
    private val coinManager: ICoinManager,
    private val marketKit: MarketKit,
    private val localStorage: ILocalStorage
) {
    private var balanceHidden = false
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

    private val platformCoins = listOf(CoinType.Bitcoin, CoinType.Ethereum)
        .mapNotNull {
            coinManager.getPlatformCoin(it)
        }
    private var platformCoin: PlatformCoin? = null
    private var coinPrice: CoinPrice? = null
    private var currency = currencyManager.baseCurrency
    private var items: List<BalanceModule.BalanceItem>? = null
    private var coinPriceUpdatesJob: Job? = null

    private var coroutineScope = CoroutineScope(Dispatchers.IO)

    fun start(balanceHidden: Boolean) {
        this.balanceHidden = balanceHidden

        coroutineScope.launch {
            currencyManager.baseCurrencyUpdatedSignal.asFlow().collect {
                handleUpdatedCurrency(currencyManager.baseCurrency)
            }
        }

        val coin = localStorage.balanceTotalCoinUid?.let { balanceTotalCoinUid ->
            platformCoins.find { it.coin.uid == balanceTotalCoinUid }
        } ?: platformCoins.firstOrNull()

        handleUpdatedPlatformCoin(coin)
    }

    fun stop() {
        coroutineScope.cancel()
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

    fun toggleType() {
        val indexOf = platformCoins.indexOf(platformCoin)
        val platformCoin = (platformCoins.getOrNull(indexOf + 1)
            ?: platformCoins.firstOrNull())

        localStorage.balanceTotalCoinUid = platformCoin?.coin?.uid

        handleUpdatedPlatformCoin(platformCoin)

        refreshTotalCoinValue()

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

    private fun handleUpdatedPlatformCoin(platformCoin: PlatformCoin?) {
        this.platformCoin = platformCoin

        refreshCoinPrice()
        resubscribeForCoinPrice()
    }

    private fun refreshCoinPrice() {
        coinPrice = platformCoin?.let {
            marketKit.coinPrice(it.coin.uid, currency.code)
        }
    }

    private fun resubscribeForCoinPrice() {
        coinPriceUpdatesJob?.cancel()

        platformCoin?.let { platformCoin ->
            coinPriceUpdatesJob = coroutineScope.launch {
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
            val currencyValue: CurrencyValue?,
            val coinValue: CoinValue?,
            val dimmed: Boolean
        ) : State()

        object Hidden : State()
    }
}
