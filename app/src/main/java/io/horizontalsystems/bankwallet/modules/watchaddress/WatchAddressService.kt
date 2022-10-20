package io.horizontalsystems.bankwallet.modules.watchaddress

import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Address

class WatchAddressService(
    private val accountFactory: IAccountFactory,
    private val accountManager: IAccountManager,
) {

    private var name: String = ""

    var address: Address? = null
        set(value) {
            field = value
            if (value?.domain != null && name.isBlank()) {
                name = value.domain
            }
        }

    val isCreatable
        get() = address != null

    fun createAccount() {
        val tmpAddress = address ?: throw EmptyAddressException()
        val accountName = name.ifBlank { accountFactory.getNextWatchAccountName() }
        val account = accountFactory.watchAccount(accountName, AccountType.Address(tmpAddress.hex))

        accountManager.save(account)
    }
}

class EmptyAddressException : Exception()