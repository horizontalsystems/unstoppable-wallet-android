package io.horizontalsystems.bankwallet.modules.watchaddress

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.WalletActivator
import io.horizontalsystems.bankwallet.entities.Address

class WatchAddressService(
    private val accountFactory: IAccountFactory,
    private val accountManager: IAccountManager,
    private val walletActivator: WalletActivator,
    private val evmBlockchainManager: EvmBlockchainManager
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

        val allBlockchains = evmBlockchainManager.allBlockchains
        allBlockchains.forEach {
            evmBlockchainManager.getEvmAccountManager(it).markAutoEnable(account)
        }
        walletActivator.activateWallets(account, coinTypes = allBlockchains.map { it.baseCoinType })
    }
}

class EmptyAddressException : Exception()