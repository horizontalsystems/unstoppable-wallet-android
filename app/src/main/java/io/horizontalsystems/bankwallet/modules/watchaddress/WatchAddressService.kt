package io.horizontalsystems.bankwallet.modules.watchaddress

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.entities.Address

class WatchAddressService(
    private val accountFactory: IAccountFactory,
    private val accountManager: IAccountManager,
) {
    val defaultName = accountFactory.getNextWatchAccountName()

    var nameState by mutableStateOf("")

    var name: String = ""

    var address: Address? = null
        set(value) {
            field = value
            if (value?.domain != null && name.isBlank()) {
                name = value.domain
                nameState = value.domain
            }
        }

    val isCreatable
        get() = address != null

    fun createAccount() {
        val tmpAddress = address ?: throw EmptyAddressException()
        val accountName = name.ifBlank { defaultName }
        val account = accountFactory.watchAccount(accountName, tmpAddress.hex, tmpAddress.domain)

        accountManager.save(account)
    }
}

class EmptyAddressException : Exception()