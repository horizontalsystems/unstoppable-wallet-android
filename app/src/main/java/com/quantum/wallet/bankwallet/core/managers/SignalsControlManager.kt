package com.quantum.wallet.bankwallet.core.managers

import com.quantum.wallet.bankwallet.core.ILocalStorage
import com.quantum.wallet.subscriptions.core.TradeSignals
import com.quantum.wallet.subscriptions.core.UserSubscriptionManager

class SignalsControlManager(
    private val localStorage: ILocalStorage,
) {
    val showSignalsStateChangedFlow = localStorage.marketSignalsStateChangedFlow

    var showSignals: Boolean
        get() = localStorage.marketFavoritesShowSignals
                && UserSubscriptionManager.isActionAllowed(TradeSignals)
        set(value) {
            localStorage.marketFavoritesShowSignals = value
        }
}