package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.LatestRateData
import io.reactivex.Single

class BackgroundPriceAlertManager(
        private val priceAlertsStorage: IPriceAlertsStorage,
        private val rateManager: IRateManager,
        private val currencyManager: ICurrencyManager,
        private val rateStorage: IRateStorage,
        private val priceAlertHandler: IPriceAlertHandler
) : IBackgroundPriceAlertManager, BackgroundManager.Listener {

    override fun fetchRates(): Single<LatestRateData> {
        return rateManager.syncLatestRatesSingle()
                .doOnSuccess {
                    priceAlertHandler.handleAlerts(it)
                }
    }

    override fun didEnterBackground() {
        val alerts = priceAlertsStorage.activePriceAlerts()
        val currency = currencyManager.baseCurrency
        alerts.forEach { priceAlert ->
            val rate = rateStorage.latestRate(priceAlert.coin.code, currency.code)
            priceAlert.lastRate = rate?.value
        }
        priceAlertsStorage.save(alerts)
    }

}
