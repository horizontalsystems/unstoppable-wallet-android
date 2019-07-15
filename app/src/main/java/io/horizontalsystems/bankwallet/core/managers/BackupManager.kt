package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IBackupManager
import io.reactivex.Flowable

class BackupManager(private val accountManager: IAccountManager) : IBackupManager {

    override val nonBackedUpCount: Int
        get() = accountManager.accounts.count { !it.isBackedUp }

    override val nonBackedUpCountFlowable: Flowable<Int>
        get() = accountManager.accountsFlowable.map { accounts ->
            accounts.count { !it.isBackedUp }
        }

    override fun setIsBackedUp(id: String) {
        accountManager.accounts.find { it.id == id }?.let { account ->
            account.isBackedUp = true
            accountManager.update(account)
        }
    }
}
