package com.quantum.wallet.bankwallet.core.managers

import com.quantum.wallet.bankwallet.core.ILocalStorage
import com.quantum.wallet.bankwallet.core.ITermsManager
import com.quantum.wallet.bankwallet.modules.settings.terms.TermsModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class TermsManager(private val localStorage: ILocalStorage) : ITermsManager {

    private val _termsAcceptedFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val termsAcceptedSharedFlow = _termsAcceptedFlow.asSharedFlow()
    override val terms = TermsModule.TermType.entries

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override val allTermsAccepted: Boolean
        get() = localStorage.termsAccepted

    override val checkedTermIds: List<String>
        get() = localStorage.checkedTerms

    override fun acceptTerms() {
        localStorage.termsAccepted = true
        localStorage.checkedTerms = terms.map { it.key }
        scope.launch {
            _termsAcceptedFlow.emit(true)
        }
    }

    override fun broadcastTermsAccepted(accepted: Boolean) {
        scope.launch {
            _termsAcceptedFlow.emit(accepted)
        }
    }

}
