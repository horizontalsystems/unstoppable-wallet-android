package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.LatestRateData
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.reactivex.Single
import kotlin.math.abs

class BackgroundPriceAlertManager(
        private val priceAlertsStorage: IPriceAlertsStorage,
        private val rateManager: IRateManager,
        private val currencyManager: ICurrencyManager,
        private val rateStorage: IRateStorage
) : IBackgroundPriceAlertManager {

    override fun fetchRates(): Single<LatestRateData> {
        return rateManager.syncLatestRatesSingle()
                .doOnSuccess {
                    handleAlerts(it)
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

    private fun handleAlerts(latestRateData: LatestRateData) {
        val priceAlerts = priceAlertsStorage.all()
        val needNotificationAlerts = mutableListOf<PriceAlert>()
        priceAlerts.forEach { priceAlert ->
            if (priceAlert.state != PriceAlert.State.OFF) {
                val latestRateNullable = latestRateData.rates[priceAlert.coin.code]?.toBigDecimalOrNull()
                val priceAlertLastRate = priceAlert.lastRate
                if (priceAlertLastRate != null) {
                    latestRateNullable?.let { latestRate ->
                        val diff = abs((latestRate.toFloat() - priceAlertLastRate.toFloat()) / latestRate.toFloat() * 100)
                        priceAlert.state.value?.let { percent ->
                            if (diff.toInt() > percent) {
                                priceAlert.lastRate = latestRate
                                priceAlertsStorage.save(priceAlert)
                                needNotificationAlerts.add(priceAlert)
                            }
                        }
                    }
                } else {
                    //fallback, store priceAlert with new rate
                    priceAlert.lastRate = latestRateNullable
                    priceAlertsStorage.save(priceAlert)
                }
            }
        }
    }
}
