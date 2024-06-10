package io.horizontalsystems.subscriptions.googleplay

import android.content.Context
import androidx.startup.Initializer
import io.horizontalsystems.subscriptionskit.UserSubscriptionManager

class SubscriptionServiceInitializer : Initializer<SubscriptionServiceGooglePlay> {
    override fun create(context: Context): SubscriptionServiceGooglePlay {
        val service = SubscriptionServiceGooglePlay(context)
        UserSubscriptionManager.registerService(service)
        return service
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
