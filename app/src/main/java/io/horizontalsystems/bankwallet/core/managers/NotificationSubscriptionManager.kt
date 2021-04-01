package io.horizontalsystems.bankwallet.core.managers

import android.util.Log
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.INotificationSubscriptionManager
import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.horizontalsystems.bankwallet.entities.SubscriptionJob
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.HashMap

class NotificationSubscriptionManager(
        appDatabase: AppDatabase,
        private val localStorage: ILocalStorage,
        private val networkManager: INetworkManager,
        appConfigProvider: IAppConfigProvider
) : INotificationSubscriptionManager {

    private val dao = appDatabase.subscriptionJobDao()
    private val priceAlertDao = appDatabase.priceAlertsDao()

    private val job = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + job)

    private val host = appConfigProvider.notificationUrl

    override fun processJobs() {
        ioScope.launch {
            val jobs = dao.all()
            jobs.forEach {
                processJob(it)
            }
        }
    }

    override fun addNewJobs(jobs: List<SubscriptionJob>) {
        ioScope.launch {
            jobs.forEach {
                dao.save(it)
                processJob(it)
            }
        }
    }

    override fun getNotificationId(): String {
        var notificationId = localStorage.notificationId
        if (notificationId == null) {
            notificationId = UUID.randomUUID().toString()
            localStorage.notificationId = notificationId
        }
        return notificationId
    }

    private suspend fun processJob(subscriptionJob: SubscriptionJob) {
        try {
            val priceAlert = priceAlertDao.priceAlert(subscriptionJob.coinType) ?: return
            val body = getBody(priceAlert, subscriptionJob.stateType)
            val notificationId = getNotificationId()

            when (subscriptionJob.jobType) {
                SubscriptionJob.JobType.Subscribe -> networkManager.subscribe(host, "subscribe/$notificationId", body)
                SubscriptionJob.JobType.Unsubscribe -> networkManager.unsubscribe(host, "unsubscribe/$notificationId", body)
            }

            dao.delete(subscriptionJob)
        } catch (e: Exception) {
            Log.e("NotifSubscrManager", "subscribe error", e)
        }
    }

    private fun getBody(priceAlert: PriceAlert, stateType: SubscriptionJob.StateType): HashMap<String, Any> {
        return when (stateType) {
            SubscriptionJob.StateType.Change -> {
                val data = hashMapOf("coin_id" to priceAlert.coinType.ID, "change" to priceAlert.changeState.value)
                hashMapOf("type" to "PRICE", "data" to data)
            }
            SubscriptionJob.StateType.Trend -> {
                val data = hashMapOf("coin_id" to priceAlert.coinType.ID, "term" to priceAlert.trendState.value)
                hashMapOf("type" to "TRENDS", "data" to data)
            }
        }
    }
}
