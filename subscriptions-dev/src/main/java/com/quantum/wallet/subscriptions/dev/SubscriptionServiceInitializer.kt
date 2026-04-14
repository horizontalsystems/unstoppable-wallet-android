package com.quantum.wallet.subscriptions.dev

import android.content.Context
import androidx.startup.Initializer
import com.quantum.wallet.subscriptions.core.UserSubscriptionManager

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
