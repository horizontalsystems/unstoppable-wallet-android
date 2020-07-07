package io.horizontalsystems.bankwallet.core.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import io.horizontalsystems.bankwallet.core.App

class MyFirebaseMessagingService: FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        App.priceAlertManager.enablePriceAlerts()
    }
}
