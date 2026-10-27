package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.ScamProtection
import io.horizontalsystems.subscriptions.core.SecureSend
import io.horizontalsystems.subscriptions.core.SwapProtection
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

    private val IPaidAction.key: String
        get() = this::class.simpleName ?: toString()
}
