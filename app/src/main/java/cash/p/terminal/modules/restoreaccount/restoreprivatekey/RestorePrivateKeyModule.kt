package cash.p.terminal.modules.restoreaccount.restoreprivatekey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App

object RestorePrivateKeyModule {

    class Factory(private val customName: String?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RestorePrivateKeyViewModel(App.accountFactory, customName) as T
        }
    }

    open class RestoreError : Exception() {
        object EmptyText : RestoreError()
        object NotSupportedDerivedType : RestoreError()
        object NonPrivateKey : RestoreError()
        object NoValidKey : RestoreError()
    }

}
