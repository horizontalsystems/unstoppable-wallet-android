package io.horizontalsystems.bankwallet.modules.watchaddress

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
    var address: Address? = null

    val isCreatable
        get() = address != null

    fun createAccount() {
        val tmpAddress = address ?: throw EmptyAddressException()

        val account = accountFactory.watchAccount(tmpAddress.hex, tmpAddress.domain)
        accountManager.save(account)

        val allBlockchains = evmBlockchainManager.allBlockchains
        allBlockchains.forEach {
            evmBlockchainManager.getEvmAccountManager(it).markAutoEnable(account)
        }
        walletActivator.activateWallets(account, coinTypes = allBlockchains.map { it.baseCoinType })
    }
}

class EmptyAddressException : Exception()