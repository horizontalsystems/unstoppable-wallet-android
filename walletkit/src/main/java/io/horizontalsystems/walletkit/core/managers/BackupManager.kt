package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.IBackupManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BackupManager(private val accountManager: IAccountManager) : IBackupManager {

    override val allBackedUp: Boolean
        get() = accountManager.accounts.all { it.isBackedUp }

    override val allBackedUpFlow: Flow<Boolean>
        get() = accountManager.accountsFlow.map { accounts ->
            accounts.all { it.isBackedUp }
        }
}
