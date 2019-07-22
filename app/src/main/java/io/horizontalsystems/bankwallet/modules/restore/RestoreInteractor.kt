package io.horizontalsystems.bankwallet.modules.restore

import io.horizontalsystems.bankwallet.core.IAccountCreator
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode

class RestoreInteractor(private val accountCreator: IAccountCreator) : RestoreModule.Interactor {

    var delegate: RestoreModule.InteractorDelegate? = null

    override fun restore(accountType: AccountType, syncMode: SyncMode?) {
        try {
            accountCreator.createRestoredAccount(accountType, syncMode, createDefaultWallets = true)
            delegate?.didRestore()
        } catch (e: Exception) {
            delegate?.didFailRestore(e)
        }
    }
}
