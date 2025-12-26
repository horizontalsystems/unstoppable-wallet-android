package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsModule
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

    init {
        scope.launch {
            _termsAcceptedFlow.emit(localStorage.termsAccepted)
        }
    }

    fun migrateToTermsV2() {
        if(localStorage.termsAccepted) {
            localStorage.termsAccepted = false

            val initialChecked = listOf(
                TermsModule.TermType.Backup.key,
                TermsModule.TermType.DisablingPin.key,
            )

            scope.launch {
                _termsAcceptedFlow.emit(false)
            }
            localStorage.checkedTerms = initialChecked
        }
    }

}
