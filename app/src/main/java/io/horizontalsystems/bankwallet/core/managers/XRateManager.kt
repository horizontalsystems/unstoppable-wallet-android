package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.IXRateManager
import io.horizontalsystems.xrateskit.XRatesKit
import io.horizontalsystems.xrateskit.entities.ChartInfo
import io.horizontalsystems.xrateskit.entities.ChartType
import io.horizontalsystems.xrateskit.entities.MarketInfo
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

class XRateManager(context: Context,
                   private val walletManager: IWalletManager,
                   private val currencyManager: ICurrencyManager
) : IXRateManager {

    private val disposables = CompositeDisposable()
    private val kit: XRatesKit = XRatesKit.create(context, currencyManager.baseCurrency.code)

    init {
        walletManager.walletsUpdatedSignal
                .subscribeOn(Schedulers.io())
                .subscribe {
                    onWalletsUpdated()
                }.let {
                    disposables.add(it)
                }

        currencyManager.baseCurrencyUpdatedSignal
                .subscribeOn(Schedulers.io())
                .subscribe {
                    onBaseCurrencyUpdated()
                }.let {
                    disposables.add(it)
                }
    }

    override fun marketInfo(coin: String, currency: String): MarketInfo? {
        return kit.getMarketInfo(coin, currency)
    }

    override fun getLatestRate(coin: String, currency: String): BigDecimal? {
        val marketInfo = marketInfo(coin, currency)

        return when {
            marketInfo == null -> null
            marketInfo.isExpired() -> null
            else -> marketInfo.rate
        }

    }

    override fun marketInfoObservable(coin: String, currency: String): Observable<MarketInfo> {
        return kit.marketInfoObservable(coin, currency)
    }

    override fun marketInfoObservable(currency: String): Observable<Map<String, MarketInfo>> {
        return kit.marketInfoMapObservable(currency)
    }

    override fun historicalRate(coin: String, currency: String, timestamp: Long): Single<BigDecimal> {
        return kit.historicalRate(coin, currency, timestamp)
    }

    override fun chartInfo(coin: String, currency: String, chartType: ChartType): ChartInfo? {
        return kit.getChartInfo(coin, currency, chartType)
    }

    override fun chartInfoObservable(coin: String, currency: String, chartType: ChartType): Observable<ChartInfo> {
        return kit.chartInfoObservable(coin, currency, chartType)
    }

    override fun refresh() {
        kit.refresh()
    }

    private fun onWalletsUpdated() {
        kit.set(walletManager.wallets.map { it.coin.code })
    }

    private fun onBaseCurrencyUpdated() {
        kit.set(currencyManager.baseCurrency.code)
    }

}
