package com.quantum.wallet.bankwallet.core.managers

import com.quantum.wallet.bankwallet.core.ILocalStorage
import com.quantum.wallet.subscriptions.core.IPaidAction
import com.quantum.wallet.subscriptions.core.ScamProtection
import com.quantum.wallet.subscriptions.core.SecureSend
import com.quantum.wallet.subscriptions.core.SwapProtection
import com.quantum.wallet.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.flow.StateFlow

class PaidActionSettingsManager(
    private val localStorage: ILocalStorage
) {
    val toggleableActions: List<IPaidAction> = listOf(
        SecureSend,
        ScamProtection,
        SwapProtection,
    )

    val enabledActionsFlow: StateFlow<Set<String>> = localStorage.enabledPaidActionsFlow

    fun isActionEnabled(action: IPaidAction): Boolean {
        return action.key in localStorage.enabledPaidActions
    }

    fun setActionEnabled(action: IPaidAction, enabled: Boolean) {
        val current = localStorage.enabledPaidActions.toMutableSet()

        if (enabled) {
            current.add(action.key)
        } else {
            current.remove(action.key)
        }

        localStorage.enabledPaidActions = current
    }

    /**
     * Returns true if action is:
     * 1. Allowed by subscription (user has premium)
     * 2. Enabled by user (in enabled list)
     */
    fun isActionActive(action: IPaidAction): Boolean {
        return UserSubscriptionManager.isActionAllowed(action) && isActionEnabled(action)
    }

    private val IPaidAction.key: String
        get() = this::class.simpleName ?: toString()
}
