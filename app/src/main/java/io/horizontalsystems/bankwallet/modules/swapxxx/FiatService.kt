package io.horizontalsystems.bankwallet.modules.swapxxx

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
import java.math.RoundingMode

class FiatService : ServiceState<FiatService.State>() {
    private var currency: Currency? = null
    private var token: Token? = null
    private var amount: BigDecimal? = null
    private var coinPrice: CoinPrice? = null

    private var fiatAmount: BigDecimal? = null

    override fun createState() = State(
        coinPrice = coinPrice,
        amount = amount,
        fiatAmount = fiatAmount
    )

    private fun refreshCoinPrice() {
        coinPrice = token?.let { token ->
            currency?.code?.let { currency ->
                App.marketKit.coinPrice(token.coin.uid, currency)
            }
        }
    }

    private fun refreshFiatAmount() {
        fiatAmount = amount?.let { amount ->
            coinPrice?.let { coinPrice ->
                amount * coinPrice.value
            }
        }
    }

    private fun refreshAmount() {
        amount = fiatAmount?.let { fiatAmount ->
            coinPrice?.let { coinPrice ->
                token?.let { token ->
                    fiatAmount.divide(coinPrice.value, token.decimals, RoundingMode.CEILING)
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
