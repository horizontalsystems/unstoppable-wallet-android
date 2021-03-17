package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.INotificationSubscriptionManager
import io.horizontalsystems.bankwallet.core.IPriceAlertManager
import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.horizontalsystems.bankwallet.entities.SubscriptionJob
import io.horizontalsystems.bankwallet.entities.canSupport
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

class PriceAlertManager(
        appDatabase: AppDatabase,
        private val notificationSubscriptionManager: INotificationSubscriptionManager,
        private val coinManager: ICoinManager
) : IPriceAlertManager {

    private val dao = appDatabase.priceAlertsDao()
    private val notificationChangedSubject = PublishSubject.create<Unit>()

    override val notificationChangedFlowable: Flowable<Unit>
        get() = notificationChangedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun getPriceAlerts(): List<PriceAlert> {
        return dao.all()
    }

    override fun savePriceAlert(priceAlert: PriceAlert) {
        val oldPriceAlert = getPriceAlert(priceAlert.coinId)
        dao.update(priceAlert)
        notificationChangedSubject.onNext(Unit)

        updateSubscription(oldPriceAlert, priceAlert)
    }

    override fun getPriceAlert(coinId: String): PriceAlert {
        val priceAlert = dao.priceAlert(coinId)
        return priceAlert
                ?: PriceAlert(coinId, PriceAlert.ChangeState.OFF, PriceAlert.TrendState.OFF)
    }

    override fun deactivateAllNotifications() {
        val alerts = dao.all()
        updateSubscription(alerts, SubscriptionJob.JobType.Unsubscribe)
        dao.deleteAll()
        notificationChangedSubject.onNext(Unit)
    }

    override fun enablePriceAlerts() {
        val alerts = dao.all()
        updateSubscription(alerts, SubscriptionJob.JobType.Subscribe)
    }

    override fun disablePriceAlerts() {
        val alerts = dao.all()
        updateSubscription(alerts, SubscriptionJob.JobType.Unsubscribe)
    }

    override fun deleteAlertsByAccountType(accountType: AccountType) {
        val alerts = dao.all()
        val coins = coinManager.coins
        val selectedAlerts = alerts.filter { alert ->
            coins.firstOrNull { it.id == alert.coinId }?.type?.canSupport(accountType) == true
        }

        updateSubscription(selectedAlerts, SubscriptionJob.JobType.Unsubscribe)
        selectedAlerts.forEach {
            dao.delete(it)
        }

        notificationChangedSubject.onNext(Unit)
    }

    private fun updateSubscription(alerts: List<PriceAlert>, jobType: SubscriptionJob.JobType) {
        val jobs = mutableListOf<SubscriptionJob>()
        alerts.forEach { alert ->
            if (alert.changeState != PriceAlert.ChangeState.OFF) {
                jobs.add(getChangeSubscriptionJob(alert.coinId, alert.changeState.value, jobType))
            }
            if (alert.trendState != PriceAlert.TrendState.OFF) {
                jobs.add(getTrendSubscriptionJob(alert.coinId, alert.trendState.value, jobType))
            }
        }
        notificationSubscriptionManager.addNewJobs(jobs)
    }

    private fun updateSubscription(oldAlert: PriceAlert, newAlert: PriceAlert) {
        val coinId = newAlert.coinId
        val jobs = mutableListOf<SubscriptionJob>()

        if (oldAlert.changeState != newAlert.changeState) {
            val subscribeJob = getChangeSubscriptionJob(coinId, newAlert.changeState.value, SubscriptionJob.JobType.Subscribe)
            val unsubscribeJob = getChangeSubscriptionJob(coinId, newAlert.changeState.value, SubscriptionJob.JobType.Unsubscribe)

            when {
                oldAlert.changeState == PriceAlert.ChangeState.OFF -> {
                    jobs.add(subscribeJob)
                }
                newAlert.changeState == PriceAlert.ChangeState.OFF -> {
                    jobs.add(unsubscribeJob)
                }
                else -> {
                    jobs.add(unsubscribeJob)
                    jobs.add(subscribeJob)
                }
            }
        } else if (oldAlert.trendState != newAlert.trendState) {
            val subscribeJob = getTrendSubscriptionJob(coinId, newAlert.trendState.value, SubscriptionJob.JobType.Subscribe)
            val unsubscribeJob = getTrendSubscriptionJob(coinId, newAlert.trendState.value, SubscriptionJob.JobType.Unsubscribe)

            when {
                oldAlert.trendState == PriceAlert.TrendState.OFF -> {
                    jobs.add(subscribeJob)
                }
                newAlert.changeState == PriceAlert.ChangeState.OFF -> {
                    jobs.add(unsubscribeJob)
                }
                else -> {
                    jobs.add(unsubscribeJob)
                    jobs.add(subscribeJob)
                }
            }
        }

        notificationSubscriptionManager.addNewJobs(jobs)
    }

    companion object{
        fun getChangeSubscriptionJob(coinId: String, value: String, subscribeType: SubscriptionJob.JobType) =
                SubscriptionJob(coinId, "${coinId}_24hour_${value}percent", SubscriptionJob.StateType.Change, subscribeType)

        fun getTrendSubscriptionJob(coinId: String, value: String, subscribeType: SubscriptionJob.JobType) =
                SubscriptionJob(coinId, "${coinId}_${value}term_trend_change", SubscriptionJob.StateType.Trend, subscribeType)
    }

}
