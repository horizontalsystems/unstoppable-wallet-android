package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode
import java.util.*

class AccountFactory : IAccountFactory {

    override fun account(type: AccountType, backedUp: Boolean, defaultSyncMode: SyncMode?): Account {
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
