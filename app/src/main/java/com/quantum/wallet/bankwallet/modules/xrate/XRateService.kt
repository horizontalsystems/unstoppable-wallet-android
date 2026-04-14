package com.quantum.wallet.bankwallet.modules.xrate

import androidx.lifecycle.ViewModel
import com.quantum.wallet.bankwallet.core.managers.MarketKitWrapper
import com.quantum.wallet.bankwallet.entities.Currency
import com.quantum.wallet.bankwallet.entities.CurrencyValue
import io.horizontalsystems.marketkit.models.CoinPrice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlow

class XRateService(
    private val marketKit: MarketKitWrapper,
    private val currency: Currency
) : ViewModel() {

    fun getRate(coinUid: String): CurrencyValue? {
        return marketKit.coinPrice(coinUid, currency.code)?.let {
            CurrencyValue(currency, it.value)
        }
    }

    fun getCoinPrice(coinUid: String) = marketKit.coinPrice(coinUid, currency.code)

    fun getRateFlow(coinUid: String): Flow<CurrencyValue> {
        return marketKit.coinPriceObservable("xrate-service", coinUid, currency.code).asFlow()
            .map {
                CurrencyValue(currency, it.value)
            }
    }

    fun getCoinPriceFlow(coinUid: String): Flow<CoinPrice> {
        return marketKit.coinPriceObservable("xrate-service", coinUid, currency.code).asFlow()
    }
}
