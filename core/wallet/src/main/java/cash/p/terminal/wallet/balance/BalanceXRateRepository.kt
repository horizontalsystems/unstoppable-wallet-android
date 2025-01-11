package cash.p.terminal.wallet.balance

import io.horizontalsystems.core.entities.Currency
import cash.p.terminal.wallet.models.CoinPrice
import io.reactivex.Observable

interface BalanceXRateRepository {
    val baseCurrency: Currency
    val itemObservable: Observable<Map<String, CoinPrice?>>
    fun setCoinUids(coinUids: List<String>)
    fun getLatestRates(): Map<String, CoinPrice?>
    fun refresh()
}