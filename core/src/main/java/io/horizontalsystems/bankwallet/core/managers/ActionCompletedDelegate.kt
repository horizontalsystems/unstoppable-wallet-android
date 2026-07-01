package io.horizontalsystems.bankwallet.core.managers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

object ActionCompletedDelegate {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _walletEvents: MutableSharedFlow<WalletEventType> = MutableSharedFlow()
    val walletEvents: SharedFlow<WalletEventType> = _walletEvents.asSharedFlow()

    fun notifyContactAdded() {
        scope.launch {
            _walletEvents.emit(WalletEventType.ContactAddedToRecent)
        }
    }
}

enum class WalletEventType {
    ContactAddedToRecent
}