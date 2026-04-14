package com.quantum.wallet.subscriptions.fdroid

import android.content.Context
import androidx.startup.Initializer
import com.quantum.wallet.subscriptions.core.UserSubscriptionManager

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
