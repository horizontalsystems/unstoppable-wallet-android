package io.horizontalsystems.bankwallet.core.notifications

import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.SubscriptionJob
import java.util.*

class NotificationNetworkWrapper(
        private val localStorage: ILocalStorage,
        private val networkManager: INetworkManager,
        appConfigProvider: IAppConfigProvider
) {

    private val host = appConfigProvider.notificationUrl

    private val notificationId: String
        get() {
            var notificationId = localStorage.notificationId
            if (notificationId == null) {
                notificationId = UUID.randomUUID().toString()
                localStorage.notificationId = notificationId
            }
            return notificationId
        }

    suspend fun processSubscription(jobType: SubscriptionJob.JobType, body: HashMap<String, Any>) {
        when (jobType) {
            SubscriptionJob.JobType.Subscribe -> networkManager.subscribe(host, "subscribe/$notificationId", body)
            SubscriptionJob.JobType.Unsubscribe -> networkManager.unsubscribe(host, "unsubscribe/$notificationId", body)
        }
    }

    suspend fun fetchNotifications(): JsonObject {
        return networkManager.getNotifications(host, "messages/$notificationId")
    }

}
