package io.horizontalsystems.bankwallet.core.managers

import android.util.Log
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.notifications.NotificationFactory
import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.horizontalsystems.bankwallet.entities.SubscriptionJob
import io.horizontalsystems.bankwallet.entities.canSupport
import io.horizontalsystems.coinkit.models.CoinType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class PriceAlertManager(
        appDatabase: AppDatabase,
        private val notificationSubscriptionManager: INotificationSubscriptionManager,
        private val networkManager: INetworkManager,
        private val notificationManager: INotificationManager,
        private val appConfigProvider: IAppConfigProvider,
        private val localStorageManager: ILocalStorage
) : IPriceAlertManager {

    private val dao = appDatabase.priceAlertsDao()
    private val notificationChangedSubject = PublishSubject.create<Unit>()

    override val notificationChangedFlowable: Flowable<Unit>
        get() = notificationChangedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun getPriceAlerts(): List<PriceAlert> {
        return dao.all()
    }

    override fun savePriceAlert(coinType: CoinType, coinName: String, changeState: PriceAlert.ChangeState, trendState: PriceAlert.TrendState) {
        val (oldChangeState, oldTrendState) = getAlertStates(coinType)
        val newPriceAlert = PriceAlert(coinType, coinName, changeState, trendState)
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

    override suspend fun fetchNotifications() {
        val priceAlerts = getPriceAlerts()
        if (priceAlerts.isEmpty())
            return

        coroutineScope {
            val notificationsId = notificationSubscriptionManager.getNotificationId()
            val jsonObject = withContext(Dispatchers.Default) { networkManager.getNotifications(appConfigProvider.notificationUrl, "messages/$notificationsId") }
            val messages = jsonObject.asJsonObject["messages"].asJsonArray.mapNotNull { jsonElement ->
                NotificationFactory.getMessageFromJson(jsonElement.asJsonObject)
            }

            //get previous server time
            val previousTime = localStorageManager.notificationServerTime

            val testTimeFilter = messages.filter { it.timestamp <= previousTime }
            Log.e("TAG", "fetchNotifications: messages with old timestamp ${testTimeFilter.size}" )

            messages
                    .filter { it.timestamp > previousTime }
                    .forEach {
                        notificationManager.show(it)
                    }

            //store new server time
            val time = jsonObject.asJsonObject["server_time"].asNumber.toLong()
            localStorageManager.notificationServerTime = time
        }
    }

    private fun updateSubscription(alerts: List<PriceAlert>, jobType: SubscriptionJob.JobType) {
        val jobs = mutableListOf<SubscriptionJob>()
        alerts.forEach { alert ->
            if (alert.changeState != PriceAlert.ChangeState.OFF) {
                jobs.add(SubscriptionJob(alert.coinType, SubscriptionJob.StateType.Change, jobType))
            }
            if (alert.trendState != PriceAlert.TrendState.OFF) {
                jobs.add(SubscriptionJob(alert.coinType, SubscriptionJob.StateType.Trend, jobType))
            }
        }
        notificationSubscriptionManager.addNewJobs(jobs)
    }

    private fun updateSubscription(newAlert: PriceAlert, oldChangeState: PriceAlert.ChangeState, oldTrendState: PriceAlert.TrendState) {
        val jobs = mutableListOf<SubscriptionJob>()

        if (oldChangeState != newAlert.changeState) {
            val subscribeJob = SubscriptionJob(newAlert.coinType, SubscriptionJob.StateType.Change, SubscriptionJob.JobType.Subscribe)
            val unsubscribeJob = SubscriptionJob(newAlert.coinType, SubscriptionJob.StateType.Change, SubscriptionJob.JobType.Unsubscribe)

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
            val subscribeJob = SubscriptionJob(newAlert.coinType, SubscriptionJob.StateType.Trend, SubscriptionJob.JobType.Subscribe)
            val unsubscribeJob = SubscriptionJob(newAlert.coinType, SubscriptionJob.StateType.Trend, SubscriptionJob.JobType.Unsubscribe)

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

}
