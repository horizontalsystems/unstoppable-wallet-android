package io.horizontalsystems.walletkit.modules.xrate

import androidx.lifecycle.ViewModel
import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.entities.CurrencyValue
import io.horizontalsystems.marketkit.models.CoinPrice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
        return marketKit.coinPriceObservable("xrate-service", coinUid, currency.code)
            .map {
                CurrencyValue(currency, it.value)
            }
    }

    fun getCoinPriceFlow(coinUid: String): Flow<CoinPrice> {
        return marketKit.coinPriceObservable("xrate-service", coinUid, currency.code)
    }
}
