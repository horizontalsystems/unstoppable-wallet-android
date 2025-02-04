package cash.p.terminal.core.managers

import cash.p.terminal.core.App
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.wallet.entities.EncryptedString
import cash.p.terminal.wallet.managers.ITransactionHiddenManager
import cash.p.terminal.wallet.managers.TransactionDisplayLevel
import cash.p.terminal.wallet.managers.TransactionHiddenState
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TransactionHiddenManager(
    private val localStorage: ILocalStorage,
    backgroundManager: BackgroundManager,
) : ITransactionHiddenManager {
    private val _transactionHiddenFlow = MutableStateFlow(
        TransactionHiddenState(
            transactionHidden = localStorage.transactionHideEnabled,
            transactionHideEnabled = localStorage.transactionHideEnabled,
            transactionDisplayLevel = localStorage.transactionDisplayLevel,
            transactionAutoHidePinExists = localStorage.transactionHideSecretPin != null
        )
    )
    override val transactionHiddenFlow: StateFlow<TransactionHiddenState> =
        _transactionHiddenFlow.asStateFlow()

    override fun showAllTransactions(show: Boolean) {
        if (!transactionHiddenFlow.value.transactionHideEnabled) return
        _transactionHiddenFlow.update {
            _transactionHiddenFlow.value.copy(
                transactionHidden = !show
            )
        }
    }

    override fun setTransactionHideEnabled(enabled: Boolean) {
        localStorage.transactionHideEnabled = enabled
        _transactionHiddenFlow.update {
            _transactionHiddenFlow.value.copy(
                transactionHideEnabled = enabled,
                transactionHidden = enabled
            )
        }
    }

    override fun setTransactionDisplayLevel(displayLevel: TransactionDisplayLevel) {
        localStorage.transactionDisplayLevel = displayLevel
        _transactionHiddenFlow.update {
            _transactionHiddenFlow.value.copy(
                transactionDisplayLevel = displayLevel
            )
        }
    }

    override fun setSeparatePin(pin: String) {
        localStorage.transactionHideSecretPin = EncryptedString(App.encryptionManager.encrypt(pin))
        _transactionHiddenFlow.update {
            _transactionHiddenFlow.value.copy(
                transactionAutoHidePinExists = true
            )
        }
    }

    override fun clearSeparatePin() {
        localStorage.transactionHideSecretPin = null
        _transactionHiddenFlow.update {
            _transactionHiddenFlow.value.copy(
                transactionAutoHidePinExists = false
            )
        }
    }

    override fun isPinMatches(pin: String): Boolean = localStorage.transactionHideSecretPin?.let {
        App.encryptionManager.decrypt(it.value) == pin
    } ?: App.pinComponent.validateCurrentLevel(pin)

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterBackground &&
                    transactionHiddenFlow.value.transactionHideEnabled
                ) {
                    _transactionHiddenFlow.update {
                        _transactionHiddenFlow.value.copy(
                            transactionHidden = true
                        )
                    }
                }
            }
        }
    }
}
