package com.quantum.wallet.subscriptions.googleplay

import android.content.Context
import androidx.startup.Initializer
import com.quantum.wallet.subscriptions.core.UserSubscriptionManager

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
