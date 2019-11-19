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
            is UnstoppableAccountType ->
                router.startRestoreWordsModule(12, accountType.title)
            is BinanceAccountType -> {
                router.startRestoreWordsModule(24, accountType.title)
            }
            is EosAccountType -> {
                router.startRestoreEosModule(accountType.title)
            }
        }
    }

    override fun onRestore(accountType: AccountType, syncMode: SyncMode?) {
        interactor.restore(accountType, syncMode)
    }

    override fun onClickClose() {
        router.close()
    }

    // IInteractor Delegate

    override fun didRestore() {
        router.startMainModule()
    }

    override fun didFailRestore(e: Exception) {
        view?.showError(e)
    }
}
