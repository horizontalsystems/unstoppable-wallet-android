package io.horizontalsystems.subscriptions.fdroid

import android.content.Context
import androidx.startup.Initializer
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager

class SubscriptionServiceInitializer : Initializer<SubscriptionServiceFDroid> {
    override fun create(context: Context): SubscriptionServiceFDroid {
        val service = SubscriptionServiceFDroid(context)
        UserSubscriptionManager.registerService(service)
        return service
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
