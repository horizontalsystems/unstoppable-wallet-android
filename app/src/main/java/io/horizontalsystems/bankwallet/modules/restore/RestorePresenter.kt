package io.horizontalsystems.bankwallet.modules.restore

import io.horizontalsystems.bankwallet.core.AccountType
import io.horizontalsystems.bankwallet.core.IAccountCreator
import io.horizontalsystems.bankwallet.core.IPredefinedAccountType
import io.horizontalsystems.bankwallet.core.IPredefinedAccountTypeManager
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.Words12AccountType

class RestorePresenter(
        private val router: RestoreModule.Router,
        private val accountCreator: IAccountCreator,
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager)
    : RestoreModule.ViewDelegate {

    var view: RestoreModule.View? = null

    //  View Delegate

    override var items = listOf<IPredefinedAccountType>()

    override fun viewDidLoad() {
        items = predefinedAccountTypeManager.allTypes
        view?.reload(items)
    }

    override fun onSelect(accountType: IPredefinedAccountType) {
        when (accountType) {
            is Words12AccountType -> {
                router.startRestoreWordsModule()
            }
        }
    }

    override fun didRestore(accountType: AccountType, syncMode: SyncMode) {
        accountCreator.createRestoredAccount(accountType, syncMode)
        router.close()
    }
}
