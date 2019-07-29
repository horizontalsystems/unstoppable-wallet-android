package io.horizontalsystems.bankwallet.modules.restore

import io.horizontalsystems.bankwallet.core.IPredefinedAccountType
import io.horizontalsystems.bankwallet.core.IPredefinedAccountTypeManager
import io.horizontalsystems.bankwallet.entities.*

class RestorePresenter(
        private val router: RestoreModule.Router,
        private val interactor: RestoreModule.Interactor,
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager)
    : RestoreModule.ViewDelegate, RestoreModule.InteractorDelegate {

    var view: RestoreModule.View? = null

    //  View Delegate

    override var items = listOf<IPredefinedAccountType>()

    override fun viewDidLoad() {
        items = predefinedAccountTypeManager.allTypes
        view?.reload(items)
    }

    override fun onSelect(accountType: IPredefinedAccountType) {
        when (accountType) {
            is Words12AccountType ->
                router.startRestoreWordsModule(12)
            is Words24AccountType -> {
                router.startRestoreWordsModule(24)
            }
            is EosAccountType -> {
                router.startRestoreEosModule()
            }
        }
    }

    override fun onRestore(accountType: AccountType, syncMode: SyncMode?) {
        interactor.restore(accountType, syncMode)
    }

    // Interactor Delegate

    override fun didRestore() {
        router.startMainModule()
    }

    override fun didFailRestore(e: Exception) {
        TODO("not implemented")
    }
}
