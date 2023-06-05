package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SubscriptionManager(private val localStorage: ILocalStorage) {

    var authToken: String?
        get() = localStorage.authToken
        set(value) {
            localStorage.authToken = value
            _authTokenFlow.update { value }
        }

    private val _authTokenFlow = MutableStateFlow(authToken)
    val authTokenFlow: StateFlow<String?> = _authTokenFlow

}