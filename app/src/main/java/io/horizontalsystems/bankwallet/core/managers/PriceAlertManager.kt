package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.INotificationSubscriptionManager
import io.horizontalsystems.bankwallet.core.IPriceAlertManager
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.horizontalsystems.bankwallet.entities.SubscriptionJob
import io.horizontalsystems.bankwallet.entities.canSupport
import io.horizontalsystems.coinkit.models.CoinType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import java.util.*

class PriceAlertManager(
        appDatabase: AppDatabase,
        private val notificationSubscriptionManager: INotificationSubscriptionManager,
        private val rateManager: IRateManager
) : IPriceAlertManager {

    private val dao = appDatabase.priceAlertsDao()
    private val notificationChangedSubject = PublishSubject.create<Unit>()

    override val notificationChangedFlowable: Flowable<Unit>
        get() = notificationChangedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun notificationCode(coinType: CoinType): String? {
        return rateManager.getNotificationCoinCode(coinType)
    }

    override fun getPriceAlerts(): List<PriceAlert> {
        return dao.all()
    }

    override fun savePriceAlert(coinType: CoinType, coinName: String, changeState: PriceAlert.ChangeState, trendState: PriceAlert.TrendState) {
        val (oldChangeState, oldTrendState) = getAlertStates(coinType)
        val notificationCoinCode = rateManager.getNotificationCoinCode(coinType) ?: return
        val newPriceAlert = PriceAlert(coinType, notificationCoinCode, coinName, changeState, trendState)
        dao.update(newPriceAlert)
        notificationChangedSubject.onNext(Unit)

        updateSubscription(newPriceAlert, oldChangeState, oldTrendState)
    }

    override fun getAlertStates(coinType: CoinType): Pair<PriceAlert.ChangeState, PriceAlert.TrendState> {
        val priceAlert = dao.priceAlert(coinType)
        return Pair(priceAlert?.changeState ?: PriceAlert.ChangeState.OFF, priceAlert?.trendState ?: PriceAlert.TrendState.OFF)
    }

    override fun hasPriceAlert(coinType: CoinType): Boolean {
        val priceAlert = dao.priceAlert(coinType) ?: return false
        return priceAlert.changeState != PriceAlert.ChangeState.OFF || priceAlert.trendState != PriceAlert.TrendState.OFF
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
        val selectedAlerts = alerts.filter { it.coinType.canSupport(accountType) }

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
                jobs.add(getChangeSubscriptionJob(alert.notificationCoinCode, alert.changeState.value, jobType))
            }
            if (alert.trendState != PriceAlert.TrendState.OFF) {
                jobs.add(getTrendSubscriptionJob(alert.notificationCoinCode, alert.trendState.value, jobType))
            }
        }
        notificationSubscriptionManager.addNewJobs(jobs)
    }

    private fun updateSubscription(newAlert: PriceAlert, oldChangeState: PriceAlert.ChangeState, oldTrendState: PriceAlert.TrendState) {
        val coinCode = newAlert.notificationCoinCode
        val jobs = mutableListOf<SubscriptionJob>()

        if (oldChangeState != newAlert.changeState) {
            val subscribeJob = getChangeSubscriptionJob(coinCode, newAlert.changeState.value, SubscriptionJob.JobType.Subscribe)
            val unsubscribeJob = getChangeSubscriptionJob(coinCode, newAlert.changeState.value, SubscriptionJob.JobType.Unsubscribe)

            when {
                oldChangeState == PriceAlert.ChangeState.OFF -> {
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
        } else if (oldTrendState != newAlert.trendState) {
            val subscribeJob = getTrendSubscriptionJob(coinCode, newAlert.trendState.value, SubscriptionJob.JobType.Subscribe)
            val unsubscribeJob = getTrendSubscriptionJob(coinCode, newAlert.trendState.value, SubscriptionJob.JobType.Unsubscribe)

            when {
                oldTrendState == PriceAlert.TrendState.OFF -> {
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
        fun getChangeSubscriptionJob(coinCode: String, value: String, subscribeType: SubscriptionJob.JobType) =
                SubscriptionJob(coinCode, "${coinCode.toUpperCase(Locale.ENGLISH)}_24hour_${value}percent", SubscriptionJob.StateType.Change, subscribeType)

        fun getTrendSubscriptionJob(coinCode: String, value: String, subscribeType: SubscriptionJob.JobType) =
                SubscriptionJob(coinCode, "${coinCode.toUpperCase(Locale.ENGLISH)}_${value}term_trend_change", SubscriptionJob.StateType.Trend, subscribeType)
    }

}
