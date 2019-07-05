package io.horizontalsystems.bankwallet.modules.restore

import io.horizontalsystems.bankwallet.core.AccountType
import io.horizontalsystems.bankwallet.core.IAccountCreator
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.entities.SyncMode

class RestorePresenter(private val router: RestoreModule.Router, private val accountCreator: IAccountCreator) : RestoreModule.ViewDelegate {

    var view: RestoreModule.View? = null

    //  View Delegate

    override fun onSelect(accountType: PredefinedAccountType) {
        when (accountType) {
            PredefinedAccountType.MNEMONIC -> {
                router.navigateToRestoreWords()
            }
            else -> {

            }
        }
    }

    override fun didRestore(accountType: AccountType, syncMode: SyncMode) {
        accountCreator.createRestoredAccount(accountType, syncMode)
        router.close()
    }
}
