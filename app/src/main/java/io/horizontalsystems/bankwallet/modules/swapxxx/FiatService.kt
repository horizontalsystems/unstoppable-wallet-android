package io.horizontalsystems.bankwallet.modules.swapxxx

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

class FiatService : ServiceState<BigDecimal?>() {
    private var currency: Currency? = null
    private var token: Token? = null
    private var amount: BigDecimal? = null
    private var coinPrice: CoinPrice? = null

    private var fiatAmount: BigDecimal? = null

    override fun createState() = fiatAmount

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
}
