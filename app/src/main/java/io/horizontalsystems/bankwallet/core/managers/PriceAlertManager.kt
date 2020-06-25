package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IPriceAlertManager
import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.entities.PriceAlert

class PriceAlertManager(private val appDatabase: AppDatabase): IPriceAlertManager {

    override fun getPriceAlerts(): List<PriceAlert> {
        return appDatabase.priceAlertsDao().all()
    }

    override fun savePriceAlert(priceAlert: PriceAlert) {
        appDatabase.priceAlertsDao().update(priceAlert)
    }

    override fun priceAlert(coinCode: String): PriceAlert {
        val priceAlert = appDatabase.priceAlertsDao().priceAlert(coinCode)
        return priceAlert ?: PriceAlert(coinCode, PriceAlert.ChangeState.OFF, PriceAlert.TrendState.OFF)
    }
}
