package io.horizontalsystems.subscriptions.dev

import android.content.Context
import androidx.startup.Initializer
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager

class SubscriptionServiceInitializer : Initializer<SubscriptionServiceDev> {
    override fun create(context: Context): SubscriptionServiceDev {
        val service = SubscriptionServiceDev(context)
        UserSubscriptionManager.registerService(service)
        return service
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
