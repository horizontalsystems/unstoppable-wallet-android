package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IPriceAlertManager
import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

class PriceAlertManager(private val appDatabase: AppDatabase): IPriceAlertManager {

    private val notificationChangedSubject = PublishSubject.create<Unit>()

    override val notificationChangedFlowable: Flowable<Unit>
        get() = notificationChangedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun getPriceAlerts(): List<PriceAlert> {
        return appDatabase.priceAlertsDao().all()
    }

    override fun savePriceAlert(priceAlert: PriceAlert) {
        appDatabase.priceAlertsDao().update(priceAlert)
        notificationChangedSubject.onNext(Unit)
    }

    override fun priceAlert(coinCode: String): PriceAlert {
        val priceAlert = appDatabase.priceAlertsDao().priceAlert(coinCode)
        return priceAlert ?: PriceAlert(coinCode, PriceAlert.ChangeState.OFF, PriceAlert.TrendState.OFF)
    }

    override fun deactivateAllNotifications() {
        appDatabase.priceAlertsDao().deleteAll()
        notificationChangedSubject.onNext(Unit)
    }
}
