package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.INotificationFactory
import io.horizontalsystems.bankwallet.core.INotificationManager
import io.horizontalsystems.bankwallet.core.IPriceAlertHandler
import io.horizontalsystems.bankwallet.core.IPriceAlertsStorage
import io.horizontalsystems.bankwallet.core.factories.PriceAlertItem
import io.horizontalsystems.bankwallet.entities.LatestRateData
import io.horizontalsystems.bankwallet.entities.PriceAlert
import java.math.BigDecimal
import kotlin.math.abs

class PriceAlertHandler(
        private val priceAlertStorage: IPriceAlertsStorage,
        private val notificationManager: INotificationManager,
        private val notificationFactory: INotificationFactory
) : IPriceAlertHandler {

    override fun handleAlerts(latestRateData: LatestRateData) {
        val priceAlerts = priceAlertStorage.activePriceAlerts()
        val significantAlerts = mutableListOf<PriceAlertItem>()
        val changedAlerts = mutableListOf<PriceAlert>()

        priceAlerts.forEach { priceAlert ->

            val latestRate = latestRateData.rates[priceAlert.coin.code]?.toBigDecimalOrNull() ?: run {
                        return@forEach
                    }

            val alertRate = priceAlert.lastRate ?: run {
                priceAlert.lastRate = latestRate
                changedAlerts.add(priceAlert)
                return@forEach
            }

            val signedState = signedState(alertRate, latestRate, priceAlert.state.value ?: 0) ?: run {
                        return@forEach
                    }

            priceAlert.lastRate = latestRate
            changedAlerts.add(priceAlert)
            significantAlerts.add(PriceAlertItem(priceAlert.coin, signedState))
        }

        if (changedAlerts.isNotEmpty()) {
            priceAlertStorage.save(changedAlerts)
        }

        val notifications = notificationFactory.notifications(significantAlerts)
        notificationManager.show(notifications)
    }

    private fun signedState(alertRate: BigDecimal, latestRate: BigDecimal, threshold: Int): Int? {
        val diff = (latestRate.toFloat() - alertRate.toFloat()) / alertRate.toFloat() * 100

        if (abs(diff.toInt()) < threshold) {
            return null
        }

        return if (diff < 0) -threshold else threshold
    }


}
