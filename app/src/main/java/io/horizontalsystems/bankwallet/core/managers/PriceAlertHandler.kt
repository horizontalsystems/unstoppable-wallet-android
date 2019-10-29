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
        val priceAlerts = priceAlertStorage.all()

        val alertItems = getAlertsToNotify(priceAlerts, latestRateData)
        notificationManager.show(notificationFactory.notifications(alertItems))

        //update latest rates only for notified coins
        val alertedCoinCodes = alertItems.map { it.coin.code }
        val filteredPriceAlerts = priceAlerts.filter { alertedCoinCodes.contains(it.coin.code) }
        val changedAlerts = getChangedAlerts(filteredPriceAlerts, latestRateData)
        if (changedAlerts.isNotEmpty()) {
            priceAlertStorage.save(changedAlerts)
        }
    }

    private fun getAlertsToNotify(priceAlerts: List<PriceAlert>, latestRateData: LatestRateData): List<PriceAlertItem> {
        return priceAlerts.mapNotNull { priceAlert ->
            val latestRate = latestRateData.rates[priceAlert.coin.code]?.toBigDecimalOrNull() ?: run {
                return@mapNotNull null
            }

            val alertRate = priceAlert.lastRate ?: run {
                return@mapNotNull null
            }

            val signedState = signedState(alertRate, latestRate, priceAlert.state.value ?: 0) ?: run {
                return@mapNotNull null
            }

            PriceAlertItem(priceAlert.coin, signedState)
        }
    }

    private fun signedState(alertRate: BigDecimal, latestRate: BigDecimal, threshold: Int): Int? {
        val diff = (latestRate.toFloat() - alertRate.toFloat()) / alertRate.toFloat() * 100

        if (abs(diff.toInt()) < threshold) {
            return null
        }

        return if (diff < 0) -threshold else threshold
    }

    private fun getChangedAlerts(priceAlerts: List<PriceAlert>, latestRateData: LatestRateData): List<PriceAlert> {
        val changedAlerts = mutableListOf<PriceAlert>()
        priceAlerts.forEach { priceAlert ->
            latestRateData.rates[priceAlert.coin.code]?.toBigDecimalOrNull()?.let { rate ->
                priceAlert.lastRate = rate
                changedAlerts.add(priceAlert)
            }
        }
        return changedAlerts
    }

}
