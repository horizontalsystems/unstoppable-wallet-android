package io.horizontalsystems.bankwallet.modules.restore.eos

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.adapters.EosAdapter
import io.horizontalsystems.bankwallet.entities.AccountType
import java.util.*

class RestoreEosService() : RestoreEosModule.IService, Clearable {

    override fun accountType(account: String, privateKey: String): AccountType {
        val accountName = account.trim().toLowerCase(Locale.ENGLISH)
        val activePrivateKey = privateKey.trim()

        EosAdapter.validateAccountName(accountName)
        EosAdapter.validatePrivateKey(privateKey)

        return AccountType.Eos(account, activePrivateKey)
    }

    override fun clear() {

    }
}
