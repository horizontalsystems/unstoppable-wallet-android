package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.INotificationFactory
import io.horizontalsystems.bankwallet.core.INotificationManager
import io.horizontalsystems.bankwallet.core.IPriceAlertHandler
import io.horizontalsystems.bankwallet.core.IPriceAlertsStorage
import io.horizontalsystems.bankwallet.core.factories.PriceAlertItem
import io.horizontalsystems.bankwallet.entities.LatestRateData
import io.horizontalsystems.bankwallet.entities.PriceAlert
import kotlin.math.abs

class PriceAlertHandler(
        private val priceAlertStorage: IPriceAlertsStorage,
        private val notificationManager: INotificationManager,
        private val notificationFactory: INotificationFactory
): IPriceAlertHandler {

    override fun handleAlerts(latestRateData: LatestRateData) {
        val priceAlerts = priceAlertStorage.all()
        val significantAlerts = mutableListOf<PriceAlertItem>()
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
                                priceAlertStorage.save(priceAlert)
                                val signedState = priceAlert.state.value?.let { if (diff >= 0.0) it else -it } ?: 0
                                significantAlerts.add(PriceAlertItem(priceAlert.coin, signedState))
                            }
                        }
                    }
                } else {
                    //fallback, store priceAlert with new rate
                    priceAlert.lastRate = latestRateNullable
                    priceAlertStorage.save(priceAlert)
                }
            }
        }
        val notifications = notificationFactory.notifications(significantAlerts)
        notificationManager.show(notifications)
    }

}
