package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.Account
import io.horizontalsystems.bankwallet.core.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode
import java.util.*

class AccountFactory {

    fun account(type: AccountType, backedUp: Boolean, defaultSyncMode: SyncMode = SyncMode.FAST): Account {
        val id = UUID.randomUUID().toString()

        return Account(
                id = id,
                name = id,
                type = type,
                isBackedUp = backedUp,
                defaultSyncMode = defaultSyncMode
        )
    }
}
