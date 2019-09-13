package io.horizontalsystems.bankwallet.core.managers

import android.app.Activity
import io.horizontalsystems.bankwallet.core.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class RateStatsSyncer(private val rateStatsManager: IRateStatsManager,
                      private val walletManager: IWalletManager,
                      private val currencyManager: ICurrencyManager,
                      private val rateStorage: IRateStorage) : IRateStatsSyncer, BackgroundManager.Listener {

    private var disposables = CompositeDisposable()
    private var rateDisposables = CompositeDisposable()

    init {
        walletManager.walletsUpdatedSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    syncStatsAll()
                    subscribeToRates(currencyManager.baseCurrency.code)
                }
                .let { disposables.add(it) }

        currencyManager.baseCurrencyUpdatedSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    syncStatsAll()
                    subscribeToRates(currencyManager.baseCurrency.code)
                }
                .let { disposables.add(it) }
    }

    // IRateStatsSyncer

    override var balanceStatsOn: Boolean = false
        set(value) {
            field = value
            syncStatsAll()
        }
    override var lockStatsOn: Boolean = false
        set(value) {
            field = value
            syncStatsAll()
        }

    override var rateChartShown: Boolean = false
        set(value) {
            field = value
            syncStatsAll()
        }

    // BackgroundManager.Listener

    override fun willEnterForeground(activity: Activity) {
        syncStatsAll()
    }

    override fun didEnterBackground() {

    }

    // private methods

    private fun syncStatsAll() {
        val baseCurrencyCode = currencyManager.baseCurrency.code
        walletManager.wallets.forEach { wallet ->
            syncStats(wallet.coin.code, baseCurrencyCode)
        }
    }

    private fun syncStats(coinCode: String, currencyCode: String) {
        if (balanceStatsOn || lockStatsOn || rateChartShown) {
            rateStatsManager.syncStats(coinCode, currencyCode)
        }
    }

    private fun subscribeToRates(baseCurrencyCode: String) {
        rateDisposables.clear()

        walletManager.wallets.forEach { wallet ->
            rateStorage.latestRateObservable(wallet.coin.code, baseCurrencyCode)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe { syncStats(wallet.coin.code, baseCurrencyCode) }
                    .let {
                        rateDisposables.add(it)
                    }
        }
    }

}
