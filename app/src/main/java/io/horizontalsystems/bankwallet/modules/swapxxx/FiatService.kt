package io.horizontalsystems.bankwallet.modules.swapxxx

import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import java.math.BigDecimal
import java.math.RoundingMode

class FiatService(private val marketKit: MarketKitWrapper) : ServiceState<FiatService.State>() {
    private var currency: Currency? = null
    private var token: Token? = null
    private var amount: BigDecimal? = null
    private var coinPrice: CoinPrice? = null

    private var fiatAmount: BigDecimal? = null
    private var coinPriceUpdatesJob: Job? = null
    private var coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun createState() = State(
        coinPrice = coinPrice,
        amount = amount,
        fiatAmount = fiatAmount
    )

    private fun refreshCoinPrice() {
        coinPrice = token?.let { token ->
            currency?.code?.let { currency ->
                marketKit.coinPrice(token.coin.uid, currency)
            }
        }
        resubscribeForCoinPrice()
    }

    private fun refreshFiatAmount() {
        fiatAmount = amount?.let { amount ->
            coinPrice?.let { coinPrice ->
                currency?.let { currency ->
                    (amount * coinPrice.value).setScale(currency.decimal, RoundingMode.DOWN).stripTrailingZeros()
                }
            }
        }
    }

    private fun refreshAmount() {
        amount = fiatAmount?.let { fiatAmount ->
            coinPrice?.let { coinPrice ->
                token?.let { token ->
                    fiatAmount.divide(coinPrice.value, token.decimals, RoundingMode.DOWN).stripTrailingZeros()
                }
            }
        }
    }

    private fun resubscribeForCoinPrice() {
        coinPriceUpdatesJob?.cancel()
        val currency = currency ?: return

        token?.let { platformCoin ->
            coinPriceUpdatesJob = coroutineScope.launch {
                marketKit.coinPriceObservable("swap", platformCoin.coin.uid, currency.code)
                    .asFlow()
                    .collect {
                        coinPrice = it
                        refreshAmount()
                        emitState()
                    }
            }
        }
    }

    fun setCurrency(currency: Currency) {
        if (this.currency == currency) return

        this.currency = currency

        refreshCoinPrice()
        refreshFiatAmount()

        emitState()
    }

    fun setToken(token: Token?) {
        if (this.token == token) return

        this.token = token

        refreshCoinPrice()
        refreshFiatAmount()

        emitState()
    }

    fun setAmount(amount: BigDecimal?) {
        if (this.amount == amount) return

        this.amount = amount
        refreshFiatAmount()

        emitState()
    }

    fun setFiatAmount(fiatAmount: BigDecimal?) {
        if (this.fiatAmount == fiatAmount) return

        this.fiatAmount = fiatAmount
        refreshAmount()

        emitState()
    }

    data class State(
        val amount: BigDecimal?,
        val fiatAmount: BigDecimal?,
        val coinPrice: CoinPrice?
    )
}
