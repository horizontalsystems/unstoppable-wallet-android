package io.horizontalsystems.subscriptions.fdroidci

import android.content.Context
import androidx.startup.Initializer
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager

class SubscriptionServiceInitializer : Initializer<SubscriptionServiceFDroidCi> {
    override fun create(context: Context): SubscriptionServiceFDroidCi {
        val service = SubscriptionServiceFDroidCi(context)
        UserSubscriptionManager.registerService(service)
        return service
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
