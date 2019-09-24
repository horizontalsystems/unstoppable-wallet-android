package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.LatestRateData
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.reactivex.Single

class BackgroundPriceAlertManager(
        private val priceAlertsStorage: IPriceAlertsStorage,
        private val rateManager: IRateManager,
        private val currencyManager: ICurrencyManager,
        private val rateStorage: IRateStorage,
        private val priceAlertHandler: IPriceAlertHandler
) : IBackgroundPriceAlertManager {

    override fun fetchRates(): Single<LatestRateData> {
        return rateManager.syncLatestRatesSingle()
                .doOnSuccess {
                    priceAlertHandler.handleAlerts(it)
                }
    }

    override fun updateAlerts() {
        val alerts = priceAlertsStorage.all()
        val currency = currencyManager.baseCurrency
        alerts.forEach { priceAlert ->
            if (priceAlert.state != PriceAlert.State.OFF) {
                val latestRate = rateStorage.latestRate(priceAlert.coin.code, currency.code)
                if (latestRate != null) {
                    priceAlert.lastRate = latestRate.value
                    priceAlertsStorage.save(priceAlert)
                }
            }
        }
    }

}
