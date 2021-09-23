package io.horizontalsystems.bankwallet.core.managers

import com.google.gson.Gson
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.INotificationManager
import io.horizontalsystems.bankwallet.core.INotificationSubscriptionManager
import io.horizontalsystems.bankwallet.core.IPriceAlertManager
import io.horizontalsystems.bankwallet.core.notifications.NotificationFactory
import io.horizontalsystems.bankwallet.core.notifications.NotificationNetworkWrapper
import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.horizontalsystems.bankwallet.entities.SubscriptionJob
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import java.net.HttpURLConnection

class PriceAlertManager(
        appDatabase: AppDatabase,
        private val notificationSubscriptionManager: INotificationSubscriptionManager,
        private val notificationManager: INotificationManager,
        private val localStorageManager: ILocalStorage,
        private val notificationNetworkWrapper: NotificationNetworkWrapper,
        private val backgroundManager: BackgroundManager
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
        return Pair(priceAlert?.changeState ?: PriceAlert.ChangeState.OFF, priceAlert?.trendState
                ?: PriceAlert.TrendState.OFF)
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

    override suspend fun fetchNotifications() {
        if (backgroundManager.inForeground || getPriceAlerts().isEmpty())
            return

        val response = notificationNetworkWrapper.fetchNotifications()

        if (response.code() == HttpURLConnection.HTTP_OK){
            val responseBody = response.body() ?: return
            val messages = responseBody.asJsonObject["messages"].asJsonArray.mapNotNull { jsonElement ->
                NotificationFactory.getMessageFromJson(jsonElement.asJsonObject)
            }

            //get previous server time
            val previousTime = localStorageManager.notificationServerTime

            messages
                    .filter { it.timestamp > previousTime }
                    .forEach {
                        notificationManager.show(it)
                    }

            localStorageManager.notificationServerTime = responseBody.asJsonObject["server_time"].asNumber.toLong()
        } else if (response.code() == HttpURLConnection.HTTP_NO_CONTENT){
            //error 204 means - we are asking messages with token without subscriptions
            //we need to update subscriptions with new token
            enablePriceAlerts()
        }
    }

    private fun updateSubscription(alerts: List<PriceAlert>, jobType: SubscriptionJob.JobType) {
        val jobs = mutableListOf<SubscriptionJob>()
        alerts.forEach { alert ->
            if (alert.changeState != PriceAlert.ChangeState.OFF) {
                jobs.add(getChangeSubscriptionJob(alert.coinType, alert.changeState, jobType))
            }
            if (alert.trendState != PriceAlert.TrendState.OFF) {
                jobs.add(getTrendSubscriptionJob(alert.coinType, alert.trendState, jobType))
            }
        }
        notificationSubscriptionManager.addNewJobs(jobs)
    }

    private fun updateSubscription(newAlert: PriceAlert, oldChangeState: PriceAlert.ChangeState, oldTrendState: PriceAlert.TrendState) {
        val jobs = mutableListOf<SubscriptionJob>()

        if (oldChangeState != newAlert.changeState) {
            val subscribeJob = getChangeSubscriptionJob(newAlert.coinType, newAlert.changeState, SubscriptionJob.JobType.Subscribe)
            val unsubscribeJob = getChangeSubscriptionJob(newAlert.coinType, oldChangeState, SubscriptionJob.JobType.Unsubscribe)

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
            val subscribeJob = getTrendSubscriptionJob(newAlert.coinType, newAlert.trendState, SubscriptionJob.JobType.Subscribe)
            val unsubscribeJob = getTrendSubscriptionJob(newAlert.coinType, oldTrendState, SubscriptionJob.JobType.Unsubscribe)

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

    /*
    JSON format
    {
        type: "PRICE",
        data: {
            coin_id: "aave|0x12312312",
            period: "24h",
            percent: 5
        }
    }

    {
        type: "TRENDS",
        data: {
            coin_id: "aave|0x12312312",
            term: "short"
        }
    }
    */

    companion object {
        fun getChangeSubscriptionJob(coinType: CoinType, changeState: PriceAlert.ChangeState, subscribeType: SubscriptionJob.JobType): SubscriptionJob {
            val data = hashMapOf("coin_id" to coinType.id, "percent" to changeState.getIntValue(), "period" to "24h")
            val bodyMap = hashMapOf("type" to "PRICE", "data" to data)
            val body = Gson().toJson(bodyMap)
            return SubscriptionJob(coinType, body, SubscriptionJob.StateType.Change, subscribeType)
        }

        fun getTrendSubscriptionJob(coinType: CoinType, trendState: PriceAlert.TrendState, subscribeType: SubscriptionJob.JobType): SubscriptionJob {
            val data = hashMapOf("coin_id" to coinType.id, "term" to trendState.value)
            val bodyMap = hashMapOf("type" to "TRENDS", "data" to data)
            val body = Gson().toJson(bodyMap)
            return SubscriptionJob(coinType, body, SubscriptionJob.StateType.Trend, subscribeType)
        }
    }

}
