package cash.p.terminal.core.managers

import cash.p.terminal.core.ILocalStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SubscriptionManager(private val localStorage: ILocalStorage) {

    var authToken: String? = ""

    private val _authTokenFlow = MutableStateFlow(authToken)
    val authTokenFlow: StateFlow<String?> = _authTokenFlow

    fun hasSubscription(): Boolean {
        return true
    }

    suspend fun showPremiumFeatureWarning() {
        showPremiumFeatureWarningFlow.emit(Unit)
    }

}