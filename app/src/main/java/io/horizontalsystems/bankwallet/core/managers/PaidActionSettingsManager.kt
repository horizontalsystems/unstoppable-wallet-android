package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.ScamProtection
import io.horizontalsystems.subscriptions.core.SecureSend
import io.horizontalsystems.subscriptions.core.SwapProtection
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.flow.StateFlow

class PaidActionSettingsManager(
    private val localStorage: ILocalStorage
) {
    val toggleableActions: List<IPaidAction> = listOf(
        SecureSend,
        ScamProtection,
        SwapProtection,
    )

    val disabledActionsFlow: StateFlow<Set<String>> = localStorage.disabledPaidActionsFlow

    fun isActionEnabled(action: IPaidAction): Boolean {
        return action.key !in localStorage.disabledPaidActions
    }

    fun setActionEnabled(action: IPaidAction, enabled: Boolean) {
        val current = localStorage.disabledPaidActions.toMutableSet()

        if (enabled) {
            current.remove(action.key)
        } else {
            current.add(action.key)
        }

        localStorage.disabledPaidActions = current
    }

    /**
     * Returns true if action is:
     * 1. Allowed by subscription (user has premium)
     * 2. Enabled by user (not in disabled list)
     */
    fun isActionActive(action: IPaidAction): Boolean {
        return UserSubscriptionManager.isActionAllowed(action) && isActionEnabled(action)
    }

    private val IPaidAction.key: String
        get() = this::class.simpleName ?: toString()
}
