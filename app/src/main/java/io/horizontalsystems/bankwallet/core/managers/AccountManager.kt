package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.Account
import io.horizontalsystems.bankwallet.core.ISecuredStorage

class AccountManager(private val secureStorage: ISecuredStorage) {

    //  TODO: get accounts from storage
    val accounts: List<Account>
        get() = listOf()
}
