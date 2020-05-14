package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.core.ICurrencyManager
import io.reactivex.Single

class BackgroundPriceAlertManager(
        private val localStorage: ILocalStorage,
        private val backgroundRateAlertScheduler: IBackgroundRateAlertScheduler,
        private val priceAlertsStorage: IPriceAlertsStorage,
        private val rateManager: IRateManager,
        private val walletStorage: IWalletStorage,
        private val currencyManager: ICurrencyManager,
        private val rateStorage: IRateStorage,
        private val priceAlertHandler: IPriceAlertHandler,
        private val notificationManager: INotificationManager)
    : IBackgroundPriceAlertManager, BackgroundManager.Listener {


    override fun onAppLaunch() {
        if (notificationManager.isEnabled && localStorage.isAlertNotificationOn) {
            backgroundRateAlertScheduler.startPeriodicWorker()
        } else {
            backgroundRateAlertScheduler.stopPeriodicWorker()
        }
    }

    override fun fetchRates(): Single<Unit> {
        val coinCodes = walletStorage.enabledCoins().map { it.code }
        val currencyCode = currencyManager.baseCurrency.code
        var hasExpiredRate = false
        val ratesMap = coinCodes.map {
            val marketInfo = rateManager.marketInfo(it, currencyCode)
            if (marketInfo?.isExpired() == true) {
                hasExpiredRate = true
            }
            it to marketInfo?.rate
        }.toMap()

         return when {
            !hasExpiredRate -> {
                priceAlertHandler.handleAlerts(ratesMap)
                Single.just(Unit)
            }
            else -> {
                rateManager.set(coinCodes)
                rateManager.marketInfoObservable(currencyCode)
                        .firstOrError()
                        .doOnSuccess { marketInfoMap ->
                            priceAlertHandler.handleAlerts(coinCodes.map { it to marketInfoMap[it]?.rate }.toMap())
                        }
                        .map { Unit }
            }
        }
    }

    override fun didEnterBackground() {
        val alerts = priceAlertsStorage.all()
        val currency = currencyManager.baseCurrency
        alerts.forEach { priceAlert ->
            val rate = rateStorage.latestRate(priceAlert.coin.code, currency.code)
            priceAlert.lastRate = rate?.value
        }
        priceAlertsStorage.save(alerts)
    }

    override fun willEnterForeground() {
        notificationManager.clear()
    }
}
