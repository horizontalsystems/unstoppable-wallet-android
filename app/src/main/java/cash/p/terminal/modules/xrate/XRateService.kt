package cash.p.terminal.modules.xrate

import androidx.lifecycle.ViewModel
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.entities.Currency
import cash.p.terminal.entities.CurrencyValue
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.Flow
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

    fun getRateFlow(coinUid: String): Flow<CurrencyValue> {
        return marketKit.coinPriceObservable(coinUid, currency.code)
            .subscribeOn(Schedulers.io())
            .map {
                CurrencyValue(currency, it.value)
            }
            .asFlow()
    }
}
