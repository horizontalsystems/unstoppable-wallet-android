package io.horizontalsystems.bankwallet.modules.createaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.ViewModelUiState

class CreateAccountPasskeyViewModel(accountFactory: IAccountFactory) : ViewModelUiState<CreateAccountPasskeyUiState>() {
    private val defaultAccountName = accountFactory.getNextAccountName()
    private var accountName: String? = defaultAccountName

    fun createAccount() {
    }

    fun onChangeAccountName(v: String) {
        accountName = v

        emitState()
    }

    override fun createState() = CreateAccountPasskeyUiState(
        defaultAccountName = defaultAccountName,
    )

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateAccountPasskeyViewModel(App.accountFactory) as T
        }
    }
}

data class CreateAccountPasskeyUiState(
    val defaultAccountName: String
)
